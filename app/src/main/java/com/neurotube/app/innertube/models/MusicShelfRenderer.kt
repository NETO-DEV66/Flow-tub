package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MusicShelfRenderer(
    val title: Runs? = null,
    val contents: List<Content>? = null,
    val continuations: List<Continuation>? = null,
    val bottomEndpoint: NavigationEndpoint? = null,
    val moreContentButton: Button? = null,
    val subheaders: List<Subheader>? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        // A signed-in library now answers a shelf with lockup rows, as the grid does.
        val lockupViewModel: JsonObject? = null,
        val continuationItemRenderer: ContinuationItemRenderer? = null,
        // A section item can itself be a grid, which is how some libraries return their saved
        // playlists and albums.
        val gridRenderer: GridRenderer? = null,
    )

    @Serializable
    data class Subheader(
        val musicSideAlignedItemRenderer: MusicSideAlignedItemRenderer? = null,
    ) {
        @Serializable
        data class MusicSideAlignedItemRenderer(
            val startItems: List<StartItem>? = null,
        )

        @Serializable
        data class StartItem(
            val musicSortFilterButtonRenderer: MusicSortFilterButtonRenderer? = null,
        )

        @Serializable
        data class MusicSortFilterButtonRenderer(
            val menu: SortFilterMenu? = null,
        )

        @Serializable
        data class SortFilterMenu(
            val musicMultiSelectMenuRenderer: MusicMultiSelectMenuRenderer? = null,
        )

        @Serializable
        data class MusicMultiSelectMenuRenderer(
            val options: List<Option>? = null,
        )

        @Serializable
        data class Option(
            val musicMultiSelectMenuItemRenderer: MusicMultiSelectMenuItemRenderer? = null,
        )

        @Serializable
        data class MusicMultiSelectMenuItemRenderer(
            val formItemEntityKey: String? = null,
        )
    }
}

fun List<MusicShelfRenderer.Content>.getItems(): List<MusicResponsiveListItemRenderer> = mapNotNull { it.musicResponsiveListItemRenderer }

fun List<MusicShelfRenderer.Content>.getContinuation(): String? =
    firstOrNull { it.continuationItemRenderer != null }
        ?.continuationItemRenderer
        ?.continuationEndpoint
        ?.continuationCommand
        ?.token
