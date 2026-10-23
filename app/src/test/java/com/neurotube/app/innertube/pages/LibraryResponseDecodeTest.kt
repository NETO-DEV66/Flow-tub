package com.neurotube.app.innertube.pages

import com.neurotube.app.innertube.models.response.BrowseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the signed-in library import.
 *
 * Every part of an import except liked songs used to come back empty. The cause was not the parser:
 * `YouTube.library` decoded the response with a `Json` that omitted `explicitNulls = false`, and
 * kotlinx treats a nullable property without a default as *required*, so the first absent key —
 * `responseContext.visitorData`, which a real library page does not send — failed the whole
 * response. The parser then saw no page at all.
 *
 * Two things are checked here. [strictJson] is the configuration that used to ship, so the models
 * must stay readable under it: that is what keeps a single missing key from failing a response
 * again. [productionJson] is the configuration that ships now.
 */
class LibraryResponseDecodeTest {
    /** The configuration that caused the bug: no `explicitNulls = false`, so absent keys fail. */
    private val strictJson = Json { ignoreUnknownKeys = true }

    /** The configuration the library path uses now, matching every other InnerTube decode site. */
    private val productionJson =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val fixtures =
        listOf(
            "library_playlists_lockup.json",
            "library_artists_lockup.json",
            "library_albums_lockup.json",
            "library_podcasts_lockup.json",
            "library_playlists_large.json",
            "library_artists_library.json",
            "library_albums_large.json",
            "account_playlists.json",
            "account_history.json",
            "cont_playlists.json",
            "cont_artists.json",
            "cont_albums.json",
        )

    private fun body(name: String): String {
        val text =
            checkNotNull(javaClass.classLoader?.getResourceAsStream("music/$name")) {
                "missing fixture $name"
            }.bufferedReader().readText()
        // Some captures were trimmed to the fields under test and dropped the envelope.
        return if (text.contains("\"responseContext\"")) text else text.replaceFirst("{", """{"responseContext":{},""")
    }

    private fun failuresUnder(json: Json): List<String> =
        fixtures.mapNotNull { name ->
            runCatching { json.decodeFromString(BrowseResponse.serializer(), body(name)) }
                .exceptionOrNull()
                ?.let { "$name -> ${it.javaClass.simpleName}: ${it.message}" }
        }

    @Test
    fun `the production library configuration reads every captured page`() {
        val failures = failuresUnder(productionJson)
        assertTrue("library pages failed to decode:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `a missing key never fails a captured page even without explicitNulls`() {
        val failures = failuresUnder(strictJson)
        assertTrue(
            "a single absent key must not fail a whole response:\n" + failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }
}
