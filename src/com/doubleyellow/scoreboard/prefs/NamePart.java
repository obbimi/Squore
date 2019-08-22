package com.doubleyellow.scoreboard.prefs;

/**
 * To indicate what part of a 'full name' of a player should be used.
 * Typically to prevent, that (if full name is used) autoresize will resize the text to very small.
 * If only first, or only last names are used autoresize can resize to bigger font to make it fit.
 */
public enum NamePart {
    First,
    Last,
    Full,
}
