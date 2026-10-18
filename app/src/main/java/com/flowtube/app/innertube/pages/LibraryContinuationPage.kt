package com.flowtube.app.innertube.pages

import com.flowtube.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
