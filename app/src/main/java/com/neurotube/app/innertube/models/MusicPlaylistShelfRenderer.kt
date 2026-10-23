package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String? = null,
    val contents: List<MusicShelfRenderer.Content>? = null,
    val collapsedItemCount: Int,
)
