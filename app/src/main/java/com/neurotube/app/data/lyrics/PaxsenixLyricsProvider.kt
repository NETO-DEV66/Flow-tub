//==================================================================================================
//This implementation was based on metrolist's (https://github.com/MetrolistGroup/Metrolist)
//==================================================================================================

package com.neurotube.app.data.lyrics

import com.neurotube.app.data.lyrics.paxsenix.Paxsenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaxsenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<List<LyricsEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val context = com.neurotube.app.FlowTubeApplication.appContext
            Paxsenix.init(context)
            val lrc = Paxsenix.getLyrics(title, artist, duration, album).getOrThrow()
            LyricsUtils.parseLyrics(lrc)
        }
    }
}
