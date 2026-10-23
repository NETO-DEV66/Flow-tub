package com.neurotube.app.innertube.pages

import com.neurotube.app.account.toSavedMusicCollections
import com.neurotube.app.account.toSubscribedArtists
import com.neurotube.app.innertube.models.AlbumItem
import com.neurotube.app.innertube.models.ArtistItem
import com.neurotube.app.innertube.models.PlaylistItem
import com.neurotube.app.innertube.models.response.BrowseResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing is checked against responses captured from a real music library, because the library's
 * shape differs from search and artist pages: saved albums carry a plain watch endpoint, and pages
 * after the first arrive as a section list continuation.
 */
@OptIn(ExperimentalSerializationApi::class)
class LibraryResponseParserTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun response(name: String): BrowseResponse {
        val text =
            checkNotNull(javaClass.classLoader?.getResourceAsStream("music/$name")) {
                "missing fixture $name"
            }.bufferedReader().readText()
        // Some captures were trimmed to the fields under test and dropped the envelope.
        val patched = if (text.contains("\"responseContext\"")) text else text.replaceFirst("{", """{"responseContext":{},""")
        return json.decodeFromString(BrowseResponse.serializer(), patched)
    }

    private fun decode(json: String): BrowseResponse = this.json.decodeFromString(BrowseResponse.serializer(), json)

    @Test
    fun `a one-section page reads the next token that hangs off the section list`() {
        val page =
            LibraryResponseParser.pageOf(
                decode(
                    """
                    {"responseContext":{},
                     "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                       {"tabRenderer":{"content":{"sectionListRenderer":{
                         "contents":[{"gridRenderer":{"items":[]}}],
                         "continuations":[{"reloadContinuationData":{"continuation":"SECTION_TOKEN"}}]
                       }}}}
                     ]}}}
                    """.trimIndent(),
                ),
            )

        assertEquals("SECTION_TOKEN", page.continuation)
    }

    @Test
    fun `a page whose grid appends its token as the last item still continues`() {
        val page =
            LibraryResponseParser.pageOf(
                decode(
                    """
                    {"responseContext":{},
                     "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                       {"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
                         {"gridRenderer":{"items":[
                           {"continuationItemRenderer":{"continuationEndpoint":
                             {"continuationCommand":{"token":"TRAILING_TOKEN"}}}}
                         ]}}
                       ]}}}}
                     ]}}}
                    """.trimIndent(),
                ),
            )

        assertEquals("TRAILING_TOKEN", page.continuation)
    }

    @Test
    fun `a multi-section page does not hand the list token to its first section`() {
        val page =
            LibraryResponseParser.pageOf(
                decode(
                    """
                    {"responseContext":{},
                     "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                       {"tabRenderer":{"content":{"sectionListRenderer":{
                         "contents":[
                           {"gridRenderer":{"items":[]}},
                           {"gridRenderer":{"items":[]}}
                         ],
                         "continuations":[{"reloadContinuationData":{"continuation":"LIST_TOKEN"}}]
                       }}}}
                     ]}}}
                    """.trimIndent(),
                ),
            )

        assertEquals(null, page.continuation)
    }

    @Test
    fun `saved playlists keep the playable ones and skip the create tile`() {
        val page = LibraryResponseParser.pageOf(response("library_playlists_large.json"))

        val playlists = page.items.filterIsInstance<PlaylistItem>()
        assertEquals(4, playlists.size)
        assertTrue(playlists.any { it.title == "Liked Music" })
        assertTrue("the create tile has nothing to open", page.items.none { it is PlaylistItem && it.title == "New playlist" })
    }

    @Test
    fun `saved albums survive the plain watch endpoint on their play tile`() {
        val page = LibraryResponseParser.pageOf(response("library_albums_large.json"))

        val albums = page.items.filterIsInstance<AlbumItem>()
        assertEquals(3, albums.size)
        assertEquals("MPREb_E7NpKw54NIT", albums.first().browseId)
        assertEquals("OLAK5uy_m8Yan_MSAia_ZYmZpl6xm1AINDg8LF1NI", albums.first().playlistId)
    }

    @Test
    fun `saved artists are kept even though they carry no shuffle or radio action`() {
        val page = LibraryResponseParser.pageOf(response("library_artists_library.json"))

        val artists = page.items.filterIsInstance<ArtistItem>()
        assertEquals(3, artists.size)
        assertEquals("Kendrick Lamar", artists.first().title)
    }

    @Test
    fun `a grid continuation is read as a page of albums`() {
        val page = LibraryResponseParser.continuationPageOf(response("cont_albums.json"))

        assertEquals(2, page.items.size)
        assertTrue(page.items.all { it is AlbumItem })
    }

    @Test
    fun `a section list continuation keeps the token that continues the library`() {
        val page = LibraryResponseParser.continuationPageOf(response("cont_playlists.json"))

        assertEquals(3, page.items.size)
        assertTrue(page.items.all { it is PlaylistItem })
        assertNotNull("the next token must survive, or the library stops after one page", page.continuation)
    }

    @Test
    fun `a shelf continuation prefers the token on the shelf`() {
        val page = LibraryResponseParser.continuationPageOf(response("cont_artists.json"))

        assertEquals(25, page.items.size)
        assertTrue(page.items.all { it is ArtistItem })
        assertNotNull(page.continuation)
    }

    @Test
    fun `a library served as lockup rows still imports its saved playlists`() {
        val page = LibraryResponseParser.pageOf(response("library_playlists_lockup.json"))

        val playlists = page.items.filterIsInstance<PlaylistItem>()
        assertEquals("every lockup row must be read, not walked past", 3, playlists.size)
        assertTrue(playlists.any { it.title == "Sam Sulek Playlist" })
        assertTrue("a lockup playlist must carry a thumbnail", playlists.all { !it.thumbnail.isNullOrBlank() })
    }

    @Test
    fun `a lockup podcast row is imported as a playlist`() {
        val page = LibraryResponseParser.pageOf(response("library_podcasts_lockup.json"))

        assertEquals(1, page.items.filterIsInstance<PlaylistItem>().size)
    }

    @Test
    fun `a library served as lockup artist rows imports its artists`() {
        val page = LibraryResponseParser.pageOf(response("library_artists_lockup.json"))

        val artists = page.items.filterIsInstance<ArtistItem>()
        assertEquals(3, artists.size)
        assertEquals("Ariana Grande", artists.first().title)
        assertTrue("an artist row must keep a channel id", artists.all { it.channelId != null })
    }

    @Test
    fun `a library served as lockup album rows imports its albums`() {
        val page = LibraryResponseParser.pageOf(response("library_albums_lockup.json"))

        val albums = page.items.filterIsInstance<AlbumItem>()
        assertEquals(2, albums.size)
        assertEquals("MPREb_E7NpKw54NIT", albums.first().browseId)
        assertEquals(
            "the album's playlist id comes from its play action",
            "OLAK5uy_m8Yan_MSAia_ZYmZpl6xm1AINDg8LF1NI",
            albums.first().playlistId,
        )
    }

    @Test
    fun `a lockup grid still reads its continuation`() {
        val page =
            LibraryResponseParser.pageOf(
                decode(
                    """
                    {"responseContext":{},
                     "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                       {"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
                         {"gridRenderer":{"items":[
                           {"lockupViewModel":{"contentId":"PL1","contentType":"LOCKUP_CONTENT_TYPE_PLAYLIST",
                             "metadata":{"lockupMetadataViewModel":{"title":{"content":"One"}}}}},
                           {"continuationItemRenderer":{"continuationEndpoint":
                             {"continuationCommand":{"token":"LOCKUP_NEXT"}}}}
                         ]}}
                       ]}}}}
                     ]}}}
                    """.trimIndent(),
                ),
            )

        assertEquals(1, page.items.size)
        assertEquals("LOCKUP_NEXT", page.continuation)
    }

    @Test
    fun `lockup rows wrapped in an item section are still read`() {
        val page =
            LibraryResponseParser.pageOf(
                decode(
                    """
                    {"responseContext":{},
                     "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                       {"tabRenderer":{"content":{"sectionListRenderer":{
                         "contents":[{"itemSectionRenderer":{"contents":[
                           {"lockupViewModel":{"contentId":"PL1","contentType":"LOCKUP_CONTENT_TYPE_PLAYLIST",
                             "metadata":{"lockupMetadataViewModel":{"title":{"content":"One"}}}}}
                         ]}}],
                         "continuations":[{"reloadContinuationData":{"continuation":"WRAPPED_NEXT"}}]
                       }}}}
                     ]}}}
                    """.trimIndent(),
                ),
            )

        assertEquals(1, page.items.size)
        assertTrue(page.items.single() is PlaylistItem)
        assertEquals("WRAPPED_NEXT", page.continuation)
    }

    @Test
    fun `a continuation of lockup rows is read with its next token`() {
        val page =
            LibraryResponseParser.continuationPageOf(
                decode(
                    """
                    {"responseContext":{},
                     "continuationContents":{"gridContinuation":{
                       "items":[
                         {"lockupViewModel":{"contentId":"MPREb_1","contentType":"LOCKUP_CONTENT_TYPE_ALBUM",
                           "itemPlayback":{"inlinePlayerData":{"onSelect":{"innertubeCommand":{"watchEndpoint":
                             {"videoId":"v1","playlistId":"OLAK5uy_1"}}}}},
                           "metadata":{"lockupMetadataViewModel":{"title":{"content":"An Album"}}}}},
                         {"continuationItemRenderer":{"continuationEndpoint":
                           {"continuationCommand":{"token":"NEXT_ALBUMS"}}}}
                       ]
                     }}}
                    """.trimIndent(),
                ),
            )

        val album = page.items.single() as AlbumItem
        assertEquals("MPREb_1", album.browseId)
        assertEquals("OLAK5uy_1", album.playlistId)
        assertEquals("NEXT_ALBUMS", page.continuation)
    }

    @Test
    fun `lockup artist rows reach the subscriptions store`() {
        val subscriptions = LibraryResponseParser.pageOf(response("library_artists_lockup.json")).items.toSubscribedArtists()

        assertEquals(3, subscriptions.size)
        assertTrue("an imported artist must be subscribed by channel id", subscriptions.all { it.channelId.startsWith("UC") })
    }

    @Test
    fun `lockup playlist rows reach the saved collections`() {
        val collections = LibraryResponseParser.pageOf(response("library_playlists_lockup.json")).items.toSavedMusicCollections()

        assertEquals("every saved playlist must survive as a collection", 3, collections.size)
        assertTrue(collections.all { it.id.isNotBlank() && it.title.isNotBlank() })
    }
}
