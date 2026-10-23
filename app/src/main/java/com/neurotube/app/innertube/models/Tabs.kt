package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab> = emptyList(),
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer,
    ) {
        @Serializable
        data class TabRenderer(
            val title: String? = null,
            val content: Content? = null,
            val endpoint: NavigationEndpoint? = null,
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer? = null,
                val musicQueueRenderer: MusicQueueRenderer? = null,
            )
        }
    }
}
