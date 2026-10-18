/*
 * Copyright (C) 2025-2026 Flow Tube | A-EDev
 *
 * This file is part of Flow Tube (https://github.com/mrvoidxin/Flow-tub).
 *
 * This recommendation algorithm (FlowNeuroEngine) is the intellectual property
 * of the Flow Tube project. Any use of this code in other projects must
 * explicitly credit "Flow Tube Android Client" and link back to the original repository.
 */

package com.flowtube.app.data.recommendation

import kotlinx.serialization.json.JsonElement

/** Everything a rank pass reads besides its candidates, as a Home snapshot stores it. */
internal data class NeuroDiagnosticState(
    val brain: JsonElement,
    val categories: NeuroCategoryStore.CategoryIndex,
    val coWatch: NeuroCoWatchStore.CoWatchIndex,
)
