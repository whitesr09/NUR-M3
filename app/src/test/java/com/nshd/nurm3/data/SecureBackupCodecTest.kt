package com.nshd.nurm3.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class SecureBackupCodecTest {
    private fun password() = "a long private recovery passphrase".toCharArray()
    @Test fun roundTripAndRandomizedEncryption() {
        val input = "NUR private backup data".toByteArray()
        val first = SecureBackupCodec.encrypt(input, password())
        val second = SecureBackupCodec.encrypt(input, password())
        assertNotEquals(first, second)
        assertArrayEquals(input, SecureBackupCodec.decrypt(first, password()))
    }
    @Test fun wrongPassphraseIsRejected() {
        val encrypted = SecureBackupCodec.encrypt("private".toByteArray(), password())
        assertThrows(IllegalArgumentException::class.java) { SecureBackupCodec.decrypt(encrypted, "another long private passphrase".toCharArray()) }
    }
    @Test fun ciphertextAndMetadataTamperingAreRejected() {
        val encrypted = SecureBackupCodec.encrypt("private".toByteArray(), password())
        val changed = JSONObject(encrypted).put("iterations", 310001).toString()
        assertThrows(IllegalArgumentException::class.java) { SecureBackupCodec.decrypt(changed, password()) }
        val root = JSONObject(encrypted)
        val bytes = java.util.Base64.getDecoder().decode(root.getString("ciphertext"))
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        root.put("ciphertext", java.util.Base64.getEncoder().encodeToString(bytes))
        assertThrows(IllegalArgumentException::class.java) { SecureBackupCodec.decrypt(root.toString(), password()) }
    }
    @Test fun oversizedOrUnsupportedEnvelopeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { SecureBackupCodec.decrypt("x".repeat(SecureBackupCodec.MAX_ENVELOPE + 1), password()) }
        val encrypted = SecureBackupCodec.encrypt("private".toByteArray(), password())
        val changed = JSONObject(encrypted).put("format", "unknown").toString()
        assertThrows(IllegalArgumentException::class.java) { SecureBackupCodec.decrypt(changed, password()) }
    }
}
