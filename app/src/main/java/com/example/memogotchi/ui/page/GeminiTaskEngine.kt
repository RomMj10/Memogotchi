package com.example.memogotchi.ui.page

import com.example.memogotchi.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

suspend fun generateTasksWithGemini(
    today: DayData?,
    batteryLevel: Int,
    categorizedApps: List<CategorizedApp>,
): List<AnalogTask> = withContext(Dispatchers.IO) {

    if (today == null) return@withContext emptyList()

    val totalHours = today.totalMs / 3_600_000.0
    val appSummary = categorizedApps.take(5).joinToString("\n") {
        "- ${it.info.appName} (${it.category.label}): ${String.format("%.1f", it.hours)}h"
    }

    val prompt = """
        A user has the following phone usage today:
        Total screen time: ${String.format("%.1f", totalHours)} hours
        Battery level: $batteryLevel%
        Top apps:
        $appSummary

        Suggest 1 to 3 offline "analog tasks" to help them disconnect.
        Each task should be specific to their actual app usage above.
        
        Respond ONLY with a JSON array, no markdown, no explanation:
        [
          {
            "title": "short task title",
            "description": "1-2 sentence encouragement",
            "category": "SOCIAL|GAMES|ENTERTAINMENT|BROWSER|PRODUCTIVITY|OTHER",
            "durationMinutes": 15,
            "triggerReason": "brief reason based on their usage"
          }
        ]
    """.trimIndent()

    return@withContext try {
        val model = GenerativeModel(
            modelName       = "gemini-1.5-flash",
            apiKey          = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.7f }
        )
        val response = model.generateContent(prompt)
        val json     = response.text?.trim() ?: return@withContext emptyList()
        parseGeminiTasks(json)
    } catch (e: Exception) {
        emptyList() // fallback to rule-based if Gemini fails
    }
}

private fun parseGeminiTasks(json: String): List<AnalogTask> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapIndexed { i, _ ->
            val obj = array.getJSONObject(i)
            AnalogTask(
                id              = "gemini_$i",
                title           = obj.getString("title"),
                description     = obj.getString("description"),
                category        = runCatching {
                    AppCategory.valueOf(obj.getString("category"))
                }.getOrDefault(AppCategory.OTHER),
                durationMinutes = obj.optInt("durationMinutes", 15),
                triggerReason   = obj.getString("triggerReason"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}