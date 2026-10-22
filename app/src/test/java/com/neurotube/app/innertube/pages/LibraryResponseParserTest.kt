package com.neurotube.app.innertube.pages

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
}
