package com.watchnav.com

/**
 * Represents parsed turn-by-turn direction data.
 */
data class NavDirection(
    val instruction: String,
    val distance: String,
    val street: String,
    val action: String = ""
)
