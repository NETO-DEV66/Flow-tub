package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Button(
    val buttonRenderer: ButtonRenderer,
) {
    @Serializable
    data class ButtonRenderer(
        val text: Runs,
        val navigationEndpoint: NavigationEndpoint? = null,
        val command: NavigationEndpoint? = null,
        val icon: Icon? = null,
    )
}
