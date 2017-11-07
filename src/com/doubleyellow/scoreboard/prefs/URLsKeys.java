package com.doubleyellow.scoreboard.prefs;

/**
 * Keys (values before the = sign) that may be used in the configuration
 * value of the preference 'feedPostUrls'
 */
public enum URLsKeys {
    Name,
    FeedPlayers,
    FeedMatches,
    PostResult,
    /**
     * Should contain a value of enum PostDataPreference
     */
    PostData,
    /**
     * plain, json
     */
    Format,
    ValidFrom,
    ValidTo,
    /* None, Basic, ... */
    Authentication,
    Organization,
    Country,
    Region,
}
