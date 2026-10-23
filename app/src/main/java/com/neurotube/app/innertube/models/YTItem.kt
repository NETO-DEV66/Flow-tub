package com.neurotube.app.innertube.models

import com.neurotube.app.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV

sealed class YTItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnail: String?
    abstract val explicit: Boolean
    abstract val shareLink: String
}

data class Artist(
    val name: String,
    val id: String? = null,
)

data class Album(
    val name: String,
    val id: String,
)

data class SongItem(
    override val id: String,
    override val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val duration: Int? = null,
    val musicVideoType: String? = null,
    val chartPosition: Int? = null,
    val chartChange: String? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
    val setVideoId: String? = null,
    val viewCountText: String? = null,
    val likeCountText: String? = null,
) : YTItem() {
    val isVideoSong: Boolean
        get() = musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV

    override val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"
}

data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    override val id: String = browseId,
    override val title: String,
    val artists: List<Artist>? = null,
    val year: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$playlistId"
}

data class PlaylistItem(
    override val id: String,
    override val title: String,
    val author: Artist? = null,
    val songCountText: String? = null,
    override val thumbnail: String?,
    val playEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint? = null,
    val radioEndpoint: WatchEndpoint? = null,
    val isEditable: Boolean = false,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$id"
}

data class ArtistItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String?,
    val channelId: String? = null,
    val playEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint? = null,
    val radioEndpoint: WatchEndpoint? = null,
    val subscriberCountText: String? = null,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/channel/$id"
}

fun <T : YTItem> List<T>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.explicit }
    } else {
        this
    }

fun <T : YTItem> List<T>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it is SongItem && it.isVideoSong }
    } else {
        this
    }
