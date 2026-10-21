package com.neurotube.app.innertube.models.body

import com.neurotube.app.innertube.models.Context
import kotlinx.serialization.Serializable

/**
 * The guide sidebar. It carries no browse id of its own — the viewer's identity comes from the
 * cookie — and its subscriptions section is what makes it worth a request.
 */
@Serializable
data class GuideBody(
    val context: Context,
    val deviceTheme: String = "DEVICE_THEME_SELECTED",
    val userInterfaceTheme: String = "USER_INTERFACE_THEME_DARK",
)
