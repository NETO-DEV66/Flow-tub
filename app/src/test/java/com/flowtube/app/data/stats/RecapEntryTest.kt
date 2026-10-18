package com.flowtube.app.data.stats

import com.google.common.truth.Truth.assertThat
import com.flowtube.app.data.local.PrivacyGate
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecapEntryTest {
    private fun gate(
        deepFocus: Boolean,
        historyPaused: Boolean,
    ) = PrivacyGate(
        deepFocusActive = { deepFocus },
        watchHistoryPaused = { historyPaused },
        scrobbleDuringDeepFocus = { false },
    )

    @Test
    fun `everything is recorded when nothing is paused`() =
        runTest {
            RecapEntry.entries.forEach { assertThat(it.allowed(gate(deepFocus = false, historyPaused = false))).isTrue() }
        }

    @Test
    fun `Deep Focus keeps searches, actions and views out`() =
        runTest {
            val gate = gate(deepFocus = true, historyPaused = false)

            assertThat(RecapEntry.ACTIVITY.allowed(gate)).isFalse()
            assertThat(RecapEntry.VIEW.allowed(gate)).isFalse()
        }

    @Test
    fun `paused watch history keeps views out but still counts searches`() =
        runTest {
            val gate = gate(deepFocus = false, historyPaused = true)

            assertThat(RecapEntry.VIEW.allowed(gate)).isFalse()
            assertThat(RecapEntry.ACTIVITY.allowed(gate)).isTrue()
        }

    @Test
    fun `forgetting stored data always goes through`() =
        runTest {
            assertThat(RecapEntry.FORGET.allowed(gate(deepFocus = true, historyPaused = true))).isTrue()
        }
}
