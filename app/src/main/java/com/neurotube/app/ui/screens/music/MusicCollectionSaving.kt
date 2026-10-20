package com.neurotube.app.ui.screens.music

import com.neurotube.app.data.local.PlaylistRepository
import com.neurotube.app.data.music.model.PlaylistDetails
import com.neurotube.app.ui.screens.music.collection.toStoredVideo

/** Saves an album or playlist to the library with its tracks, so it plays offline from the library. */
internal suspend fun PlaylistRepository.saveMusicCollection(details: PlaylistDetails) {
    saveExternalMusicPlaylist(
        id = details.id,
        name = details.title,
        description = details.description.orEmpty(),
        thumbnailUrl = details.thumbnailUrl,
    )
    addVideosToPlaylist(details.id, details.tracks.map { it.toStoredVideo() })
}
