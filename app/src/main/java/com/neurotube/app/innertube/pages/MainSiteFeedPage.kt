package com.neurotube.app.innertube.pages

import com.neurotube.app.data.model.Video
import com.neurotube.app.innertube.pages.renderer.FeedItem
import com.neurotube.app.innertube.pages.renderer.FeedItemOwner
import com.neurotube.app.innertube.pages.renderer.distinctKey
import com.neurotube.app.innertube.pages.renderer.forEachObject
import com.neurotube.app.innertube.pages.renderer.leadingCount
import com.neurotube.app.innertube.pages.renderer.lockupBadges
import com.neurotube.app.innertube.pages.renderer.toFeedItem
import com.neurotube.app.innertube.pages.renderer.videoOrNull
import kotlinx.serialization.json.JsonElement

/** A playlist the account keeps on youtube.com, as the playlists page lists it. */
data class AccountVideoCollection(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val videoCount: Int,
)

/**
 * One page of a main-site feed — the account's playlists, or its watch history — as items plus the
 * token for the next page.
 *
 * These pages are not the music library: they are served by the main site client and render their
 * rows as `lockupViewModel` cards, which is the shape a channel page and a playlist page already
 * read. Both a playlist card and a video card can sit in the same feed, so the rows are handed back
 * as [FeedItem]s and the caller keeps the kinds it wants.
 */
data class MainSiteFeedPage(
    val items: List<FeedItem>,
    val continuation: String?,
) {
    val playlists: List<AccountVideoCollection>
        get() =
            items
                .filterIsInstance<FeedItem.PlaylistItem>()
                .mapNotNull { item ->
                    val id = item.playlist.id.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    val title = item.playlist.name.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    AccountVideoCollection(
                        id = id,
                        title = title,
                        thumbnailUrl = item.playlist.thumbnailUrl,
                        videoCount = item.playlist.videoCount,
                    )
                }

    val videos: List<Video>
        get() =
            items
                .mapNotNull { it.videoOrNull() }
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
}

/**
 * Reads a main-site feed page.
 *
 * A playlist row arrives as a lockup carrying its count as a thumbnail badge, and the rows sit in a
 * grid or a shelf under a section list — or, on a continuation, under an append action. The token is
 * taken from the row that carries it, in either the `continuationItemRenderer` shape or the
 * `continuationItemViewModel` that replaced it.
 */
internal fun JsonElement.toMainSiteFeedPage(): MainSiteFeedPage {
    val items = mutableListOf<FeedItem>()
    forEachObject { node ->
        val item = node.toFeedItem(FeedItemOwner()) ?: return@forEachObject
        // A playlist lockup carries its count as a thumbnail badge rather than a subtitle, so the
        // count is filled in from the badge the generic reader does not look at.
        val count =
            if (item is FeedItem.PlaylistItem) {
                node["lockupViewModel"].objectOrNull()?.lockupBadges()?.firstNotNullOfOrNull(String::leadingCount)
            } else {
                null
            }
        items +=
            if (count != null && item is FeedItem.PlaylistItem) {
                FeedItem.PlaylistItem(item.playlist.copy(videoCount = count))
            } else {
                item
            }
    }
    return MainSiteFeedPage(
        items = items.distinctBy { it.distinctKey() },
        continuation = nextToken(),
    )
}

/**
 * The token that continues a main-site feed, or null on the last page.
 *
 * Taken as the last token in the response, since anything earlier belongs to a shelf or the header
 * rather than to the list being paged.
 */
internal fun JsonElement.nextToken(): String? {
    var token: String? = null
    forEachObject { node ->
        val value =
            node["continuationItemRenderer"]
                .objectOrNull()
                ?.get("continuationEndpoint")
                .objectOrNull()
                ?.get("continuationCommand")
                .objectOrNull()
                ?.get("token")
                .stringOrNull()
                ?: node["continuationItemViewModel"]
                    .objectOrNull()
                    ?.get("continuationCommand")
                    .objectOrNull()
                    ?.get("token")
                    .stringOrNull()
                ?: node["reloadContinuationData"].objectOrNull()?.get("continuation").stringOrNull()
                ?: node["nextContinuationData"].objectOrNull()?.get("continuation").stringOrNull()
        value?.takeIf(String::isNotBlank)?.let { token = it }
    }
    return token
}
