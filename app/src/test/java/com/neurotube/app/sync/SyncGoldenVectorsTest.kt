package com.neurotube.app.sync

import com.neurotube.app.sync.canonical.CanonicalPlaylist
import com.neurotube.app.sync.canonical.CanonicalPlaylistItem
import com.neurotube.app.sync.crypto.SyncBytes
import com.neurotube.app.sync.crypto.SyncCrypto
import com.neurotube.app.sync.protocol.SyncSerialization
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The shared flowtube-sync/1 golden vectors (addendum §11), mirrored in
 * `notes/sync/fixtures/golden_vectors.json`. Any drift in Android's HKDF, SAS, AES-GCM byte layout,
 * or canonical-JSON key ordering fails here; the desktop (Rust) asserts the **same** fixture, so the
 * union is the cross-platform anti-drift gate. Update only with a protocol version bump.
 */
class SyncGoldenVectorsTest {

    private val master = SyncBytes.fromHex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
    private val sid = SyncBytes.fromHex("a0a1a2a3a4a5a6a7a8a9aaabacadaeaf")

    @Test
    fun directional_keys_match_golden() {
        val keys = SyncCrypto.deriveKeys(master, sid)
        assertEquals("2a91f4e35756263e4b75c4ab485dac5603494b5761269719534c62daef387ac6", SyncBytes.toHex(keys.hostToClient))
        assertEquals("937e55d08128372b68bcc0594b990bd9414bfc0cfd4ae7403afaabb194923ea9", SyncBytes.toHex(keys.clientToHost))
    }

    @Test
    fun sas_matches_golden() {
        assertEquals("660529", SyncCrypto.sas(master, sid))
    }

    @Test
    fun raw_gcm_seal_matches_golden() {
        val keys = SyncCrypto.deriveKeys(master, sid)
        val nonce = SyncBytes.fromHex("0102030405060708090a0b0c")
        val aad = SyncBytes.fromHex("01a0a1a2a3a4a5a6a7a8a9aaabacadaeaf100000000000000003")
        val ct = SyncCrypto.seal(keys.hostToClient, nonce, "flowtube-sync golden".toByteArray(Charsets.UTF_8), aad)
        assertEquals("42620428485432cf9ea8802b3daee4fbc5cd8c87554a8208eaba76738623018ea719d618", SyncBytes.toHex(ct))
        // And it must open back to the plaintext (interop both directions).
        assertEquals("flowtube-sync golden", String(SyncCrypto.open(keys.hostToClient, nonce, ct, aad), Charsets.UTF_8))
    }

    @Test
    fun canonical_playlist_matches_golden() {
        val pl = CanonicalPlaylist(
            syncId = "p1", origin = "local", youtubeId = null, title = "Gym", description = "",
            isMusic = false, isUserCreated = true, isProtected = false,
            createdAtMs = 1781000000000L, updatedHlc = "100:0:aaa", deleted = false,
            items = listOf(
                CanonicalPlaylistItem(
                    videoId = "v1", position = 0, addedAtMs = 1, deleted = false, title = "A",
                    channelName = "c", channelId = "uc", thumbnailUrl = "", durationSeconds = 212,
                    isMusic = false, hlc = "100:0:aaa",
                ),
            ),
        )
        val wire = SyncSerialization.encodePlaylists(listOf(pl))
        val expected =
            """{"createdAtMs":1781000000000,"deleted":false,"description":"","isMusic":false,"isProtected":false,"isUserCreated":true,"items":[{"addedAtMs":1,"channelId":"uc","channelName":"c","deleted":false,"durationSeconds":212,"hlc":"100:0:aaa","isMusic":false,"position":0,"thumbnailUrl":"","title":"A","videoId":"v1"}],"origin":"local","syncId":"p1","title":"Gym","updatedHlc":"100:0:aaa","youtubeId":null}"""
        assertEquals("canonical JSON must be sorted-key compact", expected, wire.lines.first())
        assertEquals("a0534d548eb59e88a7125e803f5d743e55d626bbca45a8622c57d44086e1b528", wire.hash)
    }
}
