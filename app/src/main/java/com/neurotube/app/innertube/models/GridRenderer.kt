package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GridRenderer(
    val header: Header?,
    val items: List<Item>,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Header(
        val gridHeaderRenderer: GridHeaderRenderer,
    ) {
        @Serializable
        data class GridHeaderRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Item(
        // Both are optional: a grid appends a bare `continuationItemRenderer` item to carry its
        // next-page token, and a required field would make the whole response fail to parse — and
        // with it the entire library import — the moment that item appears.
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer? = null,
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
        val continuationItemRenderer: ContinuationItemRenderer? = null,
        // A signed-in music library now answers with lockup rows in place of the two-row ones, so
        // the grid is read through both shapes.
        val lockupViewModel: JsonObject? = null,
    )
}
