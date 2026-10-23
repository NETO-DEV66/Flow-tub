package com.neurotube.app.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

/**
 * The diagnostics exist to make a failed import readable after the fact. What matters is that the
 * shape of a real response is summarised without its body, that a sign-in document is recognised,
 * and that a failure keeps the error's class and message rather than a bare flag.
 */
class ImportDiagnosticsTest {
    private fun json(body: String) = Json.parseToJsonElement(body)

    private fun fixture(name: String): JsonObject =
        javaClass.classLoader!!.getResourceAsStream("music/$name")!!
            .bufferedReader()
            .use { Json.parseToJsonElement(it.readText()) } as JsonObject

    @Test
    fun `a lockup library response is summarised by its markers`() {
        val markers = fixture("library_playlists_lockup.json").describeMarkers()

        assertThat(markers).contains("lockupViewModel=3")
        assertThat(markers).contains("gridRenderer=1")
        assertThat(markers).contains("contentId=3")
    }

    @Test
    fun `a legacy library response is distinguished from a lockup one`() {
        val legacy = fixture("library_playlists_large.json").describeMarkers()
        val lockup = fixture("library_playlists_lockup.json").describeMarkers()

        assertThat(legacy).contains("musicTwoRowItemRenderer=5")
        assertThat(legacy).doesNotContain("lockupViewModel")
        assertThat(lockup).contains("lockupViewModel")
        assertThat(lockup).doesNotContain("musicTwoRowItemRenderer")
    }

    @Test
    fun `the shape line names the renderers a response is built from`() {
        val shape = fixture("account_playlists.json").describeShape()

        assertThat(shape).contains("lockup=4")
        assertThat(shape).contains("grid=1")
        assertThat(shape).contains("sectionList=1")
    }

    @Test
    fun `a music response reports the row types it carries`() {
        val types = fixture("library_playlists_lockup.json").describeContentTypes()

        assertThat(types).contains("LOCKUP_CONTENT_TYPE_PLAYLIST=3")
    }

    @Test
    fun `a sign-in document is recognised as signed out`() {
        val signIn =
            json(
                """
                {"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
                {"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
                {"signInEndpoint":{}}]}}}}]}}}
                """.trimIndent(),
            )

        assertThat(signIn.looksSignedOut()).isTrue()
    }

    @Test
    fun `a page carrying real rows is not read as signed out`() {
        assertThat(fixture("library_playlists_lockup.json").looksSignedOut()).isFalse()
    }

    @Test
    fun `a failure keeps the error class and message`() {
        val described = describeFailure(IllegalStateException("no lockup rows"))

        assertThat(described).isEqualTo("IllegalStateException: no lockup rows")
    }

    @Test
    fun `the log keeps the last import's lines and can be dumped`() {
        ImportDiagnostics.begin()
        ImportDiagnostics.record("step LIKED_SONGS: found=42")
        ImportDiagnostics.end()

        val dump = ImportDiagnostics.dump()

        assertThat(dump).contains("import: start")
        assertThat(dump).contains("step LIKED_SONGS: found=42")
    }

    @Test
    fun `a new import drops the previous one's lines`() {
        ImportDiagnostics.begin()
        ImportDiagnostics.record("step HISTORY: found=7")
        ImportDiagnostics.begin()

        assertThat(ImportDiagnostics.dump()).doesNotContain("step HISTORY: found=7")
    }
}
