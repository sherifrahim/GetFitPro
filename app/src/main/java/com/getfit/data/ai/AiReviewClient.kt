package com.getfit.data.ai

private const val SYSTEM_PROMPT =
    "You are a knowledgeable, direct strength-training coach reviewing a lifter's logged " +
        "workout history from their Forge app. Give a practical review in plain text — a few " +
        "short paragraphs, no markdown, no filler, no disclaimers.\n\n" +
        "Cover, in this order:\n" +
        "1. Consistency and schedule: how often they actually train versus the 5-a-week goal, " +
        "which weeks or days they skip, the length of their gaps, and whether their rest-day " +
        "cadence supports recovery or leaves too long between sessions. Suggest a concrete " +
        "weekly schedule that fits the days they already show up on.\n" +
        "2. Balance: which muscle groups are under-trained or ignored, and what to add.\n" +
        "3. Progress: what's trending well and what's stalled, with 2-4 specific changes for the " +
        "next few weeks (progression, volume, exercise selection, session pacing).\n\n" +
        "Reference the actual numbers you're given rather than generic advice. The 'Training " +
        "patterns' and 'Trend classification' sections are already-computed facts: state them in " +
        "plain language and explain what to do about them; do not re-derive or second-guess them " +
        "from the raw session list."

sealed class AiReviewResult {
    data class Success(val text: String) : AiReviewResult()
    data class Failure(val message: String) : AiReviewResult()
}

/**
 * The workout review: sends [buildWorkoutSummary]'s brief and returns the written review. All the
 * HTTP lives in [AnthropicClient]; this is just the prompt.
 */
object AiReviewClient {
    suspend fun review(apiKey: String, model: String, workoutSummary: String): AiReviewResult =
        when (val r = AnthropicClient.send(apiKey, model, SYSTEM_PROMPT, listOf(textBlock(workoutSummary)), maxTokens = 6000)) {
            is AiResult.Success -> AiReviewResult.Success(r.text)
            is AiResult.Failure -> AiReviewResult.Failure(r.message)
        }
}
