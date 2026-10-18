package com.flowtube.app.ui.screens.home

import com.flowtube.app.data.local.VideoHistoryEntry
import com.flowtube.app.data.model.Video

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val shorts: List<Video> = emptyList(),
    val continueWatchingVideos: List<VideoHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null,
    val isFlowFeed: Boolean = false,
    val lastRefreshTime: Long = 0L,
    /** Why each card is on Home; channel memory cards also say so under their title. */
    val origins: Map<String, HomeCardOrigin> = emptyMap(),
)
