package com.flowtube.app.innertube.pages

import com.flowtube.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
