package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicCardShelfRenderer(
    val title: Runs,
    val subtitle: Runs,
    val thumbnail: ThumbnailRenderer,
    val header: Header? = null,
    val contents: List<Content>? = null,
    val buttons: List<Button> = emptyList(),
    val onTap: NavigationEndpoint,
    val subtitleBadges: List<Badges>? = null,
) {
    @Serializable
    data class Header(
        val musicCardShelfHeaderBasicRenderer: MusicCardShelfHeaderBasicRenderer,
    ) {
        @Serializable
        data class MusicCardShelfHeaderBasicRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
    )
}
