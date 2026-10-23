package com.neurotube.app.utils

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val TAG = "AccountImportDiag"

/** How many diagnostic lines one import keeps, so a report can never grow without bound. */
internal const val IMPORT_DIAGNOSTIC_LIMIT = 400

/**
 * What one part of an import did: how many items its page carried, and what went wrong when it did
 * not finish.
 *
 * A part that fails carries the error's class and message rather than a bare flag, so the result
 * screen can name the part that broke and the diagnostics report can say why. What was actually
 * written is counted by [com.neurotube.app.account.AccountImportReport], not here.
 */
data class ImportStepDiagnostic(
    val step: String,
    val found: Int,
    val error: String? = null,
)

/**
 * A line-by-line account of the last import, kept for the diagnostics report.
 *
 * An import is the one feature whose failures were previously invisible: every part caught its own
 * errors and the screen said only "Import failed". This records the shape of each response and the
 * error of each part, which is what makes a bug report actionable — a failed import can be read
 * back line by line instead of reproduced.
 */
object ImportDiagnostics {
    private const val LIMIT = IMPORT_DIAGNOSTIC_LIMIT
    private val lines = ArrayDeque<String>()

    /**
     * Whether a request-level diagnostic is being kept. Set for the duration of an import so the
     * shape of each response is recorded, and left off the rest of the time so ordinary browsing
     * does not fill the log.
     */
    @Volatile
    var recording: Boolean = false
        private set

    /** Starts a new import, dropping the previous one's lines and recording response shapes. */
    fun begin() {
        lines.clear()
        recording = true
        record("import: start")
    }

    /** Ends an import, so responses read outside one are not recorded. */
    fun end() {
        recording = false
    }

    /** Appends one line, dropping the oldest once the limit is reached. */
    fun record(message: String) {
        lines.addLast(message)
        while (lines.size > LIMIT) lines.removeFirst()
        Log.i(TAG, message)
    }

    /** The log as one block of text, or an empty string when nothing has been recorded. */
    fun dump(): String = lines.joinToString("\n")
}

/** A short, log-safe name for a renderer key, so a response's shape is recorded without its body. */
internal fun rendererName(key: String): String = key.removeSuffix("Renderer").removeSuffix("ViewModel")

/**
 * The parts of a YouTube response a reader keys on, so the diagnostic can say which of them a
 * response actually carried. Keys the app does not read are left out, keeping the line short.
 */
internal val RESPONSE_MARKER_KEYS =
    listOf(
        "lockupViewModel",
        "musicTwoRowItemRenderer",
        "musicResponsiveListItemRenderer",
        "gridRenderer",
        "musicShelfRenderer",
        "itemSectionRenderer",
        "sectionListRenderer",
        "singleColumnBrowseResultsRenderer",
        "continuationItemRenderer",
        "continuationItemViewModel",
        "gridContinuation",
        "musicShelfContinuation",
        "sectionListContinuation",
        "lockupMetadataViewModel",
        "signInEndpoint",
        "contentId",
        "contentType",
    )

/** How often each of [RESPONSE_MARKER_KEYS] appears in a response. */
internal fun JsonElement.markerCounts(): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()

    fun walk(node: JsonElement) {
        when (node) {
            is JsonObject -> {
                for ((key, value) in node) {
                    if (key in RESPONSE_MARKER_KEYS) counts.merge(key, 1, Int::plus)
                    walk(value)
                }
            }

            is JsonArray -> {
                node.forEach(::walk)
            }

            else -> {
                Unit
            }
        }
    }
    walk(this)
    return counts
}

/** The renderer keys a response is built from, as one line, most common first. */
internal fun JsonElement.describeShape(): String {
    val counts = mutableMapOf<String, Int>()

    fun walk(node: JsonElement) {
        when (node) {
            is JsonObject -> {
                for ((key, value) in node) {
                    if (key.endsWith("Renderer") || key.endsWith("ViewModel")) {
                        counts.merge(rendererName(key), 1, Int::plus)
                    }
                    walk(value)
                }
            }

            is JsonArray -> {
                node.forEach(::walk)
            }

            else -> {
                Unit
            }
        }
    }
    walk(this)
    if (counts.isEmpty()) return "none"
    return counts.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key}=${it.value}" }
}

/** The marker keys a response carries, as one line, in a stable order. */
internal fun JsonElement.describeMarkers(): String {
    val counts = markerCounts()
    if (counts.isEmpty()) return "none"
    return RESPONSE_MARKER_KEYS
        .filter { counts.containsKey(it) }
        .joinToString(", ") { "$it=${counts.getValue(it)}" }
}

/** The `contentType` values a music response carries, so an unknown kind of row is visible. */
internal fun JsonElement.describeContentTypes(): String {
    val counts = mutableMapOf<String, Int>()

    fun walk(node: JsonElement) {
        when (node) {
            is JsonObject -> {
                (node["contentType"] as? JsonPrimitive)?.contentOrNull?.let { counts.merge(it, 1, Int::plus) }
                node.values.forEach(::walk)
            }

            is JsonArray -> {
                node.forEach(::walk)
            }

            else -> {
                Unit
            }
        }
    }
    walk(this)
    return if (counts.isEmpty()) "none" else counts.entries.joinToString(", ") { "${it.key}=${it.value}" }
}

/** Whether a response is a sign-in document rather than the page that was asked for. */
internal fun JsonElement.looksSignedOut(): Boolean {
    val counts = markerCounts()
    return counts.containsKey("signInEndpoint") && !counts.containsKey("lockupViewModel")
}

/** A one-line description of why a request or parse failed, with no response body in it. */
internal fun describeFailure(error: Throwable): String = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(200)}".trim()

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Parses a response body for the diagnostic, or null when it is not JSON. */
internal fun parseForDiagnostic(body: String): JsonElement? = runCatching { lenientJson.parseToJsonElement(body) }.getOrNull()

/**
 * Records the shape of one response: its status and size, the renderers it is built from, the keys
 * the readers look for, and the music row types it carries.
 *
 * The body itself is never recorded — only counts — so a log stays small and carries no account
 * data beyond what the app already reads.
 */
internal fun logResponse(
    label: String,
    status: Int,
    body: String,
) {
    val parsed = parseForDiagnostic(body)
    ImportDiagnostics.record("$label: status=$status bytes=${body.length}")
    if (parsed == null) {
        ImportDiagnostics.record("$label: body is not JSON")
        return
    }
    ImportDiagnostics.record("$label: shape=[${parsed.describeShape()}]")
    ImportDiagnostics.record("$label: markers=[${parsed.describeMarkers()}]")
    ImportDiagnostics.record("$label: types=[${parsed.describeContentTypes()}]")
    if (parsed.looksSignedOut()) {
        ImportDiagnostics.record("$label: SIGNED OUT — the response is a sign-in document")
    }
}

/** Records that a request or its parse failed, naming the part and the error. */
internal fun logFailure(
    label: String,
    error: Throwable,
) {
    ImportDiagnostics.record("$label: FAILED ${describeFailure(error)}")
}
