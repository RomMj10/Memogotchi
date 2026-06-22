package com.example.memogotchi.ui.page

import android.util.Log
import com.example.memogotchi.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "GEMINIPERSONALITY"

suspend fun generatePersonalityProfile(
    tagCategoryTally: Map<String, Int>,
    screenCategoryTally: Map<AppCategory, Double>,
): PersonalityProfile? = withContext(Dispatchers.IO) {

    if (BuildConfig.GEMINI_API_KEY.isBlank()) {
        Log.w(TAG, "Skipped: GEMINI_API_KEY is blank.")
        return@withContext null
    }

    val tagSummary = tagCategoryTally.entries
        .sortedByDescending { it.value }
        .joinToString("\n") { "- ${it.key}: ${it.value} logged entries" }
        .ifBlank { "- none logged yet" }

    val screenSummary = screenCategoryTally.entries
        .sortedByDescending { it.value }
        .joinToString("\n") { "- ${it.key.label}: ${String.format("%.1f", it.value)}h cumulative" }
        .ifBlank { "- none recorded yet" }

    val prompt = """
        You are generating a personality reflection for "Memo", a virtual companion in a
        wellbeing app called Memogotchi. Memo is NOT a game character earning levels — it is
        a quiet mirror of the user's real-world behavior over time.

        Behavioral data collected so far:
        Diary/goal tag categories:
        $tagSummary

        Screen time categories (cumulative hours):
        $screenSummary

        Write Memo's personality reflection following these rules:
        - Warm and observational, like a thoughtful friend describing what they've noticed
        - Second person ("You've been..." or "Memo reflects someone who...")
        - Honest about balance — mention both active and quieter categories, no judgment
        - Non-prescriptive — no advice, no suggestions on what to change
        - Avoid static identity labels (no "Memo is a Dreamer"); use evolving, present-tense phrasing
        - Tagline: exactly 1 sentence
        - Traits: exactly 3 short trait words/phrases (2-3 words each)
        - Paragraphs: 2 to 3 short paragraphs, each 1-3 sentences

        Respond ONLY with JSON, no markdown, no explanation:
        {
          "tagline": "...",
          "traits": ["...", "...", "..."],
          "paragraphs": ["...", "..."]
        }
    """.trimIndent()

    return@withContext withTimeoutOrNull(20_000L.milliseconds) {
        try {
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig { temperature = 0.8f }
            )
            val response = model.generateContent(prompt)
            val json = response.text?.trim() ?: return@withTimeoutOrNull null
            parsePersonalityProfile(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating personality profile", e)
            null
        }
    }
}

private fun parsePersonalityProfile(json: String): PersonalityProfile? {
    return try {
        val cleaned = json.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(cleaned)
        val traitsArr = obj.getJSONArray("traits")
        val paraArr = obj.getJSONArray("paragraphs")
        PersonalityProfile(
            tagline = obj.getString("tagline"),
            traits = (0 until traitsArr.length()).map { traitsArr.getString(it) },
            paragraphs = (0 until paraArr.length()).map { paraArr.getString(it) },
        )
    } catch (e: Exception) {
        null
    }
}