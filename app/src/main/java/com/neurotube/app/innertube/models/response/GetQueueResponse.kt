package com.neurotube.app.innertube.models.response

import com.neurotube.app.innertube.models.PlaylistPanelRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueResponse(
    val queueDatas: List<QueueData> = emptyList(),
) {
    @Serializable
    data class QueueData(
        val content: PlaylistPanelRenderer.Content,
    )
}
