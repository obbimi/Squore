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

enum FeedKeys {
    FeedMetaData,
        /** array with sequence in with to present the feed types */
        Sequence,
        ShortDescription,
        DisplayName,
        Image,
            ImageURL,
        BGColor,
        TextColor,
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
