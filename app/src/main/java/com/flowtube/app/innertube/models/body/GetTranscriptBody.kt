package com.flowtube.app.innertube.models.body

import com.flowtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
