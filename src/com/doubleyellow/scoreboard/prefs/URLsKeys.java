/*
 * Copyright (C) 2017  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.doubleyellow.scoreboard.prefs;

/**
 * Keys (values before the = sign) that may be used in the configuration
 * value of the preference 'feedPostUrls'
 */
public enum URLsKeys {
    Name,
    FeedPlayers,
    FeedMatches,
    /** an URL where match result should be posted to */
    PostResult,
    /**
     * Should contain a value of enum PostDataPreference
     */
    PostData,
    ValidFrom,
    ValidTo,
    /* None, Basic, ... */
    Authentication,
    Organization,
    Country,
        CountryCode,
    /** Typically used for tournaments */
    Region,
    /** Typically used for leagues */
    Section,

    /* Json config feed keys */
    config,
        avatarBaseURL,
        expandGroup,
        Placeholder_Match,
        Placeholder_Player,
        /** @Deprecated */
        Format_TeamPlayer,
        TextSizePercentage,
        Placeholder_TeamPlayer,
            AdditionalPostParams,
                PostAs,
                AllowedValues,
                DefaultValue,
                Optional,
                Caption,
                /** RadioButton (small number of 'short' values) or SelectList, or ToggleButton (only two values) */
                DisplayType,
        skipMatchSettings,
        name, /* Duplicate key.. careful, may be causing problems */
}
