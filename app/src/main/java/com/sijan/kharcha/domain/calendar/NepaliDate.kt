package com.sijan.kharcha.domain.calendar

data class NepaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int    // 1 to 32
) {
    val monthName: String
        get() = MONTH_NAMES_EN.getOrElse(month - 1) { "" }

    val monthNameNp: String
        get() = MONTH_NAMES_NP.getOrElse(month - 1) { "" }

    fun format(includeYear: Boolean = true): String {
        return if (includeYear) {
            "$monthName $day, $year BS"
        } else {
            "$monthName $day"
        }
    }

    fun formatNp(includeYear: Boolean = true): String {
        return if (includeYear) {
            "$monthNameNp $day, $year"
        } else {
            "$monthNameNp $day"
        }
    }

    companion object {
        val MONTH_NAMES_EN = listOf(
            "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
            "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
        )
        val MONTH_NAMES_NP = listOf(
            "बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
            "कार्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत"
        )
    }
}
