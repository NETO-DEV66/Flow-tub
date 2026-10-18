package com.flowtube.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private val ReleaseJson = Json { ignoreUnknownKeys = true }
private const val SHA256_PREFIX = "sha256:"
private const val FOSS_MARKER = "foss"
private const val NIGHTLY_MARKER = "nightly"
private const val APK_SUFFIX = ".apk"
private val AbiTokens = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
    val digest: String? = null,
)

internal fun parseGitHubRelease(json: String): GitHubRelease = ReleaseJson.decodeFromString(GitHubRelease.serializer(), json)

/** [release] as an [AppRelease] when it is newer than [currentVersion], otherwise null. */
internal fun GitHubRelease.toAppReleaseIfNewer(currentVersion: String): AppRelease? {
    if (tagName.isBlank() || !AppVersions.isNewer(tagName, currentVersion)) return null
    return AppRelease(
        version = AppVersions.normalize(tagName),
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = selectApk(assets)?.toReleaseApk(),
    )
}

/**
 * The github-flavour APK for this device: the universal APK, else any github APK. Foss builds are
 * never picked; they are signed for other stores. Nightly assets are never picked either — Flow Tube
 * publishes one universal APK per release.
 *
 * Asset names are matched structurally rather than by a fixed prefix, so a release may name its
 * files `FlowTube-1.0.0-universal.apk` or `flow-universal.apk` and both resolve.
 */
internal fun selectApk(assets: List<GitHubAsset>): GitHubAsset? {
    val github =
        assets.filter {
            it.name.endsWith(APK_SUFFIX, ignoreCase = true) &&
                !it.name.contains(FOSS_MARKER, ignoreCase = true) &&
                !it.name.contains(NIGHTLY_MARKER, ignoreCase = true)
        }
    val universal = github.filter { asset -> AbiTokens.none { asset.name.contains(it, ignoreCase = true) } }
    return universal.firstOrNull() ?: github.firstOrNull()
}

private fun GitHubAsset.toReleaseApk(): ReleaseApk =
    ReleaseApk(
        name = name,
        url = downloadUrl,
        sizeBytes = size,
        sha256 = digest?.takeIf { it.startsWith(SHA256_PREFIX) }?.removePrefix(SHA256_PREFIX)?.lowercase(),
    )
