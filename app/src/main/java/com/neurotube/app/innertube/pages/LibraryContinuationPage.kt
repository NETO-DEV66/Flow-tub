package com.neurotube.app.innertube.pages

import com.neurotube.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
