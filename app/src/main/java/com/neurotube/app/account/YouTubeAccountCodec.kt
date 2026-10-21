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

import org.json.JSONObject

/**
 * The on-disk shape of a stored session, kept apart from the encryption in [YouTubeAccountStore] so
 * the field mapping can be exercised without the Android keystore.
 *
 * The visitor identity is part of the session, not decoration: a cookie restored without it is
 * rejected by InnerTube, which is what made a saved sign-in come back signed out.
 */
internal object YouTubeAccountCodec {
    fun encode(account: YouTubeAccount): ByteArray =
        JSONObject()
            .put("name", account.name)
            .put("email", account.email)
            .put("channel_handle", account.channelHandle)
            .put("thumbnail_url", account.thumbnailUrl)
            .put("cookie", account.cookieHeader)
            .put("visitor_data", account.visitorData)
            .put("data_sync_id", account.dataSyncId)
            .toString()
            .toByteArray(Charsets.UTF_8)

    fun decode(plaintext: ByteArray): YouTubeAccount {
        val json = JSONObject(String(plaintext, Charsets.UTF_8))
        return YouTubeAccount(
            name = json.getString("name"),
            email = json.optString("email").ifBlank { null },
            channelHandle = json.optString("channel_handle").ifBlank { null },
            thumbnailUrl = json.optString("thumbnail_url").ifBlank { null },
            cookieHeader = json.getString("cookie"),
            visitorData = json.optString("visitor_data").ifBlank { null },
            dataSyncId = json.optString("data_sync_id").ifBlank { null },
        )
    }
}
