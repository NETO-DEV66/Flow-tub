package com.neurotube.app.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer,
) {
    @Serializable
    data class MenuRenderer(
        val items: List<Item>? = null,
        val topLevelButtons: List<TopLevelButton>? = null,
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            val menuServiceItemRenderer: MenuServiceItemRenderer? = null,
            val toggleMenuServiceItemRenderer: ToggleMenuServiceRenderer? = null,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val text: Runs,
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class MenuServiceItemRenderer(
                val text: Runs,
                val icon: Icon,
                val serviceEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class ToggleMenuServiceRenderer(
                val defaultIcon: Icon,
                val defaultServiceEndpoint: DefaultServiceEndpoint,
                val toggledServiceEndpoint: ToggledServiceEndpoint? = null,
            )
        }

        @Serializable
        data class TopLevelButton(
            val buttonRenderer: ButtonRenderer? = null,
        ) {
            @Serializable
            data class ButtonRenderer(
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )
        }
    }
}

fun Menu?.watchPlaylistEndpointFor(iconType: String): WatchEndpoint? =
    this
        ?.menuRenderer
        ?.items
        ?.firstOrNull { it.menuNavigationItemRenderer?.icon?.iconType == iconType }
        ?.menuNavigationItemRenderer
        ?.navigationEndpoint
        ?.watchPlaylistEndpoint
