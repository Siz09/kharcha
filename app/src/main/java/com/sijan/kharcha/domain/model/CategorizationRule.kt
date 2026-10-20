package com.sijan.kharcha.domain.model

data class CategorizationRule(
    val id: Long = 0L,
    val name: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val categoryId: Long,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "Rule name cannot be blank" }
        require(pattern.isNotBlank()) { "Rule pattern cannot be blank" }
    }

    fun matches(text: String): Boolean {
        if (!isActive || pattern.isBlank()) return false
        return if (isRegex) {
            try {
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
            } catch (_: Exception) {
                false
            }
        } else {
            text.contains(pattern, ignoreCase = true)
        }
    }
}

enum class InsightType {
    CATEGORY_SPIKE,
    POTENTIAL_DUPLICATE,
    HIGH_DAILY_BURN
}

data class SpendingInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val message: String,
    val categoryId: Long? = null,
    val amountPaisa: Long? = null,
    val relatedEntryIds: List<Long> = emptyList()
)
