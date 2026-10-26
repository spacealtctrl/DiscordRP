package net.spacealtctrl.discordrp.ui.screens.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicReference

private const val LOGIN_URL = "https://discord.com/login"

// Discord's login page removes the storage API from the top window once its
// scripts run, but a same-origin iframe still reaches the same storage area.
// Read through the iframe and drop it again in the same turn.
private const val TOKEN_READ =
    "(function(){var f=null;try{" +
        "f=document.createElement('iframe');" +
        "f.style.display='none';" +
        "(document.documentElement||document.body).appendChild(f);" +
        "var t=f.contentWindow.localStorage.getItem('token');" +
        "if(!t)return '';" +
        "try{return JSON.parse(t)}catch(e){return t}" +
    "}catch(e){return ''}" +
    "finally{if(f&&f.parentNode)f.parentNode.removeChild(f)}" +
    "})()"

private const val CAPTURE_INTERVAL_MS = 500L
private const val CAPTURE_WINDOW_MS = 20_000L

// A blank document commits in milliseconds; the timeout only exists so a
// wedged renderer cannot keep the storage wipe from ever running.
private const val CLEANUP_TIMEOUT_MS = 3_000L

private const val FALLBACK_UA =
    "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

private fun tokenFromEvaluation(raw: String?): String? {
    if (raw.isNullOrBlank() || raw == "null") return null
    val value = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
        ?: return null
    return value.trim().removeSurrounding("\"").trim().takeIf { it.isNotEmpty() }
}

private class WebViewSessionOps(
    private val webView: WebView,
    private val handler: Handler,
) : SessionOps {
    override fun stopPage() {
        webView.stopLoading()
    }

    override fun loadBlank() {
        webView.loadUrl(SessionCleanup.BLANK_URL)
    }

    override fun detach() {
        (webView.parent as? ViewGroup)?.removeView(webView)
    }

    override fun clearStorage() {
        // The WebView keeps a second copy of the token in localStorage while the
        // user is signed in. The blank document has committed by now, so no
        // Discord script is left to write it back.
        WebStorage.getInstance().deleteAllData()
    }

    override fun removeCookies(onDone: () -> Unit) {
        CookieManager.getInstance().removeAllCookies { handler.post(onDone) }
    }

    override fun flushCookies() {
        CookieManager.getInstance().flush()
    }

    override fun clearCache() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
    }

    override fun destroyPage() {
        webView.destroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TokenCapture(onToken: (String) -> Unit) {
    val sessionRef = remember { AtomicReference<SessionCleanup?>(null) }
    AndroidView(
        onRelease = { webView ->
            // The composable owns the view until here; detaching first keeps a
            // deferred destroy legal.
            (webView.parent as? ViewGroup)?.removeView(webView)
            sessionRef.get()?.cancel()
        },
        factory = { context ->
            val handler = Handler(Looper.getMainLooper())
            val webView = WebView(context)
            val session = SessionCleanup(
                ops = WebViewSessionOps(webView, handler),
                scheduleTimeout = { action -> handler.postDelayed(action, CLEANUP_TIMEOUT_MS) },
            )
            sessionRef.set(session)
            webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.setGeolocationEnabled(false)
                settings.mediaPlaybackRequiresUserGesture = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (Build.MANUFACTURER.equals("motorola", ignoreCase = true)) {
                    settings.userAgentString = FALLBACK_UA
                }

                webViewClient = object : WebViewClient() {
                    private var capturing = false

                    private fun capture(view: WebView, url: String?) {
                        if (session.isCancelled) return
                        if (capturing || !LoginWebPolicy.isDiscordAppUrl(url)) return
                        capturing = true
                        val deadline = System.currentTimeMillis() + CAPTURE_WINDOW_MS

                        // The token lands in storage a moment after Discord
                        // starts navigating to /app, so keep reading until the
                        // deadline instead of sampling a fixed number of times.
                        fun read() {
                            view.evaluateJavascript(TOKEN_READ) { result ->
                                if (session.isCancelled) return@evaluateJavascript
                                val token = tokenFromEvaluation(result)
                                if (token != null && !session.isCancelled &&
                                    LoginWebPolicy.isDiscordUrl(view.url)
                                ) {
                                    // Hide the page and hand it to the cleanup
                                    // sequence: it stops the page, swaps in
                                    // about:blank, waits for that commit, then
                                    // wipes storage and awaits cookie removal
                                    // before the token is accepted.
                                    view.visibility = View.GONE
                                    session.start { onToken(token) }
                                    return@evaluateJavascript
                                }
                                if (System.currentTimeMillis() < deadline &&
                                    LoginWebPolicy.isDiscordUrl(view.url)
                                ) {
                                    view.postDelayed({
                                        read()
                                    }, CAPTURE_INTERVAL_MS)
                                } else {
                                    capturing = false
                                }
                            }
                        }

                        read()
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        if (!request.isForMainFrame) return false
                        val url = request.url.toString()
                        if (!LoginWebPolicy.isDiscordUrl(url)) return true
                        if (LoginWebPolicy.isDiscordAppUrl(url)) {
                            view.stopLoading()
                            capture(view, url)
                            return true
                        }
                        return false
                    }

                    // The first composited frame can stay white while the bottom
                    // sheet animates in; repaint once the page is committed.
                    override fun onPageCommitVisible(view: WebView, url: String) {
                        super.onPageCommitVisible(view, url)
                        view.postInvalidateOnAnimation()
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.post {
                            view.requestLayout()
                            view.invalidate()
                        }
                        session.onPageFinished(url)
                        capture(view, url)
                    }
                }
                loadUrl(LOGIN_URL)
            }
        },
    )
}
