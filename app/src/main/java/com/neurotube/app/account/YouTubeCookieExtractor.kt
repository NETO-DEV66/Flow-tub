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
 * Turns what the WebView's cookie jar hands back into the single `Cookie:` header the InnerTube
 * client wants.
 *
 * Kept free of Android types so it can be unit-tested directly.
 */
object YouTubeCookieExtractor {
    /**
     * Names the app must send to be authenticated, and the only ones it keeps.
     *
     * Google spreads a session over many cookies; InnerTube's `SAPISIDHASH` authorization needs
     * `SAPISID`, and the `__Secure-*`/`*SID` set carries the rest of the identity. Everything else
     * (`VISITOR_INFO1_LIVE`, `PREF`, `SOCS`, …) is dropped so the app never persists more of the
     * session than it needs.
     */
    private val SESSION_COOKIE_NAMES =
        setOf(
            "SAPISID",
            "APISID",
            "SSID",
            "SID",
            "HSID",
            "LSID",
            "__Secure-1PSID",
            "__Secure-3PSID",
            "__Secure-1PAPISID",
            "__Secure-3PAPISID",
            "__Secure-1PSIDTS",
            "__Secure-3PSIDTS",
            "__Secure-1PSIDCC",
            "__Secure-3PSIDCC",
        )

    /** True when [cookieHeader] carries enough of a session for authenticated InnerTube calls. */
    fun hasSession(cookieHeader: String?): Boolean = extractSession(cookieHeader).containsKey("SAPISID")

    /**
     * Filters a raw `Cookie:` value down to [SESSION_COOKIE_NAMES], preserving the jar's order and
     * returning `null` when nothing usable is left.
     */
    fun extractSession(rawCookieHeader: String?): Map<String, String> {
        if (rawCookieHeader.isNullOrBlank()) return emptyMap()

        return rawCookieHeader
            .split(';')
            .mapNotNull { pair ->
                val name = pair.substringBefore('=', "").trim()
                val value = pair.substringAfter('=', "").trim()
                if (name.isEmpty() || value.isEmpty() || name !in SESSION_COOKIE_NAMES) {
                    null
                } else {
                    name to value
                }
            }.toMap()
    }

    /** Serializes the kept cookies back into a `Cookie:` header value, or `null` when empty. */
    fun toCookieHeader(rawCookieHeader: String?): String? =
        extractSession(rawCookieHeader)
            .takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(separator = "; ") { (name, value) -> "$name=$value" }
}
