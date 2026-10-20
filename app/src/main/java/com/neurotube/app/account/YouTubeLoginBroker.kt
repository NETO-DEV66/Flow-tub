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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hands the session cookie from the WebView activity (which owns the cookie jar) back to whoever
 * launched it. Same in-memory handoff pattern as `DiscordLoginBroker`, with one addition: the
 * activity also reports whether it has *finished* with the jar, so the caller knows when it is safe
 * to wipe Google's cookies without racing the WebView.
 */
object YouTubeLoginBroker {
    private var pending: CompletableDeferred<Result<String>>? = null

    private val _busy = MutableStateFlow(false)

    /** True while the login WebView is on screen. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun begin(): CompletableDeferred<Result<String>> {
        val deferred = CompletableDeferred<Result<String>>()
        pending = deferred
        _busy.value = true
        return deferred
    }

    /** The activity captured a session cookie. */
    fun complete(cookieHeader: String) {
        pending?.complete(Result.success(cookieHeader))
        pending = null
        _busy.value = false
    }

    /** The user backed out, or the capture failed. */
    fun fail(message: String) {
        pending?.complete(Result.failure(IllegalStateException(message)))
        pending = null
        _busy.value = false
    }

    /** The WebView has stopped using the cookie jar; the caller may now clear it. */
    fun released() {
        _busy.value = false
    }

    /** No login is in flight (activity destroyed before it reported). */
    fun isPending(): Boolean = pending != null
}
