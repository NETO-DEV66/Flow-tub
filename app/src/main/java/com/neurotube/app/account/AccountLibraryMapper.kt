package com.neurotube.app.account

import com.neurotube.app.data.local.ChannelSubscription
import com.neurotube.app.data.local.LikedVideoInfo
import com.neurotube.app.data.local.VideoHistoryEntry
import com.neurotube.app.data.model.Channel
import com.neurotube.app.data.model.Video
import com.neurotube.app.innertube.models.AlbumItem
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.PlaylistItem
import com.neurotube.app.innertube.models.SongItem
import com.neurotube.app.innertube.models.YTItem
import com.neurotube.app.innertube.pages.AccountVideoCollection
import com.neurotube.app.utils.ImportStepDiagnostic

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
    val videoPlaylists: Int = 0,
    val watchLater: Int = 0,
    val history: Int = 0,
    /** What each part found, wrote and failed with, in the order they ran. */
    val steps: List<ImportStepDiagnostic> = emptyList(),
) {
    val isEmpty: Boolean
        get() =
            channels == 0 &&
                artists == 0 &&
                likedSongs == 0 &&
                playlists == 0 &&
                albums == 0 &&
                videoPlaylists == 0 &&
                watchLater == 0 &&
                history == 0

    /** Whether every part finished without an error, whatever it found. */
    val isClean: Boolean
        get() = steps.none { it.error != null }

    /** The parts that failed, so the result can name them. */
    val failures: List<ImportStepDiagnostic>
        get() = steps.filter { it.error != null }
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

/**
 * The playlists a signed-in account keeps on youtube.com, as the playlists page lists them.
 *
 * These are the plain video playlists, not the music library's: they are saved as external
 * playlists so the app can open and play them, and the "Watch Later" and "Liked videos" lists
 * YouTube owns are left out, since the app keeps its own.
 */
internal fun List<AccountVideoCollection>.toSavedVideoPlaylists(): List<AccountVideoCollection> =
    filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .filter { it.id != WATCH_LATER_PLAYLIST_ID && it.id != LIKED_VIDEOS_PLAYLIST_ID }
        .distinctBy { it.id }

/** The two lists YouTube keeps for every account, which the app has its own version of. */
internal const val WATCH_LATER_PLAYLIST_ID = "WL"
internal const val LIKED_VIDEOS_PLAYLIST_ID = "LL"

/**
 * The videos a signed-in account watched, newest first, as history entries.
 *
 * [now] anchors the order: the history page lists its newest watch first, and the history store
 * sorts by the time it is given, so the first row is stamped [now] and each later one a millisecond
 * earlier. A watch with no duration is kept — the position is simply unknown — because losing the
 * row would lose the fact that the video was watched at all.
 */
internal fun List<Video>.toHistoryEntries(now: Long): List<VideoHistoryEntry> =
    distinctBy { it.id }
        .filter { it.id.isNotBlank() }
        .mapIndexed { index, video ->
            VideoHistoryEntry(
                videoId = video.id,
                position = 0L,
                duration = video.duration.toLong(),
                timestamp = now - index,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                channelName = video.channelName,
                channelId = video.channelId,
            )
        }
