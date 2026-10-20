/*
 * Copyright (C) 2026 NeuroTube | A-EDev
 *
 * This file is part of NeuroTube (https://github.com/NETO-DEV66/NeuroTube).
 *
 * NeuroTube is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package com.neurotube.app.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The signed-in YouTube account, as far as the app needs to know it.
 *
 * [cookieHeader] is the whole credential: it is a live Google session, so it is only ever written to
 * [YouTubeAccountStore]'s encrypted file and handed to the network layer. Never log it.
 */
data class YouTubeAccount(
    val name: String,
    val email: String?,
    val channelHandle: String?,
    val thumbnailUrl: String?,
    val cookieHeader: String,
)

/**
 * Encrypted-at-rest store for the YouTube session cookie.
 *
 * Same scheme as `DiscordTokenStore`: AES/GCM with a key that lives in the Android keystore, written
 * through [AtomicFile] in `noBackupFilesDir` so it is never swept into a cloud backup. A cookie that
 * cannot be decrypted (key rotated, keystore reset) is dropped rather than surfaced as an error.
 */
class YouTubeAccountStore(
    context: Context,
) {
    private val storageFile =
        AtomicFile(File(context.applicationContext.noBackupFilesDir, FILE_NAME))

    @Synchronized
    fun save(account: YouTubeAccount) {
        val plaintext =
            JSONObject()
                .put("name", account.name)
                .put("email", account.email)
                .put("channel_handle", account.channelHandle)
                .put("thumbnail_url", account.thumbnailUrl)
                .put("cookie", account.cookieHeader)
                .toString()
                .toByteArray(Charsets.UTF_8)

        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            }
        val payload =
            JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("ciphertext", Base64.encodeToString(cipher.doFinal(plaintext), Base64.NO_WRAP))
                .toString()
                .toByteArray(Charsets.UTF_8)

        val output = storageFile.startWrite()
        try {
            output.write(payload)
            output.flush()
            storageFile.finishWrite(output)
        } catch (error: Throwable) {
            storageFile.failWrite(output)
            throw error
        }
    }

    @Synchronized
    fun load(): YouTubeAccount? {
        if (!storageFile.baseFile.exists()) return null

        return runCatching {
            val payload =
                JSONObject(
                    storageFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() },
                )
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        getOrCreateKey(),
                        GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            Base64.decode(payload.getString("iv"), Base64.NO_WRAP),
                        ),
                    )
                }
            val plaintext =
                cipher.doFinal(Base64.decode(payload.getString("ciphertext"), Base64.NO_WRAP))
            val json = JSONObject(String(plaintext, Charsets.UTF_8))
            YouTubeAccount(
                name = json.getString("name"),
                email = json.optString("email").ifBlank { null },
                channelHandle = json.optString("channel_handle").ifBlank { null },
                thumbnailUrl = json.optString("thumbnail_url").ifBlank { null },
                cookieHeader = json.getString("cookie"),
            )
        }.getOrElse {
            clear()
            null
        }
    }

    @Synchronized
    fun clear() {
        storageFile.delete()
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
                deleteEntry(KEY_ALIAS)
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val FILE_NAME = "youtube_account.enc"
        const val KEY_ALIAS = "flow_youtube_account_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
