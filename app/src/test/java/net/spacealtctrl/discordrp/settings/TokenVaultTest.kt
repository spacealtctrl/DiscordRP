package net.spacealtctrl.discordrp.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.InvalidKeyException
import java.security.KeyStoreException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException

class TokenVaultTest {
    @Test
    fun `a login that can never be opened is reported as lost`() {
        assertTrue(TokenVault.isPermanent(AEADBadTagException()))
        assertTrue(TokenVault.isPermanent(BadPaddingException()))
        assertTrue(TokenVault.isPermanent(IllegalArgumentException("bad base64")))
    }

    @Test
    fun `a busy or failing keystore keeps the login`() {
        assertFalse(TokenVault.isPermanent(KeyStoreException()))
        assertFalse(TokenVault.isPermanent(ProviderException()))
        assertFalse(TokenVault.isPermanent(IllegalBlockSizeException()))
        assertFalse(TokenVault.isPermanent(InvalidKeyException()))
        assertFalse(TokenVault.isPermanent(UnrecoverableKeyException()))
        assertFalse(TokenVault.isPermanent(IllegalStateException()))
    }
}
