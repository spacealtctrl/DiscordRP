package net.spacealtctrl.discordrp.ui.screens.login

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

private const val LOGIN_URL = "https://discord.com/login"

private const val TOKEN_HOOK =
    "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3B" +
        "document.body.appendChild(i)%3B" +
        "alert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"

private const val FALLBACK_UA =
    "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TokenCapture(onToken: (String) -> Unit) {
    AndroidView(
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            if (Build.MANUFACTURER.equals("motorola", ignoreCase = true)) {
                settings.userAgentString = FALLBACK_UA
            }

            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    stopLoading()
                    if (url.endsWith("/app")) {
                        loadUrl(TOKEN_HOOK)
                        visibility = View.GONE
                    }
                    return false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView,
                    url: String,
                    message: String,
                    result: JsResult,
                ): Boolean {
                    onToken(message)
                    visibility = View.GONE
                    return true
                }
            }
            loadUrl(LOGIN_URL)
        }
    })
}
