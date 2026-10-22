package com.neurotube.app.account

import android.content.Context
import android.util.Log
import com.neurotube.app.data.engagement.FeedInvalidationBus
import com.neurotube.app.data.local.LikedVideosRepository
import com.neurotube.app.data.local.PlaylistRepository
import com.neurotube.app.data.local.SubscriptionRepository
import com.neurotube.app.data.model.Channel
import com.neurotube.app.data.music.YouTubeMusicService
import com.neurotube.app.data.music.model.PlaylistDetails
import com.neurotube.app.data.recommendation.FlowNeuroEngine
import com.neurotube.app.innertube.YouTube
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.YTItem
import com.neurotube.app.innertube.utils.completed
import com.neurotube.app.ui.screens.music.saveMusicCollection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AccountLibraryImport"

/** The music library's browse ids, one per kind of thing a viewer keeps there. */
private const val LIBRARY_LIKED_SONGS = "FEmusic_liked_videos"
private const val LIBRARY_ARTISTS = "FEmusic_library_corpus_artists"
private const val LIBRARY_PLAYLISTS = "FEmusic_liked_playlists"
private const val LIBRARY_ALBUMS = "FEmusic_liked_albums"

/** How many channel pages to read before giving up, so a loop can never run forever. */
private const val MAX_CHANNEL_PAGES = 40

/** How many lookups to run at once. Each collection is a full playlist, so this stays small. */
private const val PARALLELISM = 3

/** The parts of a library, in the order they are brought in. */
enum class AccountImportStep {
    SUBSCRIPTIONS,
    ARTISTS,
    LIKED_SONGS,
    PLAYLISTS,
    ALBUMS,
}

/** Progress through an import, so the screen can say what it is doing. */
sealed interface AccountImportState {
    data object Idle : AccountImportState

    data class Running(
        val step: AccountImportStep,
    ) : AccountImportState

    data class Done(
        val report: AccountImportReport,
    ) : AccountImportState

    data object Failed : AccountImportState
}

/**
 * Brings a signed-in YouTube account's library into the app: the channels and artists it follows,
 * the songs it liked, and the playlists and albums it saved.
 *
 * Everything lands in the same stores the rest of the app reads, so an import needs no screen of
 * its own — the subscriptions list, Liked music and the music library show the result. Each part is
 * attempted on its own, so one failing leaves the others in place, and the report counts only what
 * was actually written.
 */
