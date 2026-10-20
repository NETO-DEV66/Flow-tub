package com.neurotube.app.innertube.pages

import com.neurotube.app.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
