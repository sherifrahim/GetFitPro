package com.getfit.data.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Pins the Messages API request shape. Every assertion here is something that fails *quietly* at
 * runtime if it drifts — a malformed image block or a stray parameter comes back as an HTTP 400
 * message in a UI card, not as a compile error.
 */
class AnthropicRequestTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun text_only_request_has_the_expected_top_level_shape() {
        val body = json.parseToJsonElement(
            AnthropicClient.buildRequestBody("claude-opus-5", "sys", listOf(textBlock("hi")), 6000),
        ).jsonObject

        assertThat(body["model"]!!.jsonPrimitive.content).isEqualTo("claude-opus-5")
        assertThat(body["max_tokens"]!!.jsonPrimitive.content).isEqualTo("6000")
        assertThat(body["system"]!!.jsonPrimitive.content).isEqualTo("sys")
        val msg = body["messages"]!!.jsonArray.single().jsonObject
        assertThat(msg["role"]!!.jsonPrimitive.content).isEqualTo("user")
        assertThat(msg["content"]!!.jsonArray.single().jsonObject["type"]!!.jsonPrimitive.content).isEqualTo("text")
    }

    /**
     * `thinking` is deliberately absent: on Claude Opus 5 omitting it means adaptive thinking, and on
     * older models an explicit config would 400. Sending nothing is the one setting that's right for
     * every model the user might type into Settings.
     */
    @Test fun thinking_is_never_sent_explicitly() {
        val body = json.parseToJsonElement(
            AnthropicClient.buildRequestBody("claude-opus-5", "s", listOf(textBlock("x")), 100),
        ).jsonObject
        assertThat(body.containsKey("thinking")).isFalse()
        assertThat(body.containsKey("temperature")).isFalse() // rejected on current models
    }

    @Test fun fallbacks_default_only_on_models_that_accept_it() {
        fun has(model: String) = json.parseToJsonElement(
            AnthropicClient.buildRequestBody(model, "s", listOf(textBlock("x")), 100),
        ).jsonObject.containsKey("fallbacks")

        assertThat(has("claude-opus-5")).isTrue()
        assertThat(has("claude-fable-5-1")).isTrue()
        assertThat(has("claude-sonnet-5")).isFalse()
        assertThat(has("claude-opus-4-8")).isFalse()
        assertThat(AnthropicClient.usesFallbacks("claude-haiku-4-5")).isFalse()
    }

    @Test fun image_block_is_a_base64_source() {
        val block = imageBlock("QUJD", "image/jpeg")
        assertThat(block["type"]!!.jsonPrimitive.content).isEqualTo("image")
        val src = block["source"]!!.jsonObject
        assertThat(src["type"]!!.jsonPrimitive.content).isEqualTo("base64")
        assertThat(src["media_type"]!!.jsonPrimitive.content).isEqualTo("image/jpeg")
        assertThat(src["data"]!!.jsonPrimitive.content).isEqualTo("QUJD")
    }

    // --- body-check content assembly ---

    private val img = EncodedImage(base64 = "QUJD", width = 10, height = 10, bytes = 3)

    /** Each image is preceded by a label so the model knows which view it's looking at. */
    @Test fun body_content_labels_each_photo_and_puts_images_before_the_request_text() {
        val blocks = buildBodyAnalysisContent(
            images = listOf(PhotoView.FRONT to img, PhotoView.BACK to img),
            goal = BodyGoal.LOSE_FAT, focusArea = "posture", workoutSummary = "SUMMARY",
        )
        val types = blocks.map { it["type"]!!.jsonPrimitive.content }
        assertThat(types).containsExactly("text", "image", "text", "image", "text").inOrder()
        assertThat(blocks[0]["text"]!!.jsonPrimitive.content).isEqualTo("Photo 1 of 2: Front view.")
        assertThat(blocks[2]["text"]!!.jsonPrimitive.content).isEqualTo("Photo 2 of 2: Back view.")

        val request = blocks.last()["text"]!!.jsonPrimitive.content
        assertThat(request).contains("Goal: lose body fat")
        assertThat(request).contains("Specific area to look at: posture.")
        assertThat(request).contains("SUMMARY")
    }

    @Test fun blank_focus_area_is_omitted() {
        val blocks = buildBodyAnalysisContent(listOf(PhotoView.FRONT to img), BodyGoal.GENERAL, "   ", "S")
        assertThat(blocks.last()["text"]!!.jsonPrimitive.content).doesNotContain("Specific area")
    }

    /** The system prompt must keep its guardrails; these are the phrases the assessment depends on. */
    @Test fun system_prompt_keeps_its_guardrails() {
        val p = bodyAnalysisSystemPrompt()
        assertThat(p).contains("never judge")
        assertThat(p).contains("No medical diagnoses")
        assertThat(p).contains("LIMITS")
        assertThat(p).contains("always as a range")
    }
}
