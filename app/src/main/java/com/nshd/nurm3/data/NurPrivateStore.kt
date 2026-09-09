package com.nshd.nurm3.data

import android.content.Context
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Non-exportable Android Keystore key; private preferences are excluded from backup. */
class NurPrivateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nur_private", Context.MODE_PRIVATE)
    private val keyAlias = "nur-m3-private-v1"
    private val random = SecureRandom()
    private val lock = Any()

    private fun key(): SecretKey = synchronized(lock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey) ?: run {
            val generator = javax.crypto.KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
            generator.generateKey()
        }
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size >= 28) { "Invalid encrypted data" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun put(name: String, value: String?) = synchronized(lock) {
        require(name !in setOf("pin_hash", "pin_salt", "pin_failures", "pin_lock_until", "pin_lock_wall", "pin_lock_boot"))
        val editor = prefs.edit()
        if (value.isNullOrEmpty()) editor.remove(name) else editor.putString(name, encrypt(value))
        check(editor.commit()) { "Could not save private data" }
    }
    fun get(name: String): String? = synchronized(lock) { prefs.getString(name, null)?.let { decrypt(it) } }
    fun contains(name: String): Boolean = prefs.contains(name)
    fun clear(name: String) { prefs.edit().remove(name).apply() }
    fun hasPin(): Boolean = prefs.contains("pin_hash")

    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    private fun clearFailures(editor: android.content.SharedPreferences.Editor) = editor.putInt("pin_failures", 0)
        .remove("pin_lock_until").remove("pin_lock_wall").remove("pin_lock_boot")

    fun remainingLockoutMs(): Long {
        val wallUntil = prefs.getLong("pin_lock_wall", 0L)
        val boot = prefs.getLong("pin_lock_boot", -1L)
        val sameBoot = boot >= 0L && System.currentTimeMillis() - SystemClock.elapsedRealtime() >= boot - 2000L && System.currentTimeMillis() - SystemClock.elapsedRealtime() <= boot + 2000L
        val remaining = if (sameBoot) prefs.getLong("pin_lock_until", 0L) - SystemClock.elapsedRealtime()
        else wallUntil - System.currentTimeMillis()
        return remaining.coerceIn(0L, 300_000L)
    }

    fun verifyPin(pin: String): Boolean = synchronized(lock) {
        if (!hasPin() || remainingLockoutMs() > 0L) return false
        val salt = Base64.decode(prefs.getString("pin_salt", "") ?: "", Base64.NO_WRAP)
        val expected = Base64.decode(get("pin_hash") ?: "", Base64.NO_WRAP)
        val valid = MessageDigest.isEqual(expected, digest(pin, salt))
        if (valid) check(clearFailures(prefs.edit()).commit())
        else {
            val failures = prefs.getInt("pin_failures", 0) + 1
            val delay = when { failures >= 10 -> 300_000L; failures >= 5 -> 30_000L; else -> 0L }
            check(prefs.edit().putInt("pin_failures", failures)
                .putLong("pin_lock_until", SystemClock.elapsedRealtime() + delay)
                .putLong("pin_lock_wall", System.currentTimeMillis() + delay)
                .putLong("pin_lock_boot", System.currentTimeMillis() - SystemClock.elapsedRealtime()).commit())
        }
        valid
    }

    fun setPin(pin: String, current: String? = null) = synchronized(lock) {
        require(pin.length in 6..12 && pin.all(Char::isDigit)) { "Use a 6–12 digit PIN" }
        if (hasPin()) require(current != null && verifyPin(current)) { "Current PIN is required" }
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = digest(pin, salt)
        check(clearFailures(prefs.edit())
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", encrypt(Base64.encodeToString(hash, Base64.NO_WRAP))).commit())
    }
    fun removePin(current: String) = synchronized(lock) {
        require(verifyPin(current)) { "Current PIN is incorrect" }
        check(clearFailures(prefs.edit()).remove("pin_hash").remove("pin_salt").commit())
    }
}

/** UI access guard, not full-database encryption. */
class NurLock(private val store: NurPrivateStore) {
    private val _locked = MutableStateFlow(store.hasPin())
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private val _configured = MutableStateFlow(store.hasPin())
    val configuredFlow: StateFlow<Boolean> = _configured.asStateFlow()
    val configured: Boolean get() = store.hasPin()
    fun lock() { if (configured) _locked.value = true }
    fun unlock(pin: String): Boolean {
        val ok = store.verifyPin(pin)
        if (ok) _locked.value = false
        return ok
    }
    /** Call only after a successful Android system credential/biometric callback. */
    fun unlockAfterDeviceAuthentication() { if (configured) _locked.value = false }
    fun setPin(pin: String, current: String? = null) {
        store.setPin(pin, current)
        _configured.value = true
        _locked.value = false
    }
    fun removePin(current: String) {
        store.removePin(current)
        _configured.value = false
        _locked.value = false
    }
    fun remainingLockoutMs(): Long = store.remainingLockoutMs()
}
