package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object SmartDateParser {

    data class ParsedDateTime(
        val timestamp: Long,
        val formattedDate: String,
        val formattedTime: String,
        val hasExplicitTime: Boolean
    )

    // Regex for time: 11:59 PM, 11:59pm, 23:59, 23:59:00, 2:30 pm, @ 8:00 AM, 1299 / hrs
    private val timePattern = Regex(
        """(?i)(?:at|@|\b)?\s*(\d{1,2})[:.](\d{2})(?::\d{2})?\s*(am|pm|hrs|hours)?""",
        RegexOption.IGNORE_CASE
    )

    fun parseExpiryDateTime(text: String): ParsedDateTime? {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // 1. EXTRACT DATE (DD/MM/YYYY, DD-MMM-YYYY, etc.)
        val datePattern = Regex(
            """(?i)\b(\d{1,2})[-/\s.]([a-zA-Z]{3,}|\d{1,2})[-/\s.](\d{2,4})\b"""
        )
        val dateMatch = datePattern.find(text) ?: return null

        val dayStr = dateMatch.groupValues[1]
        val monthStr = dateMatch.groupValues[2]
        val yearStr = dateMatch.groupValues[3]

        // Parse Day, Month, Year into Calendar
        val day = dayStr.toIntOrNull() ?: 1
        val year = when (yearStr.length) {
            2 -> 2000 + yearStr.toInt()
            4 -> yearStr.toInt()
            else -> calendar.get(Calendar.YEAR)
        }
        val month = parseMonth(monthStr)

        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)

        // 2. EXTRACT EXACT TIME IF PRESENT IN TEXT
        // Search for time near the matched date or near keywords like "at", "valid till", "expires"
        var hasExplicitTime = false
        var targetHour = 23
        var targetMinute = 59 // Default for telecom plans if no time is specified (End of day)

        val timeMatch = timePattern.find(text)
        if (timeMatch != null) {
            val rawHour = timeMatch.groupValues[1].toIntOrNull()
            val rawMinute = timeMatch.groupValues[2].toIntOrNull()
            val meridiem = timeMatch.groupValues[3].lowercase()

            if (rawHour != null && rawMinute != null) {
                hasExplicitTime = true
                if (meridiem == "pm" && rawHour < 12) {
                    targetHour = rawHour + 12
                } else if (meridiem == "am" && rawHour == 12) {
                    targetHour = 0
                } else if (meridiem == "am" || meridiem == "pm" || meridiem == "hrs" || meridiem == "hours" || meridiem == "") {
                    // 24 hour or simple assignment
                    if (rawHour in 0..23) {
                        targetHour = rawHour
                    }
                }
                if (rawMinute in 0..59) {
                    targetMinute = rawMinute
                }
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Adjust if date has elapsed in current year
        if (calendar.timeInMillis < now) {
            calendar.add(Calendar.YEAR, 1)
        }

        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val tf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return ParsedDateTime(
            timestamp = calendar.timeInMillis,
            formattedDate = df.format(calendar.time),
            formattedTime = tf.format(calendar.time),
            hasExplicitTime = hasExplicitTime
        )
    }

    private fun parseMonth(monthStr: String): Int {
        val m = monthStr.lowercase(Locale.getDefault()).take(3)
        return when (m) {
            "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
            "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
            else -> {
                val num = monthStr.toIntOrNull()
                if (num != null && num in 1..12) num - 1 else 0
            }
        }
    }
}
