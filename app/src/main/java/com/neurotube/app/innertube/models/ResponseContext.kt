package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ResponseContext(
    // Both are optional in a real response — a music library page frequently omits them — and a
    // nullable field without a default is *required* by kotlinx, so one missing key would fail the
    // whole response. Defaulting them keeps a page readable.
    val visitorData: String? = null,
    val serviceTrackingParams: List<ServiceTrackingParam>? = null,
) {
    @Serializable
    data class ServiceTrackingParam(
        val params: List<Param> = emptyList(),
        val service: String,
    ) {
        @Serializable
        data class Param(
            val key: String,
            val value: String,
        )
    }
}
