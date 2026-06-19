package com.example.memogotchi.ui.page

// ════════════════════════════════════════════════════════════════════════════
//  SHARED TAG CATEGORIES — used by both diary entries and goals
// ════════════════════════════════════════════════════════════════════════════

data class TagCategory(
    val label: String,
    val tags: List<String>,
)

val tagCategories: List<TagCategory> = listOf(
    TagCategory("Social",    listOf("Family", "Friends", "Love")),
    TagCategory("Work",      listOf("Work", "Learning")),
    TagCategory("Wellness",  listOf("Health", "Self-care", "Rest", "Exercise")),
    TagCategory("Lifestyle", listOf("Hobbies", "Food", "Travel", "Creativity", "Gratitude", "Stress")),
)

// Flat list, derived from the grouped structure — kept for any code that still wants all tags at once
val allTagOptions: List<String> = tagCategories.flatMap { it.tags }