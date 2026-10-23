package com.neurotube.app.innertube.models.response

import com.neurotube.app.innertube.models.Continuation
import com.neurotube.app.innertube.models.MusicResponsiveListItemRenderer
import com.neurotube.app.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs? = null,
    )

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation? = null,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content> = emptyList(),
            val continuations: List<Continuation>? = null,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer,
            )
        }
    }
}
