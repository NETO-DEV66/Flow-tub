package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Badges(
    val musicInlineBadgeRenderer: MusicInlineBadgeRenderer? = null,
) {
    @Serializable
    data class MusicInlineBadgeRenderer(
        val icon: Icon,
    )
}
