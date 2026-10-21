package com.neurotube.app.account

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YouTubeCookieExtractorTest {
    @Test
    fun `a full google session keeps only the identity cookies`() {
        val jar =
            "SID=abc; HSID=def; SSID=ghi; APISID=jkl; SAPISID=mno; " +
                "VISITOR_INFO1_LIVE=tracking; PREF=f1%3D50000000; SOCS=CAISEwgDEgk; " +
                "__Secure-1PSID=p1; __Secure-3PSID=p3; __Secure-1PAPISID=pa1; __Secure-3PAPISID=pa3"

        val session = YouTubeCookieExtractor.extractSession(jar)

        assertThat(session.keys)
            .containsExactly(
                "SID",
                "HSID",
                "SSID",
                "APISID",
                "SAPISID",
                "__Secure-1PSID",
                "__Secure-3PSID",
                "__Secure-1PAPISID",
                "__Secure-3PAPISID",
            )
        assertThat(session).doesNotContainKey("VISITOR_INFO1_LIVE")
        assertThat(session).doesNotContainKey("PREF")
        assertThat(session).doesNotContainKey("SOCS")
    }

    @Test
    fun `a session without SAPISID is not usable`() {
        val anonymous = "VISITOR_INFO1_LIVE=abc; PREF=f6%3D400; SOCS=CAISEwgDEgk"

        assertThat(YouTubeCookieExtractor.hasSession(anonymous)).isFalse()
        assertThat(YouTubeCookieExtractor.hasSession("SID=abc; HSID=def")).isFalse()
        assertThat(YouTubeCookieExtractor.hasSession(null)).isFalse()
        assertThat(YouTubeCookieExtractor.hasSession("")).isFalse()
    }

    @Test
    fun `a session with SAPISID is usable`() {
        assertThat(YouTubeCookieExtractor.hasSession("SAPISID=mno; SID=abc")).isTrue()
        assertThat(YouTubeCookieExtractor.hasSession("  SAPISID = mno ;  SID = abc ")).isTrue()
    }

    @Test
    fun `toCookieHeader serializes the kept cookies in jar order`() {
        val header = YouTubeCookieExtractor.toCookieHeader("SID=abc; PREF=x; SAPISID=mno; HSID=def")

        assertThat(header).isEqualTo("SID=abc; SAPISID=mno; HSID=def")
    }

    @Test
    fun `toCookieHeader is null when nothing usable survives`() {
        assertThat(YouTubeCookieExtractor.toCookieHeader("PREF=x; SOCS=y")).isNull()
        assertThat(YouTubeCookieExtractor.toCookieHeader(null)).isNull()
        assertThat(YouTubeCookieExtractor.toCookieHeader("   ")).isNull()
    }

    @Test
    fun `malformed pairs are ignored rather than corrupting the header`() {
        val jar = "SAPISID=mno; =orphan; NOVALUE; SID=abc; VISITOR_INFO1_LIVE="

        val header = YouTubeCookieExtractor.toCookieHeader(jar)

        assertThat(header).isEqualTo("SAPISID=mno; SID=abc")
    }

    @Test
    fun `a round trip through the extractor is idempotent`() {
        val jar = "SAPISID=mno; SID=abc; PREF=x; __Secure-3PSIDTS=ts"

        val once = YouTubeCookieExtractor.toCookieHeader(jar)
        val twice = YouTubeCookieExtractor.toCookieHeader(once)

        assertThat(twice).isEqualTo(once)
    }

    @Test
    fun `dataSyncId keeps only the part before the separator`() {
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("ABC123||XYZ"))
            .isEqualTo("ABC123")
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("ABC123")).isEqualTo("ABC123")
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("  ABC123 ||")).isEqualTo("ABC123")
    }

    @Test
    fun `dataSyncId is null when the page had nothing usable`() {
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId(null)).isNull()
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("")).isNull()
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("||")).isNull()
        assertThat(YouTubeCookieExtractor.normalizeDataSyncId("   ")).isNull()
    }
}
