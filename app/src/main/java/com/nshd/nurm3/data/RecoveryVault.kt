package com.nshd.nurm3.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local recovery encryption, not a portable backup. The key is non-exportable. */
class RecoveryVault(private val context: Context) {
    private val alias = "nur-m3-recovery-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: synchronized(RecoveryVault::class.java) {
            val again = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (again.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setRandomizedEncryptionRequired(true).build())
                generateKey()
            }
        }
    }
    private fun file(name: String): File {
        require(name.matches(Regex("[a-z0-9-]{1,64}")))
        return File(context.noBackupFilesDir, "$name.nur-recovery")
    }
    fun save(name: String, plaintext: ByteArray) {
        require(plaintext.size <= SecureBackupCodec.MAX_PLAINTEXT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val bytes = byteArrayOf(1) + cipher.iv + cipher.doFinal(plaintext)
        val target = file(name)
        val temp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(temp).use { it.write(bytes); it.fd.sync() }
        try { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    }
    fun read(name: String): ByteArray? {
        val target = file(name)
        if (!target.exists()) return null
        require(target.length() in 29..(SecureBackupCodec.MAX_PLAINTEXT + 29).toLong())
        val bytes = target.readBytes()
        require(bytes[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size))
    }
    fun exists(name: String) = file(name).exists()
}
