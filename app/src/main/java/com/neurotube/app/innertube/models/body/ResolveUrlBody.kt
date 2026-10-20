package com.neurotube.app.innertube.models.body

import com.neurotube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlBody(
    val context: Context,
    val url: String,
)
