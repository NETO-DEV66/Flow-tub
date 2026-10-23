package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ContinuationItemRenderer(
    val continuationEndpoint: ContinuationEndpoint? = null,
) {
    @Serializable
    data class ContinuationEndpoint(
        val continuationCommand: ContinuationCommand? = null,
    ) {
        @Serializable
        data class ContinuationCommand(
            val token: String? = null,
        )
    }
}
