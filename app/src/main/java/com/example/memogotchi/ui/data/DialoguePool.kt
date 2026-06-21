package com.example.memogotchi.ui.data

enum class DialogueCategory {
    HAPPY,
    CONCERNED,
    TIRED,
    ALARMED,
    NEW_TASK,
    TASK_DONE
}

object DialoguePool {
    private val pool:
            MutableMap<DialogueCategory, MutableList<String>> = mutableMapOf(
                DialogueCategory.HAPPY to mutableListOf(
                    "Looking good today!",
                    "{hours}? You're doing great, keep it up!",
                    "Nice and balanced today!",
                    "{name} is looking great!",
                    "{name} is having a great day!",
                ),
                DialogueCategory.CONCERNED to mutableListOf(
                    "{hours} today. Take a break soon!",
                    "That's a fair bit if screen time you got there",
                    "Feeling a little screen-fatigue today?",
                    "{name} is seeing a lot of screen time today."
                ),
                DialogueCategory.TIRED to mutableListOf(
                    "It's late and you've been on your phone for {hours}. Maybe rest?",
                    "{name} is getting sleepy... how about you?",
                    "{name} is getting a little tired.",
                ),
                DialogueCategory.ALARMED to mutableListOf(
                    "{hours}? That's a lot..",
                    "Whoa, that's a lot of screen time today!",
                    "{name} is getting sleepy... how about you?",
                    "{name} is concerned about the screen time."
                ),
                DialogueCategory.NEW_TASK to mutableListOf(
                    "New idea!, how about: \"{task}\"",
                    "{name} found something interesting, try: \"{task}\"",
                    "Got a suggestion you might like: \"{task}\"",
                ),
                DialogueCategory.TASK_DONE to mutableListOf(
                    "You did it! \"{task}\" is done!",
                    "{name} is proud of you for finishing \"{task}\"!",
                    "👍",
                    "\"{task}\" finished, nice work!"
                )
    )
    fun addDialogue(category: DialogueCategory, vararg dialogues: String) {
     pool.getOrPut(category) {
         mutableListOf()
     }   .addAll(dialogues)
    }
    fun randomLine(category: DialogueCategory): String? = pool[category]?.randomOrNull()
    fun firstLine(category: DialogueCategory): String? = pool[category]?.firstOrNull()
}

fun String.fillTemplate(vararg pairs: Pair<String, String>): String {
    var result = this
    pairs.forEach {
        (k, v) -> result = result.replace("{$k}", v)
    }
    return result
}