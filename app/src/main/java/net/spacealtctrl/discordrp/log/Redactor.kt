package net.spacealtctrl.discordrp.log

/**
 * Every log line and crash report passes through here. Exception messages and
 * headers can quote credentials or signed URLs, so the text is stripped before
 * it leaves the process -- including debug builds.
 */
internal object Redactor {
    private const val REPLACEMENT = "[redacted]"

    private val HEADER = Regex(
        "(?i)\\b(authorization|proxy-authorization|cookie|set-cookie)\\s*:\\s*[^\\r\\n]+",
    )
    private val AUTH_SCHEME = Regex("(?i)\\b(bearer|bot)\\s+[A-Za-z0-9._~+/=-]{8,}")
    private val TOKEN = Regex(
        "\\b(?:mfa\\.[A-Za-z0-9_-]{20,}" +
            "|[A-Za-z0-9_-]{23,28}\\.[A-Za-z0-9_-]{6,7}\\.[A-Za-z0-9_-]{27,40})\\b",
    )
    // Opaque credentials are long and base64-like; no message the app logs
    // legitimately needs a run that long.
    private val BASE64 = Regex("[A-Za-z0-9+/_-]{60,}={0,2}")
    private val QUERY = Regex(
        "(?i)([?&](?:" +
            "token|access_token|refresh_token|id_token|client_secret|api[_-]?key|" +
            "password|passwd|secret|session|session_id|code|hm|sig|signature|key|" +
            "x-amz-signature|x-amz-credential|x-amz-security-token|" +
            "x-goog-signature|x-goog-credential|googleaccessid|awsaccesskeyid" +
            ")=)[^&#\\s'\"]+",
    )
    private val FIELD = Regex(
        "(?i)([\"']?(?:password|passwd|pwd|secret|token|cookie|set[_-]?cookie|" +
            "authorization|client[_-]?secret|api[_-]?key|" +
            "access[_-]?token|refresh[_-]?token|id[_-]?token|auth[_-]?token)" +
            "[\"']?\\s*[=:]\\s*)" +
            "(\"[^\"]*\"|'[^']*'|[^\\s,;&}\\)]+)",
    )
    private val USERINFO = Regex("([A-Za-z][A-Za-z0-9+.-]*://)[^/?#\\s@]+@")

    fun redact(text: String): String = text
        .replace(HEADER) { match -> "${match.groupValues[1]}: $REPLACEMENT" }
        .replace(AUTH_SCHEME) { match -> "${match.groupValues[1]} $REPLACEMENT" }
        .replace(TOKEN, "[redacted-token]")
        .replace(BASE64, REPLACEMENT)
        .replace(QUERY) { match -> "${match.groupValues[1]}$REPLACEMENT" }
        .replace(FIELD) { match -> "${match.groupValues[1]}$REPLACEMENT" }
        .replace(USERINFO) { match -> "${match.groupValues[1]}$REPLACEMENT@" }
}
