package com.neurotube.app.innertube.models.response

import com.neurotube.app.innertube.models.SearchSuggestionsSectionRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsResponse(
    val contents: List<Content>? = null,
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer? = null,
    )
}
