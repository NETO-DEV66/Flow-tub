package com.neurotube.app.innertube.models

data class SearchSuggestions(
    val queries: List<String> = emptyList(),
    val recommendedItems: List<YTItem> = emptyList(),
)
