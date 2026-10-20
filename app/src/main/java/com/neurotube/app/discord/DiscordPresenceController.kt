package com.neurotube.app.discord

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable access to Discord Rich Presence. The shipping build wires the real
 * [DiscordPresenceRuntime]; this adapter lets settings take it as a dependency instead of reaching
 * the object directly.
 */
@Singleton
class DiscordPresenceController
    @Inject
    constructor() {
        val settingsState: StateFlow<DiscordSettingsState> = DiscordPresenceRuntime.settingsState

        suspend fun setEnabled(enabled: Boolean) = DiscordPresenceRuntime.setEnabled(enabled)

        suspend fun connectAccount(): DiscordLinkResult = DiscordPresenceRuntime.connectAccount()

        suspend fun retry(): DiscordLinkResult = DiscordPresenceRuntime.retry()

        suspend fun unlink(): Boolean = DiscordPresenceRuntime.unlink()
    }
