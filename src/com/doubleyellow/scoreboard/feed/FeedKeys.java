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

package com.doubleyellow.scoreboard.feed;

import com.doubleyellow.scoreboard.Brand;

import java.util.Locale;

/**
 * Keys for (how to present) the list of feeds.
 */
enum FeedKeys {
    FeedMetaData,
        /** array with sequence in with to present the feed types */
        Sequence,
        ShortDescription,
            /** Default sorting of feeds is sorting both header and items alpahbetically. For leaguemaster it is overwritten to NOT sort */
            SortOrder,
        ReuseCacheForXMinutes,
        DisplayName,
        Image,
            ImageURL,
        BGColor,
        TextColor,
        /* if no array with feeds is configured, an URL should be configured to fetch the json array elsewhere */
        URL,
        //DisplayFormat,

    ;
    public String getBrandSuffixed() {
        return this.toString() + "-" + Brand.brand;
    }
    public String getLangSuffixed(String sLang) {
        return this.toString() + "-" + sLang;
    }
    public String getLocalSuffixed(Locale locale) {
        return this.toString() + "-" + locale.toString();
    }
}
