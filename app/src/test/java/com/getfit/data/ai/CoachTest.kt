package com.getfit.data.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoachTest {
    private val ids = setOf("bench", "squat", "pullup")

    @Test fun splits_prose_from_targets_block() {
        val text = "Do Pull Day next. Bench: go for 102.5 x 5.\n\nTARGETS:\n- bench | 105 | 8\n- pullup | 12 reps | 6\n- deadlift | 200 | 8\n"
        val (prose, targets) = parseCoachAnswer(text, ids)
        assertThat(prose).isEqualTo("Do Pull Day next. Bench: go for 102.5 x 5.")
        assertThat(targets).containsExactly(SuggestedTarget("bench", 105.0, 8), SuggestedTarget("pullup", 12.0, 6)).inOrder()
    }

    @Test fun no_block_means_no_targets_and_untouched_prose() {
        val (prose, targets) = parseCoachAnswer("  Just words.  ", ids)
        assertThat(prose).isEqualTo("Just words.")
        assertThat(targets).isEmpty()
    }

    @Test fun tolerates_units_decimals_and_clamps_weeks() {
        val (_, t) = parseCoachAnswer("x\nTARGETS:\n- squat | 142.5 kg | 30\n- squat | 150 | 4\n- bench | 0 | 8\n", ids)
        assertThat(t).containsExactly(SuggestedTarget("squat", 142.5, 16))   // first squat wins, weeks clamped
    }

    @Test fun prompts_mention_the_block_only_where_it_applies() {
        assertThat(coachSystemPrompt(CoachMode.LAST_WORKOUT, "kg")).doesNotContain("TARGETS:")
        assertThat(coachSystemPrompt(CoachMode.NEXT, "lb")).contains("TARGETS:")
        assertThat(coachSystemPrompt(CoachMode.NEXT, "lb")).contains("weight in lb")
        val brief = buildCoachBrief(CoachMode.NEXT, "SUMMARY", null, "RECOVERY", "- bench: Bench")
        assertThat(brief.indexOf("RECOVERY")).isLessThan(brief.indexOf("SUMMARY"))
        assertThat(brief).contains("Targetable exercises")
        assertThat(buildCoachBrief(CoachMode.LAST_WORKOUT, "S", "SESSION", null, "")).doesNotContain("Targetable")
    }
}