@Singleton
class AccountLibraryImport
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val subscriptions: SubscriptionRepository,
        private val likedVideos: LikedVideosRepository,
        private val musicPlaylists: PlaylistRepository,
    ) {
        /**
         * Reads the account's library and stores it. [onStep] is called before each part, and the
         * report counts what each part added.
         */
        suspend fun run(onStep: (AccountImportStep) -> Unit = {}): AccountImportReport {
            var report = AccountImportReport()

            onStep(AccountImportStep.SUBSCRIPTIONS)
            val channels = runCatching { fetchSubscriptions() }.getOrElse { emptyList() }
            if (channels.isNotEmpty()) {
                runCatching { subscriptions.subscribeAll(channels.toSubscribedChannels()) }
                    .onSuccess { report = report.copy(channels = channels.size) }
                    .onFailure { Log.w(TAG, "Importing channels failed", it) }
            }

            onStep(AccountImportStep.ARTISTS)
            val artists = runCatching { fetchLibrary(LIBRARY_ARTISTS) }.getOrElse { emptyList() }
            if (artists.isNotEmpty()) {
                runCatching {
                    val music = artists.toSubscribedArtists(resolveArtistChannels(artists))
                    subscriptions.subscribeAll(music)
                    music.size
                }.onSuccess { report = report.copy(artists = it) }
                    .onFailure { Log.w(TAG, "Importing artists failed", it) }
            }

            onStep(AccountImportStep.LIKED_SONGS)
            val songs = runCatching { fetchLibrary(LIBRARY_LIKED_SONGS) }.getOrElse { emptyList() }
            if (songs.isNotEmpty()) {
                runCatching { likedVideos.importLikes(songs.toLikedSongs(System.currentTimeMillis())) }
                    .onSuccess { report = report.copy(likedSongs = it.size) }
                    .onFailure { Log.w(TAG, "Importing liked songs failed", it) }
            }

            onStep(AccountImportStep.PLAYLISTS)
            val collections = runCatching { fetchLibrary(LIBRARY_PLAYLISTS) }.getOrElse { emptyList() }
            if (collections.isNotEmpty()) {
                val saved =
                    runCatching { saveCollections(collections.toSavedMusicCollections()) }
                        .getOrElse { 0 }
                report = report.copy(playlists = saved)
            }

            onStep(AccountImportStep.ALBUMS)
            val albums = runCatching { fetchLibrary(LIBRARY_ALBUMS) }.getOrElse { emptyList() }
            if (albums.isNotEmpty()) {
                val saved =
                    runCatching { saveCollections(albums.toSavedMusicCollections()) }
                        .getOrElse { 0 }
                report = report.copy(albums = saved)
            }

            seedFeedFromImport(report)

            return report
        }

        /**
         * Teaches the recommendation engine the channels just imported and drops the feed caches, so
         * the next load is ranked for this account rather than the previous identity. Mirrors what the
         * backup and Takeout importers do; without it an import left the subscriptions list correct
         * but the home feed ignorant of every new channel.
         */
        private suspend fun seedFeedFromImport(report: AccountImportReport) {
            if (report.isEmpty) return

            val channelNames =
                runCatching { subscriptions.getAllSubscriptions().first() }
                    .getOrDefault(emptyList())
                    .map { it.channelName }
                    .filter { it.isNotBlank() }
            if (channelNames.isNotEmpty()) {
                runCatching { FlowNeuroEngine.bootstrapFromSubscriptions(context, channelNames) }
                    .onFailure { Log.w(TAG, "Seeding the engine from subscriptions failed", it) }
            }

            FeedInvalidationBus.emit(FeedInvalidationBus.Event.AccountChanged)
        }

        /**
         * Every subscription the account holds: the guide's short list, which is always populated,
         * merged with `FEchannels`, which is the full one and may be empty on its own.
         */
        private suspend fun fetchSubscriptions(): List<Channel> =
            withContext(Dispatchers.IO) {
                val all = mutableListOf<Channel>()
                all += YouTube.guideSubscriptions().getOrDefault(emptyList())

                var continuation: String? = null
                var pages = 0
                do {
                    val page = YouTube.subscribedChannels(continuation).getOrNull() ?: break
                    all += page.channels
                    continuation = page.continuation
                    pages++
                } while (continuation != null && pages < MAX_CHANNEL_PAGES)

                all.distinctBy { it.id }
            }

        private suspend fun fetchLibrary(browseId: String): List<YTItem> =
            YouTube
                .library(browseId)
                .completed()
                .getOrNull()
                ?.items
                .orEmpty()

        /**
         * Saves each collection with its songs, so the library can play it offline. Collections are
         * fetched a few at a time, and one failing does not stop the rest. Returns how many were saved.
         */
        private suspend fun saveCollections(collections: List<AccountMusicCollection>): Int {
            val gate = Semaphore(PARALLELISM)
            val saved =
                coroutineScope {
                    collections
                        .map { collection ->
                            async {
                                gate.withPermit {
                                    val details = fetchCollection(collection) ?: return@withPermit false
                                    runCatching { musicPlaylists.saveMusicCollection(details) }
                                        .onFailure { Log.w(TAG, "Saving ${collection.id} failed", it) }
                                        .isSuccess
                                }
                            }
                        }.awaitAll()
                }
            return saved.count { it }
        }

        /**
         * An album's browse id is not its playlist id, so an album that yields no songs as a
         * playlist is read as an album instead.
         */
        private suspend fun fetchCollection(collection: AccountMusicCollection): PlaylistDetails? {
            val asPlaylist = YouTubeMusicService.fetchPlaylistDetails(collection.id)
            if (asPlaylist != null && asPlaylist.tracks.isNotEmpty()) return asPlaylist
            return if (collection.isAlbum) YouTubeMusicService.fetchAlbumDetails(collection.id) else asPlaylist
        }

        /**
         * The channel behind each artist page. Most artists already carry one; the rest are looked
         * up one at a time, which is only needed for the few whose id is a radio or a shelf.
         */
        private suspend fun resolveArtistChannels(artists: List<YTItem>): Map<String, String> {
            val unresolved =
                artists
                    .filterIsInstance<ArtistItem>()
                    .filter { it.channelId == null && !it.id.startsWith("UC") }
                    .map { it.id }
                    .distinct()
            if (unresolved.isEmpty()) return emptyMap()

            return coroutineScope {
                unresolved
                    .map { id -> async { id to YouTube.getChannelId(id) } }
                    .awaitAll()
                    .filter { (_, channelId) -> channelId.isNotBlank() }
                    .toMap()
            }
        }
    }
