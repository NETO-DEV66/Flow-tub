package com.neurotube.app.innertube.pages

import com.neurotube.app.innertube.models.GridRenderer
import com.neurotube.app.innertube.models.MusicShelfRenderer
import com.neurotube.app.innertube.models.SectionListRenderer
import com.neurotube.app.innertube.models.YTItem
import com.neurotube.app.innertube.models.getContinuation
import com.neurotube.app.innertube.models.response.BrowseResponse

/**
 * Reads the shape of the music library, which is not uniform. A first page puts its items in a grid
 * or a shelf directly under the section list, some accounts wrap that in an item section, and a later
 * page arrives as a continuation holding either a bare grid or shelf or a whole section list. These
 * helpers flatten all of that into items plus a next token.
 */
object LibraryResponseParser {
    /** The items and next token of the library tab at [tabIndex]. */
    fun pageOf(
        response: BrowseResponse,
        tabIndex: Int = 0,
    ): LibraryPage {
        val section = firstSection(response, tabIndex)
        val grid = section?.gridRenderer
        if (grid != null) {
            return LibraryPage(grid.items.toLibraryItems(), grid.continuations?.getContinuation())
        }
        val shelf = section?.musicShelfRenderer
        return LibraryPage(shelf.toItems(), shelf?.continuations?.getContinuation())
    }

    /** The items and next token of a library continuation response. */
    fun continuationPageOf(response: BrowseResponse): LibraryContinuationPage {
        val contents = response.continuationContents

        contents?.gridContinuation?.let { grid ->
            return LibraryContinuationPage(grid.items.toLibraryItems(), grid.continuations?.getContinuation())
        }

        contents?.sectionListContinuation?.let { sectionList ->
            // A continuation of a grid is bare, but a continuation of a shelf repeats the section
            // list and puts the next token on the section list itself.
            return fromSlice(
                section = sectionList.contents.firstOrNull(),
                fallbackContinuation = sectionList.continuations?.getContinuation(),
            )
        }

        val shelf = contents?.musicShelfContinuation
        return LibraryContinuationPage(shelf.toItems(), shelf?.continuations?.getContinuation())
    }

    /**
     * The section holding a library tab's items. The section list usually starts with the grid or
     * shelf itself, but can start with an item section wrapping it, and that item section can in turn
     * hold a grid, a shelf, or a list of section items.
     */
    private fun firstSection(
        response: BrowseResponse,
        tabIndex: Int,
    ): SectionListRenderer.Content? {
        val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs ?: return null
        if (tabIndex !in tabs.indices) return null
        val sections =
            tabs[tabIndex]
                .tabRenderer.content
                ?.sectionListRenderer
                ?.contents ?: return null
        val section = sections.firstOrNull() ?: return null
        val itemSection = section.itemSectionRenderer ?: return section

        itemSection.gridRenderer?.let { return SectionListRenderer.Content(gridRenderer = it) }
        itemSection.musicShelfRenderer?.let { return SectionListRenderer.Content(musicShelfRenderer = it) }
        val nestedGrid = itemSection.contents.orEmpty().firstNotNullOfOrNull { it.gridRenderer }
        if (nestedGrid != null) return SectionListRenderer.Content(gridRenderer = nestedGrid)
        return SectionListRenderer.Content(
            musicShelfRenderer =
                MusicShelfRenderer(
                    title = null,
                    contents = itemSection.contents,
                    continuations = null,
                    bottomEndpoint = null,
                    moreContentButton = null,
                ),
        )
    }

    /** One slice of a library: its items, and the token to continue from when it has no own token. */
    private fun fromSlice(
        section: SectionListRenderer.Content?,
        fallbackContinuation: String?,
    ): LibraryContinuationPage {
        val grid = section?.gridRenderer
        if (grid != null) {
            return LibraryContinuationPage(
                items = grid.items.toLibraryItems(),
                continuation = grid.continuations?.getContinuation() ?: fallbackContinuation,
            )
        }
        val shelf = section?.musicShelfRenderer
        return LibraryContinuationPage(
            items = shelf.toItems(),
            continuation = shelf?.continuations?.getContinuation() ?: fallbackContinuation,
        )
    }

    private fun List<GridRenderer.Item>.toLibraryItems(): List<YTItem> =
        mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
            .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }

    private fun MusicShelfRenderer?.toItems(): List<YTItem> =
        this
            ?.contents
            .orEmpty()
            .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
}
