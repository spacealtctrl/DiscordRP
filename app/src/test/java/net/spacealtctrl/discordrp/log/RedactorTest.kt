package net.spacealtctrl.discordrp.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorTest {
    @Test
    fun removesTokenShapedValues() {
        val token = "${"M".repeat(24)}.${"Ga4BcD"}.${"a".repeat(38)}"
        val mfa = "mfa.${"A".repeat(84)}"

        val redacted = Redactor.redact("token=$token\nmfa=$mfa")
        assertFalse(redacted.contains(token))
        assertFalse(redacted.contains(mfa))
        assertTrue(redacted.contains("[redacted-token]"))
    }

    @Test
    fun removesCredentialHeaders() {
        val report = """
            Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature
            Cookie: session=abc123; discord_token=secret
            Accept: application/json
        """.trimIndent()

        val redacted = Redactor.redact(report)
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse(redacted.contains("session=abc123"))
        assertTrue(redacted.contains("Authorization: [redacted]"))
        assertTrue(redacted.contains("Cookie: [redacted]"))
        assertTrue(redacted.contains("Accept: application/json"))
    }

    @Test
    fun removesAuthenticatedUrlParts() {
        val redacted = Redactor.redact(
            "GET https://cdn.discordapp.com/a/b.png?ex=1&is=2&hm=deadbeef " +
                "https://user:pass@discord.com/app?token=abc",
        )
        assertFalse(redacted.contains("deadbeef"))
        assertFalse(redacted.contains("token=abc"))
        assertFalse(redacted.contains("user:pass"))
        assertTrue(redacted.contains("ex=1&is=2"))
        assertTrue(redacted.contains("https://[redacted]@discord.com/app"))
    }

    @Test
    fun removesAwsAndGoogleSignedUrlSecrets() {
        val report =
            "GET https://bucket.s3.amazonaws.com/a?X-Amz-Signature=deadbeef" +
                "&X-Amz-Credential=AKIAEXAMPLE&X-Amz-Security-Token=secretvalue" +
                "&X-Amz-Date=20260101T000000Z\n" +
                "GET https://storage.googleapis.com/b?X-Goog-Signature=cafe" +
                "&X-Goog-Credential=svc%2F20260101&GoogleAccessId=svc@example.test&Expires=1"

        val redacted = Redactor.redact(report)
        assertFalse(redacted.contains("deadbeef"))
        assertFalse(redacted.contains("AKIAEXAMPLE"))
        assertFalse(redacted.contains("secretvalue"))
        assertFalse(redacted.contains("cafe"))
        assertFalse(redacted.contains("svc@example.test"))
        assertTrue(redacted.contains("X-Amz-Date=20260101T000000Z"))
        assertTrue(redacted.contains("Expires=1"))
    }

    @Test
    fun redactsStructuredCredentialFields() {
        val report = """
            {"password":"hunter2","clientSecret":"abc123","api_key":"k-1"}
            client_secret=shh
            PASSWORD: swordfish
        """.trimIndent()

        val redacted = Redactor.redact(report)
        assertFalse(redacted.contains("hunter2"))
        assertFalse(redacted.contains("abc123"))
        assertFalse(redacted.contains("k-1"))
        assertFalse(redacted.contains("shh"))
        assertFalse(redacted.contains("swordfish"))
    }

    @Test
    fun redactsGenericStructuredTokensAndCookies() {
        val report = """
            {"token":"short-test-secret"}
            {"Cookie":"session=example-secret"}
            {"set-cookie":"session=json-cookie"}
            {"Authorization":"Basic dXNlcjpwYXNz"}
            token=plainvalue
            cookie=plaincookie
            set_cookie=kv-cookie
        """.trimIndent()

        val redacted = Redactor.redact(report)
        assertFalse(redacted.contains("short-test-secret"))
        assertFalse(redacted.contains("example-secret"))
        assertFalse(redacted.contains("json-cookie"))
        assertFalse(redacted.contains("dXNlcjpwYXNz"))
        assertFalse(redacted.contains("plainvalue"))
        assertFalse(redacted.contains("plaincookie"))
        assertFalse(redacted.contains("kv-cookie"))
        assertTrue(redacted.contains("\"token\""))
        assertTrue(redacted.contains("\"Cookie\""))
        assertTrue(redacted.contains("\"set-cookie\""))
        assertTrue(redacted.contains("\"Authorization\""))
        assertTrue(redacted.contains("set_cookie="))
    }

    @Test
    fun redactsLongOpaqueRuns() {
        val blob = "A".repeat(64)
        val redacted = Redactor.redact("material=$blob setting=ok")
        assertFalse(redacted.contains(blob))
        assertTrue(redacted.contains("[redacted]"))
        assertTrue(redacted.contains("setting=ok"))
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        val report = """
            java.lang.IllegalStateException: update signature mismatch
                at net.spacealtctrl.discordrp.update.UpdateIdentity.verify(UpdateIdentity.kt:31)
            App version: 1.5.3
            See https://discord.com/app
        """.trimIndent()

        assertEquals(report, Redactor.redact(report))
    }
}
