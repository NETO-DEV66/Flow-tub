package com.neurotube.app.account

import com.google.common.truth.Truth.assertThat
import com.neurotube.app.data.model.Channel
import com.neurotube.app.data.model.Video
import com.neurotube.app.innertube.models.AlbumItem
import com.neurotube.app.innertube.models.Artist
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.PlaylistItem
import com.neurotube.app.innertube.models.SongItem
import com.neurotube.app.innertube.pages.AccountVideoCollection
import org.junit.Test

/**
 * The mappers turn the pages YouTube returns into the app's own stores. What matters here is which
 * fields survive the trip and which entries are dropped, since a wrong id becomes a subscription
 * that never yields an upload.
 */
class AccountLibraryMapperTest {
    private fun channel(
        id: String,
        name: String,
    ) = Channel(id = id, name = name, thumbnailUrl = "https://yt3.ggpht.com/$id", subscriberCount = 0)

    private fun artist(
        id: String,
        title: String,
        channelId: String? = null,
    ) = ArtistItem(
        id = id,
        title = title,
        thumbnail = "https://yt3.ggpht.com/$id",
        channelId = channelId,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

    private fun song(
        id: String,
        title: String,
        artists: List<Artist> = listOf(Artist("Some Artist", "UCartist")),
        duration: Int? = 200,
    ) = SongItem(
        id = id,
        title = title,
        artists = artists,
        thumbnail = "https://i.ytimg.com/vi/$id/hq.jpg",
        duration = duration,
    )

    @Test
    fun `channels keep the fields the subscription store needs`() {
        val subscriptions = listOf(channel("UCabc", "Lofi Girl")).toSubscribedChannels()

        assertThat(subscriptions).hasSize(1)
        val subscription = subscriptions.single()
        assertThat(subscription.channelId).isEqualTo("UCabc")
        assertThat(subscription.channelName).isEqualTo("Lofi Girl")
        assertThat(subscription.channelThumbnail).isEqualTo("https://yt3.ggpht.com/UCabc")
    }

    @Test
    fun `a channel without an id is dropped`() {
        val subscriptions =
            listOf(channel("", "Nameless"), channel("UCabc", "Lofi Girl"), channel("UCabc", "Lofi Girl"))
                .toSubscribedChannels()

        assertThat(subscriptions.map { it.channelId }).containsExactly("UCabc")
    }

    @Test
    fun `an artist's own channel id is used as it stands`() {
        val subscriptions = listOf(artist("UCabc", "Lofi Girl")).toSubscribedArtists()

        assertThat(subscriptions.single().channelId).isEqualTo("UCabc")
        assertThat(subscriptions.single().isMusic).isTrue()
    }

    @Test
    fun `an artist carrying a channel field uses that instead of its page id`() {
        val subscriptions = listOf(artist("MPREb_radio", "Lofi Girl", channelId = "UCabc")).toSubscribedArtists()

        assertThat(subscriptions.single().channelId).isEqualTo("UCabc")
    }

    @Test
    fun `an artist page resolved by the caller takes the resolved channel`() {
        val subscriptions =
            listOf(artist("UCsomebodyelse", "Lofi Girl")).toSubscribedArtists(mapOf("UCsomebodyelse" to "UCabc"))

        assertThat(subscriptions.single().channelId).isEqualTo("UCabc")
    }

    @Test
    fun `an artist whose channel cannot be found is left out`() {
        val subscriptions = listOf(artist("MPREb_radio", "Unknown")).toSubscribedArtists()

        assertThat(subscriptions).isEmpty()
    }

    @Test
    fun `liked songs keep the order the library listed them in, newest first`() {
        val likes = listOf(song("v1", "First"), song("v2", "Second")).toLikedSongs(now = 1000L)

        assertThat(likes.map { it.videoId }).containsExactly("v1", "v2").inOrder()
        assertThat(likes.map { it.likedAt }).containsExactly(1000L, 999L).inOrder()
    }

    @Test
    fun `a liked song carries its artist, length and thumbnail`() {
        val like = listOf(song("v1", "First", duration = 245)).toLikedSongs(now = 1000L).single()

        assertThat(like.channelName).isEqualTo("Some Artist")
        assertThat(like.channelId).isEqualTo("UCartist")
        assertThat(like.durationSeconds).isEqualTo(245)
        assertThat(like.thumbnail).isEqualTo("https://i.ytimg.com/vi/v1/hq.jpg")
        assertThat(like.isMusic).isTrue()
    }

    @Test
    fun `a song without a length is kept with a zero length`() {
        val like = listOf(song("v1", "First", duration = null)).toLikedSongs(now = 1000L).single()

        assertThat(like.durationSeconds).isEqualTo(0)
    }

    @Test
    fun `a song liked twice is listed once`() {
        val likes = listOf(song("v1", "First"), song("v1", "First")).toLikedSongs(now = 1000L)

        assertThat(likes).hasSize(1)
    }

    private fun playlist(
        id: String,
        title: String,
    ) = PlaylistItem(
        id = id,
        title = title,
        author = null,
        songCountText = null,
        thumbnail = "https://i.ytimg.com/vi/$id/hq.jpg",
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

    private fun album(
        browseId: String,
        title: String,
    ) = AlbumItem(
        browseId = browseId,
        playlistId = "OLAK5uy_$browseId",
        title = title,
        artists = null,
        thumbnail = "https://i.ytimg.com/vi/$browseId/hq.jpg",
    )

    @Test
    fun `a saved playlist and album are read as collections`() {
        val collections = listOf(playlist("PLabc", "Chill"), album("MPREb_xyz", "An Album")).toSavedMusicCollections()

        assertThat(collections.map { it.id }).containsExactly("PLabc", "MPREb_xyz").inOrder()
        assertThat(collections[0].isAlbum).isFalse()
        assertThat(collections[1].isAlbum).isTrue()
    }

    @Test
    fun `an album is saved under its own browse id, not its playlist id`() {
        val collections = listOf(album("MPREb_xyz", "An Album")).toSavedMusicCollections()

        assertThat(collections.single().id).isEqualTo("MPREb_xyz")
    }

    @Test
    fun `the library's own generated entries are not collections`() {
        val collections =
            listOf(
                playlist("LM", "Liked music"),
                playlist("SE", "My Supermix"),
                playlist("LL", "A playlist"),
                playlist("PLabc", "Chill"),
            ).toSavedMusicCollections()

        assertThat(collections.map { it.id }).containsExactly("PLabc")
    }

    @Test
    fun `a collection without a name is left out`() {
        val collections = listOf(playlist("PLabc", ""), playlist("PLdef", "Chill")).toSavedMusicCollections()

        assertThat(collections.map { it.id }).containsExactly("PLdef")
    }

    @Test
    fun `an empty library reports nothing`() {
        assertThat(AccountImportReport().isEmpty).isTrue()
        assertThat(AccountImportReport(likedSongs = 1).isEmpty).isFalse()
        assertThat(AccountImportReport(videoPlaylists = 1).isEmpty).isFalse()
        assertThat(AccountImportReport(watchLater = 1).isEmpty).isFalse()
        assertThat(AccountImportReport(history = 1).isEmpty).isFalse()
    }

    @Test
    fun `the watch later and liked lists YouTube owns are not imported as playlists`() {
        val playlists =
            listOf(
                AccountVideoCollection("WL", "Watch Later", "", 12),
                AccountVideoCollection("LL", "Liked videos", "", 40),
                AccountVideoCollection("PLabc", "Chill", "", 7),
            ).toSavedVideoPlaylists()

        assertThat(playlists.map { it.id }).containsExactly("PLabc")
    }

    @Test
    fun `a video playlist without a name is left out`() {
        val playlists =
            listOf(
                AccountVideoCollection("PLabc", "", "", 7),
                AccountVideoCollection("PLdef", "Chill", "", 7),
            ).toSavedVideoPlaylists()

        assertThat(playlists.map { it.id }).containsExactly("PLdef")
    }

    @Test
    fun `watched videos become history entries, newest first`() {
        val now = 1_700_000_000_000L
        val entries =
            listOf(
                video("v1", "First watched"),
                video("v2", "Second watched"),
            ).toHistoryEntries(now)

        assertThat(entries.map { it.videoId }).containsExactly("v1", "v2").inOrder()
        assertThat(entries[0].timestamp).isEqualTo(now)
        assertThat(entries[1].timestamp).isEqualTo(now - 1)
        assertThat(entries[0].title).isEqualTo("First watched")
    }

    @Test
    fun `a video watched twice is one history entry`() {
        val entries = listOf(video("v1", "Once"), video("v1", "Once")).toHistoryEntries(1L)

        assertThat(entries.map { it.videoId }).containsExactly("v1")
    }

    private fun video(
        id: String,
        title: String,
    ) = Video(
        id = id,
        title = title,
        channelName = "A Channel",
        channelId = "UCchannel",
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hq.jpg",
        duration = 210,
        viewCount = 0,
        uploadDate = "",
    )
}
