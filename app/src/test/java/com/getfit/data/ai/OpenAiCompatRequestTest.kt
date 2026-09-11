package com.getfit.data.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * The compatible client takes the same content blocks the Anthropic client takes and translates
 * them. A wrong translation is an HTTP 400 from a provider at runtime, so the shape is pinned here.
 */
class OpenAiCompatRequestTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun system_prompt_becomes_a_system_message_and_user_blocks_follow() {
        val body = json.parseToJsonElement(
            OpenAiCompatClient.buildRequestBody("some-model", "SYS", listOf(textBlock("hello")), 500),
        ).jsonObject

        assertThat(body["model"]!!.jsonPrimitive.content).isEqualTo("some-model")
        val msgs = body["messages"]!!.jsonArray
        assertThat(msgs).hasSize(2)
        assertThat(msgs[0].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("system")
        assertThat(msgs[0].jsonObject["content"]!!.jsonPrimitive.content).isEqualTo("SYS")
        assertThat(msgs[1].jsonObject["role"]!!.jsonPrimitive.content).isEqualTo("user")
        val part = msgs[1].jsonObject["content"]!!.jsonArray.single().jsonObject
        assertThat(part["type"]!!.jsonPrimitive.content).isEqualTo("text")
        assertThat(part["text"]!!.jsonPrimitive.content).isEqualTo("hello")
    }

    /** Anthropic base64 image blocks become OpenAI image_url parts carrying a data: URL. */
    @Test fun image_blocks_are_translated_to_data_url_image_parts() {
        val body = json.parseToJsonElement(
            OpenAiCompatClient.buildRequestBody("m", "s", listOf(imageBlock("QUJD", "image/jpeg"), textBlock("t")), 500),
        ).jsonObject
        val parts = body["messages"]!!.jsonArray[1].jsonObject["content"]!!.jsonArray

        val img = parts[0].jsonObject
        assertThat(img["type"]!!.jsonPrimitive.content).isEqualTo("image_url")
        assertThat(img["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content).isEqualTo("data:image/jpeg;base64,QUJD")
        assertThat(parts[1].jsonObject["type"]!!.jsonPrimitive.content).isEqualTo("text")
    }

    /** Both names are sent: new OpenAI models reject max_tokens, older servers only know it. */
    @Test fun token_cap_is_sent_under_both_names() {
        val body = json.parseToJsonElement(OpenAiCompatClient.buildRequestBody("m", "s", listOf(textBlock("t")), 777)).jsonObject
        assertThat(body["max_completion_tokens"]!!.jsonPrimitive.content).isEqualTo("777")
        assertThat(body["max_tokens"]!!.jsonPrimitive.content).isEqualTo("777")
    }

    @Test fun no_anthropic_only_fields_leak_through() {
        val body = json.parseToJsonElement(OpenAiCompatClient.buildRequestBody("m", "s", listOf(textBlock("t")), 1)).jsonObject
        assertThat(body.containsKey("system")).isFalse()      // it's a message, not a top-level field
        assertThat(body.containsKey("fallbacks")).isFalse()
        assertThat(body.containsKey("thinking")).isFalse()
    }

    // --- response parsing ---

    @Test fun extracts_string_content() {
        val r = json.parseToJsonElement("""{"choices":[{"message":{"role":"assistant","content":"  Hi there  "}}]}""").jsonObject
        assertThat(OpenAiCompatClient.extractText(r)).isEqualTo("Hi there")
    }

    /** Some providers return content as an array of parts rather than a string. */
    @Test fun extracts_array_content() {
        val r = json.parseToJsonElement(
            """{"choices":[{"message":{"content":[{"type":"text","text":"A"},{"type":"text","text":"B"}]}}]}""",
        ).jsonObject
        assertThat(OpenAiCompatClient.extractText(r)).isEqualTo("A\nB")
    }

    @Test fun missing_choices_is_null_not_a_crash() {
        val r = json.parseToJsonElement("""{"id":"x"}""").jsonObject
        assertThat(OpenAiCompatClient.extractText(r)).isNull()
    }

    // --- presets ---

    /** Every preset with a URL must be https (Android blocks cleartext) and end in a version path. */
    @Test fun presets_are_https_and_well_formed() {
        CompatPreset.values().filter { it.baseUrl.isNotBlank() }.forEach { p ->
            assertThat(p.baseUrl).startsWith("https://")
            assertThat(p.baseUrl).doesNotContain(" ")
            assertThat(p.baseUrl.endsWith("/")).isFalse()
        }
    }
}

class OpenAiCompatReasoningTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Groq's Qwen models think inline and the free tier's token budget is tight; only Groq gets the flag. */
    @Test fun reasoning_is_disabled_on_groq_only() {
        fun has(base: String) = json.parseToJsonElement(
            OpenAiCompatClient.buildRequestBody("m", "s", listOf(textBlock("t")), 100, baseUrl = base),
        ).jsonObject.containsKey("reasoning_effort")
        assertThat(has("https://api.groq.com/openai/v1")).isTrue()
        assertThat(has("https://api.openai.com/v1")).isFalse()
        assertThat(has("https://api.deepseek.com")).isFalse()
        assertThat(has("")).isFalse()
    }

    @Test fun think_blocks_are_stripped_from_answers() {
        assertThat(OpenAiCompatClient.stripThinking("<think>\nplan: say ok\n</think>\n\nOK").trim()).isEqualTo("OK")
        assertThat(OpenAiCompatClient.stripThinking("A <think>x</think> B <think>y</think> C")).isEqualTo("A  B  C")
        assertThat(OpenAiCompatClient.stripThinking("no tags here")).isEqualTo("no tags here")
    }

    /** A max_tokens cut-off mid-thought leaves an unterminated block; that must vanish, not leak. */
    @Test fun unterminated_think_block_is_removed() {
        assertThat(OpenAiCompatClient.stripThinking("<think>still thinking about").trim()).isEmpty()
        assertThat(OpenAiCompatClient.stripThinking("Answer.\n<think>trailing").trim()).isEqualTo("Answer.")
    }

    @Test fun extract_text_applies_stripping() {
        val r = json.parseToJsonElement("""{"choices":[{"message":{"content":"<think>hmm</think>\nReal answer"}}]}""").jsonObject
        assertThat(OpenAiCompatClient.extractText(r)).isEqualTo("Real answer")
    }
}
