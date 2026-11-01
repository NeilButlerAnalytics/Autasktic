package com.autasktic.voice

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceTaskParserTest {
    private val today = LocalDate.of(2026, 11, 1)

    @Test
    fun parsesTimeAndRemovesItFromDescription() {
        val result = VoiceTaskParser.parse("Ring the dentist at 3 o'clock", today)

        assertEquals("Ring the dentist", result.description)
        assertEquals(LocalTime.of(3, 0), result.time)
        assertEquals(today, result.date)
    }

    @Test
    fun parsesDateAndTimeTogether() {
        val result = VoiceTaskParser.parse("Call Dave on 12th December at 10:30", today)

        assertEquals("Call Dave", result.description)
        assertEquals(LocalDate.of(2026, 12, 12), result.date)
        assertEquals(LocalTime.of(10, 30), result.time)
    }

    @Test
    fun defaultsToSelectedDateWithoutGuessingTime() {
        val result = VoiceTaskParser.parse("Buy food", today)

        assertEquals("Buy food", result.description)
        assertEquals(today, result.date)
        assertNull(result.time)
    }
}
