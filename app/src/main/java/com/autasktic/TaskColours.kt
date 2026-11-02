package com.autasktic

object TaskColours {
    val palette = listOf(
        "#D9EAD3",
        "#D0E0E3",
        "#FCE5CD",
        "#EADCF8",
        "#FFF2CC",
        "#F4CCCC",
        "#CFE2F3",
        "#D9D2E9",
        "#F9CB9C",
        "#B6D7A8",
        "#A2C4C9",
        "#FFD6E0",
        "#E2F0CB",
        "#DDEBF7",
        "#EADCF8",
        "#FCE4EC",
        "#FFF4CC",
        "#D5E8D4",
        "#E1D5E7",
        "#F8CECC",
    ).distinct()

    fun choose(usedColours: Set<String>): String {
        val unusedColours = palette.filterNot(usedColours::contains)
        return (unusedColours.ifEmpty { palette }).random()
    }
}
