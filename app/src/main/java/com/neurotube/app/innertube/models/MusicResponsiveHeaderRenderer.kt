package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicResponsiveHeaderRenderer(
    val thumbnail: ThumbnailRenderer? = null,
    val buttons: List<Button> = emptyList(),
    val title: Runs,
    val subtitle: Runs,
    val secondSubtitle: Runs? = null,
    val straplineTextOne: Runs? = null,
    val subtitleBadge: List<Badges>? = null,
) {
    @Serializable
    data class Button(
        val musicPlayButtonRenderer: MusicPlayButtonRenderer? = null,
        val menuRenderer: Menu.MenuRenderer? = null,
    ) {
        @Serializable
        data class MusicPlayButtonRenderer(
            val playNavigationEndpoint: NavigationEndpoint? = null,
        )
    }
}
