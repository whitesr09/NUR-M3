package com.nshd.nurm3.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NurAiModelPolicyTest {
    @Test fun defaultAndPresetsAreAccepted() {
        assertTrue(NurAiRepository.validModel(NurAiRepository.DEFAULT_MODEL))
        NurAiRepository.MODEL_PRESETS.forEach { assertTrue(NurAiRepository.validModel(it)) }
    }

    @Test fun pathQueryWhitespaceAndOversizedIdsAreRejected() {
        listOf(
            "",
            "ab",
            "gemini/2.5-flash",
            "gemini-2.5-flash?key=bad",
            "gemini 2.5 flash",
            "gemini-2.5-flash#fragment",
            "x".repeat(81)
        ).forEach { assertFalse(it, NurAiRepository.validModel(it)) }
    }
}
