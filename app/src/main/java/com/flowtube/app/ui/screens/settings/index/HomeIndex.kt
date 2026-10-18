package com.flowtube.app.ui.screens.settings.index

import com.flowtube.app.R
import com.flowtube.app.ui.components.settings.SettingAvailability
import com.flowtube.app.ui.components.settings.SettingEntry
import com.flowtube.app.ui.components.settings.SettingsDestination

/** Options that live directly on the settings list rather than on a page of their own. */
internal object HomeIndex {
    private val home = SettingsDestination.HOME

    val persona =
        SettingEntry(
            key = "home.persona",
            title = R.string.taste_title,
            summary = R.string.taste_summary,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFocus =
        SettingEntry(
            key = "home.deep_focus",
            title = R.string.deep_focus_mode_title,
            summary = R.string.deep_focus_mode_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFocusDuration =
        SettingEntry(
            key = "home.deep_focus_duration",
            title = R.string.deep_focus_expire_duration_title,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFocusHistory =
        SettingEntry(
            key = "home.deep_focus_history",
            title = R.string.deep_focus_save_history_title,
            summary = R.string.deep_focus_save_history_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFocusScrobble =
        SettingEntry(
            key = "home.deep_focus_scrobble",
            title = R.string.deep_focus_scrobble_title,
            summary = R.string.deep_focus_scrobble_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val checkForUpdates =
        SettingEntry(
            key = "home.updates",
            title = R.string.check_for_updates,
            summary = R.string.check_for_updates_subtitle,
            section = R.string.settings_header_about,
            destination = home,
            availability = SettingAvailability.GithubOnly,
        )
    /** The persona card is found through Your taste's own destination entry, not listed twice. */
    val all = listOf(deepFocus, deepFocusDuration, deepFocusHistory, deepFocusScrobble, checkForUpdates)
}
