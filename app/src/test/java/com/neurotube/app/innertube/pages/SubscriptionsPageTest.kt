package com.neurotube.app.innertube.pages

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * The subscriptions reader serves two pages that look nothing alike: the guide's flat list of
 * entries and the channels grid's cards. Both are read here from the shapes YouTube returns.
 */
class SubscriptionsPageTest {
    private fun parse(json: String) = Json.parseToJsonElement(json)

    private fun guideEntry(
        id: String,
        title: String,
        thumbnail: String = "https://yt3.ggpht.com/$id",
    ) = """
        {"guideEntryRenderer":{"channelId":"$id","title":{"simpleText":"$title"},
        "thumbnail":{"thumbnails":[{"url":"$thumbnail","width":88,"height":88}]}}}
        """.trimIndent()

    @Test
    fun `the guide's subscriptions are read as channels`() {
        val channels =
            parse(
                """
                {"items":[
                  {"guideSectionRenderer":{"items":[${guideEntry("UCabc", "Lofi Girl")}]}},
                  {"guideSectionRenderer":{"items":[${guideEntry("UCdef", "Kurzgesagt")}]}}
                ]}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels.map { it.name }).containsExactly("Lofi Girl", "Kurzgesagt").inOrder()
        assertThat(channels.first().id).isEqualTo("UCabc")
        assertThat(channels.first().thumbnailUrl).isEqualTo("https://yt3.ggpht.com/UCabc")
        assertThat(channels.first().url).isEqualTo("https://www.youtube.com/channel/UCabc")
    }

    @Test
    fun `the guide's own destinations are not subscriptions`() {
        val channels =
            parse(
                """
                {"items":[
                  {"guideEntryRenderer":{"title":{"simpleText":"Home"},
                    "navigationEndpoint":{"browseEndpoint":{"browseId":"FEwhat_to_watch"}}}},
                  {"guideEntryRenderer":{"title":{"simpleText":"Shorts"},
                    "navigationEndpoint":{"browseEndpoint":{"browseId":"FEshorts"}}}},
                  {"guideEntryRenderer":{"channelId":"UCabc","title":{"simpleText":"Lofi Girl"}}}
                ]}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels.map { it.id }).containsExactly("UCabc")
    }

    @Test
    fun `a channels grid is read from its cards`() {
        val channels =
            parse(
                """
                {"contents":{"gridRenderer":{"items":[
                  {"gridChannelRenderer":{"channelId":"UCabc","title":{"simpleText":"Lofi Girl"},
                    "thumbnail":{"thumbnails":[{"url":"https://yt3.ggpht.com/abc"}]}}}
                ]}}}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels.map { it.id }).containsExactly("UCabc")
        assertThat(channels.single().name).isEqualTo("Lofi Girl")
    }

    @Test
    fun `a channel carried by its browse endpoint keeps its id`() {
        val channels =
            parse(
                """
                {"items":[{"channelRenderer":{"title":{"simpleText":"Some Channel"},
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"UCxyz"}}}}]}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels.single().id).isEqualTo("UCxyz")
    }

    @Test
    fun `a channel seen twice is listed once`() {
        val channels =
            parse(
                """
                {"items":[
                  ${guideEntry("UCabc", "Lofi Girl")},
                  {"gridChannelRenderer":{"channelId":"UCabc","title":{"simpleText":"Lofi Girl"}}}
                ]}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels).hasSize(1)
    }

    @Test
    fun `an entry without an id or a name is skipped`() {
        val channels =
            parse(
                """
                {"items":[
                  {"guideEntryRenderer":{"title":{"simpleText":"No id here"}}},
                  {"guideEntryRenderer":{"channelId":"UCabc"}},
                  {"guideEntryRenderer":{"channelId":"UCabc","title":{"simpleText":"Lofi Girl"}}}
                ]}
                """.trimIndent(),
            ).toSubscribedChannels()

        assertThat(channels.map { it.name }).containsExactly("Lofi Girl")
    }

    @Test
    fun `the next page token is the one at the end of the list`() {
        val token =
            parse(
                """
                {"contents":{"gridRenderer":{"items":[
                  {"continuationItemRenderer":{"continuationEndpoint":
                    {"continuationCommand":{"token":"FIRST"}}}},
                  {"gridChannelRenderer":{"channelId":"UCabc","title":{"simpleText":"A"}}},
                  {"continuationItemRenderer":{"continuationEndpoint":
                    {"continuationCommand":{"token":"SECOND"}}}}
                ]}}}
                """.trimIndent(),
            ).nextChannelContinuation()

        assertThat(token).isEqualTo("SECOND")
    }

    @Test
    fun `the last page carries no token`() {
        val token =
            parse(
                """{"contents":{"gridRenderer":{"items":[
                  {"gridChannelRenderer":{"channelId":"UCabc","title":{"simpleText":"A"}}}
                ]}}}""",
            ).nextChannelContinuation()

        assertThat(token).isNull()
    }
}
