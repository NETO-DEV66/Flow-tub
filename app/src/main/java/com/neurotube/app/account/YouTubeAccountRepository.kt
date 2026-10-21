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
import android.util.Log
import com.neurotube.app.data.engagement.FeedInvalidationBus
import com.neurotube.app.innertube.YouTube
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the signed-in YouTube session for the whole process.
 *
 * The app already talks to YouTube through the [`YouTube`] singleton, so the cookie is applied
 * there rather than passed around: every request that asks for login (`accountMenu`, likes,
 * subscriptions, …) starts sending `SAPISIDHASH` as soon as a session exists, and stops the moment
 * it is cleared.
 *
 * [initialize] must run before any request needs the session — it is called from
 * `FlowTubeApplication.onCreate`.
 */
object YouTubeAccountRepository {
    private const val TAG = "YouTubeAccount"

    private lateinit var store: YouTubeAccountStore

    private val _account = MutableStateFlow<YouTubeAccount?>(null)

    /** The signed-in account, or `null` when signed out. */
    val account: StateFlow<YouTubeAccount?> = _account.asStateFlow()

    /** Serializes sign-in/out so a slow account-menu round trip cannot clobber a newer state. */
    private val mutex = Mutex()

    /** Loads any saved session and starts applying it. Safe to call more than once. */
    fun initialize(context: Context) {
        if (::store.isInitialized) return
        store = YouTubeAccountStore(context)
        store.load()?.let(::apply)
    }

    val isSignedIn: Boolean get() = _account.value != null

    /**
     * Validates a freshly captured session against the account menu, then persists it.
     *
     * Validation is what proves the cookie is a real, working session rather than a half-finished
     * login: `accountInfo()` only returns a header when YouTube accepts the credential.
     */
    suspend fun signIn(session: YouTubeLoginSession): Result<YouTubeAccount> =
        mutex.withLock {
            val cookieHeader =
                YouTubeCookieExtractor.toCookieHeader(session.cookieHeader)
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("Captured cookies do not contain a YouTube session"),
                    )
            if (!YouTubeCookieExtractor.hasSession(cookieHeader)) {
                return@withLock Result.failure(
                    IllegalArgumentException("Captured cookies do not contain a YouTube session"),
                )
            }

            val previous = SessionSnapshot(YouTube.cookie, YouTube.visitorData, YouTube.dataSyncId)
            YouTube.cookie = cookieHeader
            // The identity the session was minted with has to be in place before the first call,
            // because InnerTube echoes it back as the request context.
            session.visitorData?.let { YouTube.visitorData = it }
            session.dataSyncId?.let { YouTube.dataSyncId = it }

            val info = YouTube.accountInfo()
            if (info.isFailure) {
                previous.restore()
                return@withLock Result.failure(
                    info.exceptionOrNull() ?: IllegalStateException("YouTube rejected the session"),
                )
            }

            val account =
                info.getOrThrow().let { header ->
                    YouTubeAccount(
                        name = header.name,
                        email = header.email,
                        channelHandle = header.channelHandle,
                        thumbnailUrl = header.thumbnailUrl,
                        cookieHeader = cookieHeader,
                        visitorData = session.visitorData,
                        dataSyncId = session.dataSyncId,
                    )
                }

            runCatching { store.save(account) }
                .onFailure { Log.w(TAG, "Could not persist the session", it) }

            apply(account)
            notifyAccountChanged()
            Result.success(account)
        }

    /** Drops the session from memory and from disk. */
    suspend fun signOut() =
        mutex.withLock {
            YouTube.cookie = null
            YouTube.dataSyncId = null
            YouTube.useLoginForBrowse = false
            _account.value = null
            runCatching { store.clear() }
            notifyAccountChanged()
        }

    private fun notifyAccountChanged() {
        FeedInvalidationBus.emit(FeedInvalidationBus.Event.AccountChanged)
    }

    private fun apply(account: YouTubeAccount) {
        YouTube.cookie = account.cookieHeader
        account.visitorData?.let { YouTube.visitorData = it }
        YouTube.dataSyncId = account.dataSyncId
        // Browse and search ask for login only when this is on, which is what makes the signed-in
        // library (subscriptions, likes, playlists) come back personalised instead of anonymous.
        YouTube.useLoginForBrowse = true
        _account.value = account
    }

    /** The three request-scoped values a session owns, so a rejected sign-in can put them back. */
    private class SessionSnapshot(
        private val cookie: String?,
        private val visitorData: String?,
        private val dataSyncId: String?,
    ) {
        fun restore() {
            YouTube.cookie = cookie
            YouTube.visitorData = visitorData
            YouTube.dataSyncId = dataSyncId
        }
    }
}
