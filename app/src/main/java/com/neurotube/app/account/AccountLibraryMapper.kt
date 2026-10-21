package com.neurotube.app.account

import com.neurotube.app.data.local.ChannelSubscription
import com.neurotube.app.data.local.LikedVideoInfo
import com.neurotube.app.data.model.Channel
import com.neurotube.app.innertube.models.AlbumItem
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.PlaylistItem
import com.neurotube.app.innertube.models.SongItem
import com.neurotube.app.innertube.models.YTItem

/** A playlist or album the account keeps in its YouTube Music library, as that page lists it. */
data class AccountMusicCollection(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val isAlbum: Boolean,
)

/** What one pass over the signed-in library brought in, so the result can be reported. */
data class AccountImportReport(
    val channels: Int = 0,
    val artists: Int = 0,
    val likedSongs: Int = 0,
    val playlists: Int = 0,
    val albums: Int = 0,
) {
    val isEmpty: Boolean get() = channels == 0 && artists == 0 && likedSongs == 0 && playlists == 0 && albums == 0
}

/** The browse ids the music library uses for collections that are not a person's own playlist. */
private val RESERVED_COLLECTION_IDS = setOf("LM", "SE", "LL")

private const val CHANNEL_ID_PREFIX = "UC"

/**
 * The channels a signed-in account follows, as subscriptions.
 *
 * A [Channel] already carries its own id, so nothing has to be resolved; only the fields the
 * subscription store needs are kept.
 */
internal fun List<Channel>.toSubscribedChannels(): List<ChannelSubscription> =
    distinctBy { it.id }
        .filter { it.id.isNotBlank() }
        .map {
            ChannelSubscription(
                channelId = it.id,
                channelName = it.name,
                channelThumbnail = it.thumbnailUrl,
                isMusic = it.isMusic,
            )
        }

/**
 * The artists a signed-in account follows on YouTube Music, as music subscriptions.
 *
 * An [ArtistItem] id is usually already the artist's channel, but a few arrive as a radio or a
 * different page id; those are resolved to a channel id by the caller and handed in through
 * [channelIds], so an artist whose channel cannot be found is dropped rather than subscribed under
 * an id that never yields uploads.
 */
internal fun List<YTItem>.toSubscribedArtists(channelIds: Map<String, String> = emptyMap()): List<ChannelSubscription> =
    filterIsInstance<ArtistItem>()
        .distinctBy { it.id }
        .mapNotNull { artist ->
            val channelId =
                artist.channelId
                    ?: channelIds[artist.id]
                    ?: artist.id.takeIf { it.startsWith(CHANNEL_ID_PREFIX) }
                    ?: return@mapNotNull null
            ChannelSubscription(
                channelId = channelId,
                channelName = artist.title,
                channelThumbnail = artist.thumbnail.orEmpty(),
                isMusic = true,
            )
        }.distinctBy { it.channelId }

/**
 * The songs a signed-in account has liked, newest first.
 *
 * [now] anchors the order: the library page lists its newest like first, and the like store sorts by
 * the time it is given, so the first row is stamped [now] and each later one a millisecond earlier.
 */
internal fun List<YTItem>.toLikedSongs(now: Long): List<LikedVideoInfo> =
    filterIsInstance<SongItem>()
        .distinctBy { it.id }
        .mapIndexed { index, song ->
            LikedVideoInfo(
                videoId = song.id,
                title = song.title,
                thumbnail = song.thumbnail,
                channelName = song.artists.joinToString { it.name },
                likedAt = now - index,
                isMusic = true,
                channelId = song.artists.firstOrNull()?.id,
                durationSeconds = song.duration ?: 0,
            )
        }

/**
 * The playlists and albums a signed-in account has saved.
 *
 * An album is saved under its own browse id so opening it shows the album's layout; a playlist keeps
 * the id the library lists it by. The library's own auto-generated entries — Liked music, the
 * "SE" mix and the "LL" list — are skipped, since the songs they hold arrive as likes.
 */
internal fun List<YTItem>.toSavedMusicCollections(): List<AccountMusicCollection> =
    mapNotNull { item ->
        when (item) {
            is AlbumItem -> {
                AccountMusicCollection(
                    id = item.browseId,
                    title = item.title,
                    thumbnailUrl = item.thumbnail,
                    isAlbum = true,
                )
            }

            is PlaylistItem -> {
                AccountMusicCollection(
                    id = item.id,
                    title = item.title,
                    thumbnailUrl = item.thumbnail.orEmpty(),
                    isAlbum = false,
                )
            }

            else -> {
                null
            }
        }
    }.filter { it.id.isNotBlank() && it.id !in RESERVED_COLLECTION_IDS && it.title.isNotBlank() }
        .distinctBy { it.id }
