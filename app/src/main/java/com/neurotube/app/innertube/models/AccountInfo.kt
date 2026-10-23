package com.neurotube.app.innertube.models

data class AccountInfo(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val thumbnailUrl: String? = null,
)
