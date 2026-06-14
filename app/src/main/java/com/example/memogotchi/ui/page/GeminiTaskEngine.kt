package com.example.memogotchi.ui.page

import com.example.memogotchi.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun generateTasksWithGemini(
    weekData: List<DayData>,
    batteryLevel: Int,
    categorizedApps: List<CategorizedApp>,
    completedHistory: List<CompletedTaskRecord>,
): List<AnalogTask> = withContext(Dispatchers.IO) {

    val today = weekData.lastOrNull() ?: return@withContext emptyList()
    val totalHours = today.totalMs / 3_600_000.0

    val appSummary = categorizedApps.take(5).joinToString("\n") {
        "- ${it.info.appName} (${it.category.label}): ${String.format("%.1f", it.hours)}h"
    }

    val weekSummary = weekData.joinToString("\n") {
        "- ${it.fullLabel}: ${String.format("%.1f", it.totalMs / 3_600_000.0)}h"
    }

    val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm", Locale.getDefault()).format(Date())

    val favoriteSummary = completedHistory
        .groupingBy { it.title }.eachCount()
        .entries.sortedByDescending { it.value }.take(5)
        .joinToString(", ") { "${it.key} (x${it.value})" }
        .ifBlank { "none yet" }

    val recentSummary = completedHistory.takeLast(10).joinToString(", ") { it.title }.ifBlank { "none yet" }

    val prompt = """
        You are a digital wellbeing assistant for a virtual pet app called Memogotchi.
        Current date & time: $dateStr
        Battery level: $batteryLevel%

        Today's screen time: ${String.format("%.1f", totalHours)} hours
        Top apps used today:
        $appSummary

        Past 7 days screen time trend:
        $weekSummary

        User's most completed offline tasks: $favoriteSummary
        Recently completed tasks (avoid repeating these exactly): $recentSummary

        Suggest 1 to 3 offline "analog tasks" personalized to usage patterns, time of day,
        battery level, and past preferences. Each task must be specific and actionable.

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
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.7f }
        )
        val response = model.generateContent(prompt)
        val json = response.text?.trim() ?: return@withContext emptyList()
        parseGeminiTasks(json)
    } catch (e: Exception) {
        emptyList()
    }
}

private fun parseGeminiTasks(json: String): List<AnalogTask> {
    return try {
        val cleaned = json.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = JSONArray(cleaned)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            AnalogTask(
                id = "gemini_${System.currentTimeMillis()}_$i",
                title = obj.getString("title"),
                description = obj.getString("description"),
                category = runCatching { AppCategory.valueOf(obj.getString("category")) }.getOrDefault(AppCategory.OTHER),
                durationMinutes = obj.optInt("durationMinutes", 15),
                triggerReason = obj.getString("triggerReason"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}