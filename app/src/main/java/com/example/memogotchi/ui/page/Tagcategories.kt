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

// ── Add to TagCategories.kt ──────────────────────────────────────────────

// Reverse lookup: tag string -> its category label ("Social", "Work", etc.)
private val tagToCategoryLabel: Map<String, String> =
    tagCategories.flatMap { cat -> cat.tags.map { tag -> tag to cat.label } }.toMap()

fun categoryLabelForTag(tag: String): String? = tagToCategoryLabel[tag]

/** Maps a list of raw tags (e.g. from a diary entry or goal) to the set of category labels they touch. */
fun categoryLabelsForTags(tags: List<String>): Set<String> =
    tags.mapNotNull { categoryLabelForTag(it) }.toSet()