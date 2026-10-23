package com.neurotube.app.innertube.pages

import com.neurotube.app.innertube.models.AlbumItem
import com.neurotube.app.innertube.models.Artist
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.PlaylistItem
import com.neurotube.app.innertube.models.SongItem
import com.neurotube.app.innertube.models.WatchEndpoint
import com.neurotube.app.innertube.models.YTItem
import com.neurotube.app.innertube.pages.renderer.forEachObject
import com.neurotube.app.innertube.pages.renderer.leadingCount
import com.neurotube.app.innertube.pages.renderer.lockupBadges
import com.neurotube.app.innertube.pages.renderer.lockupByline
import com.neurotube.app.innertube.pages.renderer.lockupThumbnailUrl
import com.neurotube.app.innertube.pages.renderer.metadataParts
import com.neurotube.app.innertube.pages.renderer.parseDurationText
import kotlinx.serialization.json.JsonObject

/** The `contentType`s a music library row can carry, as `lockupViewModel` names them. */
private const val LOCKUP_CONTENT_TYPE_ALBUM = "LOCKUP_CONTENT_TYPE_ALBUM"
private const val LOCKUP_CONTENT_TYPE_AUDIOBOOK = "LOCKUP_CONTENT_TYPE_AUDIOBOOK"
private const val LOCKUP_CONTENT_TYPE_PLAYLIST = "LOCKUP_CONTENT_TYPE_PLAYLIST"
private const val LOCKUP_CONTENT_TYPE_PODCAST = "LOCKUP_CONTENT_TYPE_PODCAST"
private const val LOCKUP_CONTENT_TYPE_ARTIST = "LOCKUP_CONTENT_TYPE_ARTIST"
private const val LOCKUP_CONTENT_TYPE_SONG = "LOCKUP_CONTENT_TYPE_SONG"

/**
 * A music library row carried by a `lockupViewModel`.
 *
 * The library used to answer with `musicTwoRowItemRenderer` and `musicResponsiveListItemRenderer`
 * rows and still does in older captures, but a signed-in account is now served `lockupViewModel`
 * rows instead — the same shape change that emptied the subscriptions list. Those rows carry no
 * renderer the old readers know, so they were walked past and every `FEmusic_liked_*` page imported
 * as nothing while the liked-songs shelf, which still uses the older renderer, kept working.
 *
 * Returns null for a row this app has no place for, so an unknown `contentType` is skipped rather
 * than aborting the page it sits in.
 */
internal fun JsonObject.toLibraryItem(): YTItem? {
    val contentId = this["contentId"].stringOrNull()?.takeIf(String::isNotBlank) ?: return null
    val metadata = this["metadata"].objectOrNull()?.get("lockupMetadataViewModel").objectOrNull()
    val title = metadata?.get("title").youtubeText()?.takeIf(String::isNotBlank) ?: return null
    val thumbnail = lockupThumbnailUrl().orEmpty()
    val explicit = containsExplicitBadge()

    return when (this["contentType"].stringOrNull()) {
        LOCKUP_CONTENT_TYPE_ALBUM,
        LOCKUP_CONTENT_TYPE_AUDIOBOOK,
        -> {
            // An album's browse id is not its playlist id, and the importer resolves the album page
            // from the browse id, so the watch endpoint's playlist id is a display convenience.
            AlbumItem(
                browseId = contentId,
                playlistId = lockupPlaylistId() ?: contentId,
                title = title,
                artists = metadata.lockupByline()?.let { listOf(Artist(name = it.name, id = it.id.takeIf(String::isNotBlank))) },
                thumbnail = thumbnail,
                explicit = explicit,
            )
        }

        LOCKUP_CONTENT_TYPE_PLAYLIST,
        LOCKUP_CONTENT_TYPE_PODCAST,
        -> {
            val byline = metadata.lockupByline()
            PlaylistItem(
                id = contentId,
                title = title,
                author = byline?.let { Artist(name = it.name, id = it.id.takeIf(String::isNotBlank)) },
                songCountText = lockupBadges().firstOrNull { it.leadingCount() != null },
                thumbnail = thumbnail,
                playEndpoint = lockupWatchEndpoint(),
                shuffleEndpoint = null,
                radioEndpoint = null,
                isEditable = false,
            )
        }

        LOCKUP_CONTENT_TYPE_ARTIST -> {
            ArtistItem(
                id = contentId,
                title = title,
                thumbnail = thumbnail,
                channelId = contentId.takeIf { it.startsWith(CHANNEL_ID_PREFIX) },
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        }

        LOCKUP_CONTENT_TYPE_SONG -> {
            val byline = metadata.lockupByline()
            SongItem(
                id = contentId,
                title = title,
                artists = listOfNotNull(byline?.let { Artist(name = it.name, id = it.id.takeIf(String::isNotBlank)) }),
                album = null,
                duration = lockupBadges().firstNotNullOfOrNull(::parseDurationText),
                thumbnail = thumbnail,
                explicit = explicit,
                endpoint = lockupWatchEndpoint(),
            )
        }

        else -> {
            null
        }
    }
}

private const val CHANNEL_ID_PREFIX = "UC"

/**
 * The playlist id a lockup's play action points at.
 *
 * Only the album rows need this — their `contentId` is the album's browse id, while the play action
 * names the playlist id the album's tracks are listed under.
 */
private fun JsonObject.lockupPlaylistId(): String? =
    this["itemPlayback"]
        .objectOrNull()
        ?.get("inlinePlayerData")
        .objectOrNull()
        ?.get("onSelect")
        .objectOrNull()
        ?.get("innertubeCommand")
        .objectOrNull()
        ?.get("watchEndpoint")
        .objectOrNull()
        ?.get("playlistId")
        .stringOrNull()
        ?.takeIf(String::isNotBlank)

/** The play action of a lockup, which is what opens the playlist, album or song. */
private fun JsonObject.lockupWatchEndpoint(): WatchEndpoint? {
    val watch =
        this["itemPlayback"]
            .objectOrNull()
            ?.get("inlinePlayerData")
            .objectOrNull()
            ?.get("onSelect")
            .objectOrNull()
            ?.get("innertubeCommand")
            .objectOrNull()
            ?.get("watchEndpoint")
            .objectOrNull()
            ?: return null
    val videoId = watch["videoId"].stringOrNull()?.takeIf(String::isNotBlank) ?: return null
    return WatchEndpoint(
        videoId = videoId,
        playlistId = watch["playlistId"].stringOrNull()?.takeIf(String::isNotBlank),
        params = watch["params"].stringOrNull(),
    )
}

/** The explicit-content badge, which a lockup carries as an inline badge in its metadata. */
private fun JsonObject.containsExplicitBadge(): Boolean {
    var found = false
    forEachObject { node ->
        if (found) return@forEachObject
        val icon =
            node["musicInlineBadgeRenderer"]
                .objectOrNull()
                ?.get("icon")
                .objectOrNull()
                ?.get("iconType")
                .stringOrNull()
        if (icon == "MUSIC_EXPLICIT_BADGE") found = true
    }
    return found
}
