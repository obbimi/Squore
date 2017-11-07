package com.doubleyellow.scoreboard.feed;

import com.doubleyellow.scoreboard.Brand;

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
}
