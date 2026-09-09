package com.nshd.nurm3.data

import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable encrypted export. No Android Keystore key is needed on the destination device. */
object SecureBackupCodec {
    const val FORMAT = "NUR-M3-secure-v1"
    const val MAX_PLAINTEXT = 16 * 1024 * 1024
    const val MAX_ENVELOPE = 24 * 1024 * 1024
    private const val ITERATIONS = 310_000
    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private fun encode(bytes: ByteArray) = b64.encodeToString(bytes)
    private fun decode(value: String, size: Int): ByteArray = decoder.decode(value).also { require(it.size == size) }
    private fun key(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        require(password.size >= 12) { "Use a passphrase of at least 12 characters." }
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    private fun aad(iterations: Int, salt: String, nonce: String): ByteArray = "$FORMAT|$iterations|$salt|$nonce".toByteArray(Charsets.UTF_8)
    fun encrypt(plaintext: ByteArray, password: CharArray): String {
        require(plaintext.isNotEmpty() && plaintext.size <= MAX_PLAINTEXT)
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val saltText = encode(salt); val nonceText = encode(nonce)
        val derived = key(password, salt, ITERATIONS)
        val ciphertext = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(aad(ITERATIONS, saltText, nonceText))
                doFinal(plaintext)
            }
        } finally { derived.fill(0) }
        return JSONObject().put("format", FORMAT).put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", ITERATIONS).put("salt", saltText).put("nonce", nonceText)
            .put("cipher", "AES-256-GCM").put("ciphertext", encode(ciphertext)).toString()
    }
    fun decrypt(envelope: String, password: CharArray): ByteArray {
        require(envelope.toByteArray(Charsets.UTF_8).size <= MAX_ENVELOPE) { "Encrypted file is too large." }
        val root = JSONObject(envelope)
        require(root.getString("format") == FORMAT && root.getString("kdf") == "PBKDF2-HMAC-SHA256" && root.getString("cipher") == "AES-256-GCM") { "Unsupported encrypted backup format." }
        val iterations = root.getInt("iterations")
        require(iterations in 210_000..600_000) { "Unsupported key derivation parameters." }
        val saltText = root.getString("salt"); val nonceText = root.getString("nonce")
        val salt = decode(saltText, 16); val nonce = decode(nonceText, 12)
        val ciphertext = decoder.decode(root.getString("ciphertext"))
        require(ciphertext.size in 17..MAX_PLAINTEXT + 16) { "Invalid encrypted payload size." }
        val derived = key(password, salt, iterations)
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(aad(iterations, saltText, nonceText))
                doFinal(ciphertext).also { require(it.size <= MAX_PLAINTEXT) }
            }
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("Incorrect passphrase or backup integrity check failed.")
        } finally { derived.fill(0) }
    }
    fun fingerprint(envelope: String): String = MessageDigest.getInstance("SHA-256")
        .digest(envelope.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
}
