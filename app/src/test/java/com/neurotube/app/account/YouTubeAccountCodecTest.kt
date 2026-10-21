package com.neurotube.app.account

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * A saved session has to come back whole. The visitor identity is the half that is easy to drop and
 * expensive to lose: InnerTube validates the request context against it, so a cookie restored
 * without one is refused and the app looks signed out after every restart.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class YouTubeAccountCodecTest {
    @Test
    fun `a session round trips with its visitor identity`() {
        val account =
            YouTubeAccount(
                name = "Test Channel",
                email = "test@example.com",
                channelHandle = "@test",
                thumbnailUrl = "https://example.com/a.jpg",
                cookieHeader = "SAPISID=mno; SID=abc",
                visitorData = "CgtWSVNJVE9S",
                dataSyncId = "ABC123",
            )

        assertThat(YouTubeAccountCodec.decode(YouTubeAccountCodec.encode(account)))
            .isEqualTo(account)
    }

    @Test
    fun `absent fields stay absent rather than becoming empty strings`() {
        val account =
            YouTubeAccount(
                name = "Test Channel",
                email = null,
                channelHandle = null,
                thumbnailUrl = null,
                cookieHeader = "SAPISID=mno",
            )

        val decoded = YouTubeAccountCodec.decode(YouTubeAccountCodec.encode(account))

        assertThat(decoded).isEqualTo(account)
        assertThat(decoded.visitorData).isNull()
        assertThat(decoded.dataSyncId).isNull()
        assertThat(decoded.email).isNull()
    }

    @Test
    fun `a payload written before the visitor identity existed still loads`() {
        val legacy = """{"name":"Old","cookie":"SAPISID=mno"}""".toByteArray(Charsets.UTF_8)

        val decoded = YouTubeAccountCodec.decode(legacy)

        assertThat(decoded.name).isEqualTo("Old")
        assertThat(decoded.cookieHeader).isEqualTo("SAPISID=mno")
        assertThat(decoded.visitorData).isNull()
        assertThat(decoded.dataSyncId).isNull()
    }
}
