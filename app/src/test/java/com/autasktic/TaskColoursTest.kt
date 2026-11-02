package com.autasktic

import org.junit.Assert.assertTrue
import org.junit.Test

class TaskColoursTest {
    @Test
    fun choosesOnlyFromUnusedColoursWhenAvailable() {
        val used = TaskColours.palette.take(3).toSet()

        repeat(30) {
            assertTrue(TaskColours.choose(used) !in used)
        }
    }

    @Test
    fun reusesPaletteOnlyAfterEveryColourHasBeenUsed() {
        val used = TaskColours.palette.toSet()

        repeat(30) {
            assertTrue(TaskColours.choose(used) in TaskColours.palette)
        }
    }
}
