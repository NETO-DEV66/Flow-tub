package com.neurotube.app.data.local

import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What Deep Focus and paused watch history allow to be recorded. Writers ask here instead of reading
 * the preferences themselves, so every one of them follows the same rules.
 */
@Singleton
class PrivacyGate internal constructor(
    private val deepFocusActive: suspend () -> Boolean,
    private val watchHistoryPaused: suspend () -> Boolean,
    private val scrobbleDuringDeepFocus: suspend () -> Boolean,
) {
    @Inject
    constructor(preferences: PlayerPreferences) : this(
        deepFocusActive = preferences::isDeepFocusCurrentlyActive,
        watchHistoryPaused = { preferences.watchHistoryPaused.first() },
        scrobbleDuringDeepFocus = { preferences.deepFocusScrobble.first() },
    )

    /** Deep Focus is on and its timer has not run out. */
    suspend fun isDeepFocusActive(): Boolean = deepFocusActive()

    suspend fun isWatchHistoryPaused(): Boolean = watchHistoryPaused()

    /** Listens go to scrobbling services unless Deep Focus is on and the user kept them private. */
    suspend fun allowsScrobbling(): Boolean = !deepFocusActive() || scrobbleDuringDeepFocus()
}
