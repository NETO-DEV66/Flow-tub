package com.flowtube.app.ui.screens.settings.index

import com.flowtube.app.R
import com.flowtube.app.ui.components.settings.SettingEntry
import com.flowtube.app.ui.components.settings.SettingsDestination
import com.flowtube.app.widget.core.FlowWidgetEntry
import com.flowtube.app.widget.core.FlowWidgets

internal object WidgetsIndex {
    fun entryFor(widget: FlowWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = FlowWidgets.catalog.map(::entryFor)
}
