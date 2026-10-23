package com.neurotube.app.innertube.models.response

import com.neurotube.app.innertube.models.NavigationEndpoint
import com.neurotube.app.innertube.models.PlaylistPanelRenderer
import com.neurotube.app.innertube.models.Tabs
import com.neurotube.app.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents? = null,
    val currentVideoEndpoint: NavigationEndpoint? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer? = null,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults? = null,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer? = null,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? = null,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab> = emptyList(),
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer,
    )
}
