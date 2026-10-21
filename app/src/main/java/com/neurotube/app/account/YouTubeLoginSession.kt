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

/**
 * What the sign-in WebView captured: the session cookie, plus the visitor identity the session was
 * minted with.
 *
 * InnerTube identifies an authenticated caller by both halves — the cookie proves *who*, and the
 * visitor data and data-sync id are echoed back as the request context — so all three travel
 * together.
 */
data class YouTubeLoginSession(
    val cookieHeader: String,
    val visitorData: String? = null,
    val dataSyncId: String? = null,
)
