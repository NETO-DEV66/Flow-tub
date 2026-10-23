package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicEditablePlaylistDetailHeaderRenderer(
    val header: Header,
    val editHeader: EditHeader
) {
    @Serializable
    data class Header(
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? = null,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? = null)

    @Serializable
    data class EditHeader(
        val musicPlaylistEditHeaderRenderer: MusicPlaylistEditHeaderRenderer? = null)
}

@Serializable
data class MusicDetailHeaderRenderer(
    val title: Runs,
    val subtitle: Runs,
    val secondSubtitle: Runs,
    val description: Runs? = null,
    val thumbnail: ThumbnailRenderer,
    val menu: Menu,
)

@Serializable
data class MusicPlaylistEditHeaderRenderer(
    val editTitle: Runs? = null)
