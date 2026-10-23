package com.neurotube.app.innertube.pages

import com.neurotube.app.data.model.Channel
import com.neurotube.app.innertube.pages.renderer.forEachObject
import com.neurotube.app.innertube.pages.renderer.largestImageUrl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val CHANNEL_ID_PREFIX = "UC"

/** The `contentType` a channel's modern `lockupViewModel` carries. */
private const val LOCKUP_CONTENT_TYPE_CHANNEL = "LOCKUP_CONTENT_TYPE_CHANNEL"

/** One page of the viewer's subscriptions, with the token for the next one. */
data class SubscriptionsPage(
    val channels: List<Channel>,
    val continuation: String?,
)

/**
 * The channels a signed-in account follows, read from whichever page carried them.
 *
 * Two pages answer this and neither is complete on its own: the guide's subscriptions section is
 * short but always present, and `FEchannels` holds the full list a page at a time. Both render a
 * channel as a browse entry, so one reader serves both and the caller merges the results by id.
 *
 * Only ids that are channels are kept — the guide also carries "Home", "Shorts", "History" and
 * other destinations, which are browse ids of a different kind.
 */
internal fun JsonElement.toSubscribedChannels(): List<Channel> {
    val channels = LinkedHashMap<String, Channel>()
    forEachObject { node ->
        channelEntry(node)?.let { channels.putIfAbsent(it.id, it) }
    }
    return channels.values.toList()
}

/**
 * One channel, whatever renderer it arrived in: a guide entry, a grid's channel card, or — on the
 * newer pages — a channel `lockupViewModel`, which YouTube hands out in place of the older
 * `channelRenderer` family and which the reader would otherwise walk straight past, importing an
 * empty subscription list.
 */
private fun channelEntry(node: JsonObject): Channel? {
    node["lockupViewModel"]?.objectOrNull()?.let { lockup ->
        if (lockup["contentType"].stringOrNull() == LOCKUP_CONTENT_TYPE_CHANNEL) {
            return lockupChannel(lockup)
        }
    }
    val renderer =
        node["guideEntryRenderer"] as? JsonObject
            ?: node["gridChannelRenderer"] as? JsonObject
            ?: node["channelRenderer"] as? JsonObject
            ?: return null
    val id = renderer.channelId() ?: return null
    val name = renderer.channelName() ?: return null
    return Channel(
        id = id,
        name = name,
        thumbnailUrl = renderer["thumbnail"].largestImageUrl().orEmpty(),
        subscriberCount = 0,
        url = "https://www.youtube.com/channel/$id",
    )
}

/**
 * A channel carried by a `lockupViewModel`: `contentId` holds the channel id and the avatar hangs
 * under `metadata.lockupMetadataViewModel.image.decoratedAvatarViewModel`.
 */
private fun lockupChannel(lockup: JsonObject): Channel? {
    val id = lockup["contentId"].stringOrNull()?.takeIf { it.startsWith(CHANNEL_ID_PREFIX) } ?: return null
    val metadata = lockup["metadata"].objectOrNull()?.get("lockupMetadataViewModel").objectOrNull()
    val name = metadata?.get("title").youtubeText()?.takeIf(String::isNotBlank) ?: return null
    val avatar =
        metadata
            ?.get("image")
            .objectOrNull()
            ?.get("decoratedAvatarViewModel")
            .objectOrNull()
            ?.get("avatar")
            .objectOrNull()
            ?.get("avatarViewModel")
            .objectOrNull()
            ?.get("image")
            .objectOrNull()
            ?.get("sources")
            .arrayOrNull()
            ?.maxByOrNull {
                it
                    .objectOrNull()
                    ?.get("width")
                    .stringOrNull()
                    ?.toIntOrNull() ?: 0
            }?.objectOrNull()
            ?.get("url")
            .stringOrNull()
            ?.takeIf(String::isNotBlank)
    return Channel(
        id = id,
        name = name,
        thumbnailUrl = normalizeImageUrl(avatar.orEmpty()),
        subscriberCount = 0,
        url = "https://www.youtube.com/channel/$id",
    )
}

private fun JsonObject.channelId(): String? =
    this["channelId"].stringOrNull()?.takeIf { it.startsWith(CHANNEL_ID_PREFIX) }
        ?: this["navigationEndpoint"]
            .objectOrNull()
            ?.get("browseEndpoint")
            .objectOrNull()
            ?.get("browseId")
            .stringOrNull()
            ?.takeIf { it.startsWith(CHANNEL_ID_PREFIX) }

private fun JsonObject.channelName(): String? =
    this["title"].youtubeText()?.takeIf(String::isNotBlank)
        ?: this["formattedTitle"].youtubeText()?.takeIf(String::isNotBlank)
        ?: this["accessibility"]
            .objectOrNull()
            ?.get("accessibilityData")
            .objectOrNull()
            ?.get("label")
            .stringOrNull()
            ?.takeIf(String::isNotBlank)

/**
 * The token for the next page of channels, or null on the last one.
 *
 * A channels page carries one token at the end of its grid, and anything earlier belongs to a shelf
 * or the header rather than to the list being paged, so the last one wins.
 */
internal fun JsonElement.nextChannelContinuation(): String? = nextToken()
