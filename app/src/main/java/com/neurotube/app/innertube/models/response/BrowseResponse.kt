package com.neurotube.app.innertube.models.response

import com.neurotube.app.innertube.models.Button
import com.neurotube.app.innertube.models.Continuation
import com.neurotube.app.innertube.models.GridRenderer
import com.neurotube.app.innertube.models.Menu
import com.neurotube.app.innertube.models.MusicDetailHeaderRenderer
import com.neurotube.app.innertube.models.MusicEditablePlaylistDetailHeaderRenderer
import com.neurotube.app.innertube.models.MusicShelfRenderer
import com.neurotube.app.innertube.models.ResponseContext
import com.neurotube.app.innertube.models.Runs
import com.neurotube.app.innertube.models.SectionListRenderer
import com.neurotube.app.innertube.models.SubscriptionButton
import com.neurotube.app.innertube.models.Tabs
import com.neurotube.app.innertube.models.ThumbnailRenderer
import kotlinx.serialization.Serializable

@Serializable
data class BrowseResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val onResponseReceivedActions: List<ResponseAction>? = null,
    val header: Header? = null,
    val microformat: Microformat? = null,
    val responseContext: ResponseContext,
    val background: ThumbnailRenderer? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs? = null,
        val sectionListRenderer: SectionListRenderer? = null,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer? = null,
    )

    @Serializable
    data class TwoColumnBrowseResultsRenderer(
        val tabs: List<Tabs.Tab?>? = null,
        val secondaryContents: SecondaryContents? = null,
    )

    @Serializable
    data class SecondaryContents(
        val sectionListRenderer: SectionListRenderer? = null,
    )

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation? = null,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation? = null,
        val gridContinuation: GridContinuation? = null,
        val musicShelfContinuation: MusicShelfRenderer? = null,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content> = emptyList(),
            val continuations: List<Continuation>? = null,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content> = emptyList(),
            val continuations: List<Continuation>? = null,
        )

        @Serializable
        data class GridContinuation(
            val items: List<GridRenderer.Item> = emptyList(),
            val continuations: List<Continuation>? = null,
        )
    }

    @Serializable
    data class ResponseAction(
        val appendContinuationItemsAction: ContinuationItems? = null,
    ) {
        @Serializable
        data class ContinuationItems(
            val continuationItems: List<MusicShelfRenderer.Content>? = null,
        )
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer? = null,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? = null,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null,
        val musicVisualHeaderRenderer: MusicVisualHeaderRenderer? = null,
        val musicHeaderRenderer: MusicHeaderRenderer? = null,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: Runs,
            val description: Runs? = null,
            val thumbnail: ThumbnailRenderer? = null,
            val playButton: Button? = null,
            val startRadioButton: Button? = null,
            val subscriptionButton: SubscriptionButton? = null,
            val menu: Menu,
            val monthlyListenerCount: Runs? = null,
        )

        @Serializable
        data class MusicVisualHeaderRenderer(
            val title: Runs,
            val foregroundThumbnail: ThumbnailRenderer,
            val thumbnail: ThumbnailRenderer? = null,
        )

        @Serializable
        data class Buttons(
            val menuRenderer: Menu.MenuRenderer? = null,
        )

        @Serializable
        data class MusicHeaderRenderer(
            val buttons: List<Buttons>? = null,
            val title: Runs? = null,
            val thumbnail: MusicThumbnailRenderer? = null,
            val subtitle: Runs? = null,
            val secondSubtitle: Runs? = null,
            val straplineTextOne: Runs? = null,
            val straplineThumbnail: MusicThumbnailRenderer? = null,
        )

        @Serializable
        data class MusicThumbnail(
            val url: String? = null,
        )

        @Serializable
        data class MusicThumbnailRenderer(
            val musicThumbnailRenderer: MusicThumbnailRenderer,
            val thumbnails: List<MusicThumbnail>? = null,
        )
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer? = null,
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String? = null,
        )
    }
}
