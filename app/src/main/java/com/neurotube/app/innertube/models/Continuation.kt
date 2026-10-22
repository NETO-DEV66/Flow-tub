package com.neurotube.app.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Continuation(
    @JsonNames("nextContinuationData", "nextRadioContinuationData")
    val nextContinuationData: NextContinuationData? = null,
    // A library section hands out its next slice as a reload of the same section rather than an
    // append, so both forms carry a usable token.
    val reloadContinuationData: NextContinuationData? = null,
) {
    @Serializable
    data class NextContinuationData(
        val continuation: String,
    )
}

fun List<Continuation>.getContinuation() = firstOrNull()?.let { it.nextContinuationData ?: it.reloadContinuationData }?.continuation
