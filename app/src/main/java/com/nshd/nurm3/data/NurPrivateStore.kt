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

/** Local-only secrets. The key is non-exportable and the encrypted preferences are not backed up. */
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
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build())
            generator.generateKey()
        }
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size >= 28) { "Invalid encrypted data" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun put(name: String, value: String?) = synchronized(lock) {
        if (value.isNullOrEmpty()) prefs.edit().remove(name).commit()
        else prefs.edit().putString(name, encrypt(value)).commit()
    }

    fun get(name: String): String? = synchronized(lock) {
        prefs.getString(name, null)?.let { decrypt(it) }
    }

    fun contains(name: String): Boolean = prefs.contains(name)
    fun clear(name: String) { prefs.edit().remove(name).apply() }

    fun hasPin(): Boolean = prefs.contains("pin_hash")
    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    fun setPin(pin: String, current: String? = null) = synchronized(lock) {
        require(pin.length in 6..12 && pin.all(Char::isDigit)) { "Use a 6–12 digit PIN" }
        if (hasPin()) require(current != null && verifyPin(current)) { "Current PIN is required" }
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = digest(pin, salt)
        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", encrypt(Base64.encodeToString(hash, Base64.NO_WRAP)))
            .putInt("pin_failures", 0)
            .remove("pin_lock_until")
            .commit()
    }

    fun remainingLockoutMs(): Long = (prefs.getLong("pin_lock_until", 0L) - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

    fun verifyPin(pin: String): Boolean = synchronized(lock) {
        if (!hasPin() || remainingLockoutMs() > 0L) return false
        val salt = Base64.decode(prefs.getString("pin_salt", "") ?: "", Base64.NO_WRAP)
        val expected = Base64.decode(get("pin_hash") ?: "", Base64.NO_WRAP)
        val valid = MessageDigest.isEqual(expected, digest(pin, salt))
        if (valid) prefs.edit().putInt("pin_failures", 0).remove("pin_lock_until").commit()
        else {
            val failures = prefs.getInt("pin_failures", 0) + 1
            val delay = when { failures >= 10 -> 300_000L; failures >= 5 -> 30_000L; else -> 0L }
            prefs.edit().putInt("pin_failures", failures).putLong("pin_lock_until", SystemClock.elapsedRealtime() + delay).commit()
        }
        valid
    }

    fun removePin(current: String) = synchronized(lock) {
        require(verifyPin(current)) { "Current PIN is incorrect" }
        prefs.edit().remove("pin_hash").remove("pin_salt").remove("pin_failures").remove("pin_lock_until").commit()
    }
}

/** The lock guards UI access. It does not claim to encrypt the Room database. */
class NurLock(private val store: NurPrivateStore) {
    private val _locked = MutableStateFlow(store.hasPin())
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    val configured: Boolean get() = store.hasPin()
    fun lock() { if (configured) _locked.value = true }
    fun unlock(pin: String): Boolean {
        val ok = store.verifyPin(pin)
        if (ok) _locked.value = false
        return ok
    }
    fun unlockWithDeviceCredential() { if (configured) _locked.value = false }
    fun setPin(pin: String, current: String? = null) { store.setPin(pin, current); _locked.value = false }
    fun removePin(current: String) { store.removePin(current); _locked.value = false }
    fun remainingLockoutMs(): Long = store.remainingLockoutMs()
}
