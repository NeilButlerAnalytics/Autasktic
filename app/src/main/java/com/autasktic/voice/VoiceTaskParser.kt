package com.autasktic.voice

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.util.Locale
import java.util.regex.Pattern

data class ParsedVoiceTask(
    val description: String,
    val date: LocalDate,
    val time: LocalTime?,
)

object VoiceTaskParser {
    private val datePattern = Pattern.compile(
        "\\b(?:on\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?(january|february|march|april|may|june|july|august|september|october|november|december)\\b",
        Pattern.CASE_INSENSITIVE,
    )
    private val numericTimePattern = Pattern.compile(
        "\\b(?:at\\s+)?(\\d{1,2}):(\\d{2})\\b",
        Pattern.CASE_INSENSITIVE,
    )
    private val oClockPattern = Pattern.compile(
        "\\bat\\s+(\\d{1,2})\\s+o'?clock\\b",
        Pattern.CASE_INSENSITIVE,
    )
    private val spokenTimePattern = Pattern.compile(
        "\\bat\\s+(half\\s+past|quarter\\s+past|twenty\\s+past)\\s+(\\d{1,2})\\b",
        Pattern.CASE_INSENSITIVE,
    )

    fun parse(spokenText: String, today: LocalDate = LocalDate.now()): ParsedVoiceTask {
        var description = spokenText.trim()
        var date = today
        var time: LocalTime? = null

        val dateMatcher = datePattern.matcher(description)
        if (dateMatcher.find()) {
            val day = dateMatcher.group(1)?.toIntOrNull()
            val monthName = dateMatcher.group(2)
            if (day != null && monthName != null) {
                runCatching { LocalDate.of(today.year, monthFromName(monthName), day) }
                    .onSuccess { date = it }
                description = description.replaceRange(dateMatcher.start(), dateMatcher.end(), " ")
            }
        }

        val numericTimeMatcher = numericTimePattern.matcher(description)
        if (numericTimeMatcher.find()) {
            val hour = numericTimeMatcher.group(1)?.toIntOrNull()
            val minute = numericTimeMatcher.group(2)?.toIntOrNull()
            if (hour != null && minute != null) {
                runCatching { LocalTime.of(hour, minute) }.onSuccess { time = it }
                description = description.replaceRange(numericTimeMatcher.start(), numericTimeMatcher.end(), " ")
            }
        } else {
            val oClockMatcher = oClockPattern.matcher(description)
            if (oClockMatcher.find()) {
                val hour = oClockMatcher.group(1)?.toIntOrNull()
                if (hour != null) {
                    time = LocalTime.of(normaliseHour(hour), 0)
                    description = description.replaceRange(oClockMatcher.start(), oClockMatcher.end(), " ")
                }
            } else {
                val spokenMatcher = spokenTimePattern.matcher(description)
                if (spokenMatcher.find()) {
                    val phrase = spokenMatcher.group(1)
                    val hour = spokenMatcher.group(2)?.toIntOrNull()
                    if (phrase != null && hour != null) {
                        val minutes = when (phrase.lowercase(Locale.UK)) {
                            "half past" -> 30
                            "quarter past" -> 15
                            else -> 20
                        }
                        time = LocalTime.of(normaliseHour(hour), minutes)
                        description = description.replaceRange(spokenMatcher.start(), spokenMatcher.end(), " ")
                    }
                }
            }
        }

        return ParsedVoiceTask(
            description = description.replace(Regex("\\s+"), " ").trim(),
            date = date,
            time = time,
        )
    }

    private fun monthFromName(name: String): Int =
        Month.valueOf(name.uppercase(Locale.UK)).value

    private fun normaliseHour(hour: Int): Int = if (hour in 1..12) hour else hour % 24
}
