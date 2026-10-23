package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicCarouselShelfRenderer(
    val header: Header? = null,
    val contents: List<Content> = emptyList(),
    val itemSize: String,
    val numItemsPerColumn: Int? = null,
) {
    @Serializable
    data class Header(
        val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer,
    ) {
        @Serializable
        data class MusicCarouselShelfBasicHeaderRenderer(
            val strapline: Runs? = null,
            val title: Runs,
            val thumbnail: ThumbnailRenderer? = null,
            val moreContentButton: Button? = null,
        )
    }

    @Serializable
    data class Content(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer? = null, // navigation button in explore tab
    )
}
