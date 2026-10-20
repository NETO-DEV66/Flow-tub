package com.neurotube.app.data.local

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrivacyGateTest {
    private fun gate(
        deepFocus: Boolean,
        historyPaused: Boolean = false,
        scrobbleDuringDeepFocus: Boolean = false,
    ) = PrivacyGate(
        deepFocusActive = { deepFocus },
        watchHistoryPaused = { historyPaused },
        scrobbleDuringDeepFocus = { scrobbleDuringDeepFocus },
    )

    @Test
    fun `scrobbling runs while Deep Focus is off`() =
        runTest {
            assertThat(gate(deepFocus = false).allowsScrobbling()).isTrue()
        }

    @Test
    fun `Deep Focus stops scrobbling by default`() =
        runTest {
            assertThat(gate(deepFocus = true).allowsScrobbling()).isFalse()
        }

    @Test
    fun `Deep Focus scrobbles when the user chose to`() =
        runTest {
            assertThat(gate(deepFocus = true, scrobbleDuringDeepFocus = true).allowsScrobbling()).isTrue()
        }

    @Test
    fun `the gate reports both settings as they are`() =
        runTest {
            val gate = gate(deepFocus = true, historyPaused = true)

            assertThat(gate.isDeepFocusActive()).isTrue()
            assertThat(gate.isWatchHistoryPaused()).isTrue()
        }
}
