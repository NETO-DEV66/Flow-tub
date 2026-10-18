package com.flowtube.app.ui.screens.settings.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.flowtube.app.BuildConfig
import com.flowtube.app.R
import com.flowtube.app.data.recommendation.FlowPersona
import com.flowtube.app.ui.components.settings.SettingsDestination
import com.flowtube.app.ui.components.settings.SettingsGroupScope
import com.flowtube.app.ui.components.settings.SettingsListScope
import com.flowtube.app.ui.components.settings.SettingsTarget
import com.flowtube.app.ui.components.settings.nav
import com.flowtube.app.ui.components.settings.switch
import com.flowtube.app.ui.components.shared.FlowNavRow
import com.flowtube.app.ui.components.shared.FlowSwitchRow
import com.flowtube.app.ui.screens.settings.index.DestinationIndex
import com.flowtube.app.ui.screens.settings.index.HomeIndex

@Immutable
internal data class SettingsHomeState(
    val selected: SettingsDestination?,
    val persona: FlowPersona?,
    val deepFocus: DeepFocusState,
    /** Shown on the update row while a check the user started is running; null when idle. */
    val updateCheckingLabel: String?,
)

internal class SettingsHomeActions(
    val onOpen: (SettingsTarget) -> Unit,
    val onDeepFocusChange: (Boolean) -> Unit,
    val onDurationClick: () -> Unit,
    val onSaveHistoryChange: (Boolean) -> Unit,
    val onScrobbleChange: (Boolean) -> Unit,
    val onCheckForUpdates: () -> Unit,
)

/** The settings list when no search is running. */
internal fun SettingsListScope.homeContent(
    state: SettingsHomeState,
    actions: SettingsHomeActions,
) {
    item(HomeIndex.persona.key) {
        PersonaEntryCard(
            persona = state.persona,
            selected = state.selected == SettingsDestination.TASTE,
            onOpen = { actions.onOpen(SettingsTarget(SettingsDestination.TASTE)) },
        )
    }

    fun SettingsGroupScope.page(destination: SettingsDestination) =
        nav(
            entry = DestinationIndex.entry(destination),
            onClick = { actions.onOpen(SettingsTarget(destination)) },
            icon = destinationIcon(destination),
            selected = state.selected == destination,
            showChevron = state.selected == null,
        )

    group(key = "home.engine", header = R.string.settings_flow_engine_header) {
        row(HomeIndex.deepFocus.key) { shape ->
            FlowSwitchRow(
                title = stringResource(HomeIndex.deepFocus.title),
                supportingText = deepFocusStatus(state.deepFocus) ?: stringResource(R.string.deep_focus_mode_subtitle),
                checked = state.deepFocus.active,
                onCheckedChange = actions.onDeepFocusChange,
                leadingIcon = HomeRowIcons.DeepFocus,
                shape = shape,
            )
        }
        row(HomeIndex.deepFocusDuration.key) { shape ->
            FlowNavRow(
                title = stringResource(HomeIndex.deepFocusDuration.title),
                supportingText = deepFocusDurationLabel(state.deepFocus.expireHours),
                onClick = actions.onDurationClick,
                leadingIcon = HomeRowIcons.DeepFocusDuration,
                shape = shape,
            )
        }
        switch(
            HomeIndex.deepFocusHistory,
            checked = state.deepFocus.saveToHistory,
            onCheckedChange = actions.onSaveHistoryChange,
            icon = HomeRowIcons.DeepFocusHistory,
        )
        switch(
            HomeIndex.deepFocusScrobble,
            checked = state.deepFocus.scrobble,
            onCheckedChange = actions.onScrobbleChange,
            icon = HomeRowIcons.DeepFocusScrobble,
        )
        page(SettingsDestination.TOPICS)
    }
    group(key = "home.look", header = R.string.settings_group_look) {
        page(SettingsDestination.APPEARANCE)
        page(SettingsDestination.LANGUAGE_REGION)
        page(SettingsDestination.WIDGETS)
    }
    group(key = "home.watching", header = R.string.settings_group_watching) {
        page(SettingsDestination.PLAYBACK)
        page(SettingsDestination.QUALITY)
        page(SettingsDestination.CONTENT)
        page(SettingsDestination.INTEGRATIONS)
    }
    group(key = "home.data", header = R.string.settings_group_data) {
        page(SettingsDestination.BACKUP)
        page(SettingsDestination.SYNC)
        page(SettingsDestination.HISTORY)
        page(SettingsDestination.DOWNLOADS)
    }
    group(key = "home.system", header = R.string.settings_group_system) {
        page(SettingsDestination.NOTIFICATIONS)
        page(SettingsDestination.NETWORK)
        page(SettingsDestination.WELLBEING)
    }
    group(key = "home.about", header = R.string.settings_header_about) {
        page(SettingsDestination.ABOUT)
        page(SettingsDestination.DIAGNOSTICS)
        if (BuildConfig.UPDATER_ENABLED) {
            nav(
                HomeIndex.checkForUpdates,
                onClick = actions.onCheckForUpdates,
                value = state.updateCheckingLabel,
                enabled = state.updateCheckingLabel == null,
                showChevron = false,
                icon = HomeRowIcons.Updates,
            )
        }
    }
}
