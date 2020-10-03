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

package com.doubleyellow.scoreboard;

import android.content.Context;
import com.doubleyellow.android.task.URLTask;
import com.doubleyellow.android.util.ContentReceiver;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extending URLTask mainly for
 * - hiding 'caching parameters' purposes (but doing caching nevertheless)
 * - using configurable timeout
 */
public class URLFeedTask extends URLTask
{
    //public static final String SQUORE_BASE_URL = "http://squore.double-yellow.be";
    public static String prefixWithBaseIfRequired(String sURL) {
        if ( sURL == null ) { return null; }
        if ( sURL.startsWith("http") == false ) {
            sURL = Brand.getBaseURL() + (sURL.startsWith("/")?"":"/") + sURL;

            // just a safety precaution
            while ( sURL.contains("squore/squore/") ) {
                sURL = sURL.replace("squore/squore/", "squore/");
            }
        }
        return sURL;
    }

    private int iReadTimeoutMS = 30000;
    public URLFeedTask(Context ctx, String sUrl) {
        this(ctx, sUrl, PreferenceValues.getFeedReadTimeout(ctx) * 1000);
    }
    private URLFeedTask(Context ctx, String sUrl, int iReadTimeoutMS) {
        super(ctx, sUrl, sUrl.replace(Brand.getBaseURL(), "").replaceAll("(.)/","$1.").replaceAll("[^a-zA-Z0-9\\.]", "") + ".txt", "(<html|<HTML|Undefined index)");
        this.iReadTimeoutMS = iReadTimeoutMS;
    }

    @Override public int getMaximumReuseCacheTimeMS(String sCacheFile) {
        return I_HOUR_IN_MILLISECS;
    }
    @Override public int getReadTimeout() {
        return iReadTimeoutMS;
    }

    @Override public boolean returnContent(String sURL) { return true; }
    @Override public String getBaseUrl(Context context) { return ""; }
    @Override public String getAccountName(Context context) { return ""; }
    @Override public String addParametersToReturnUrl(Context ctx, String sURL, String configuredAccountName, String sReturn) { return null; }

    @Override public boolean validateContent(String sURL, String sContent, String sValidation) {
        if ( StringUtil.isEmpty(sContent) ) {
            return false;
        }
        Pattern p = Pattern.compile(sValidation);
        Matcher m = p.matcher(sContent);
        boolean bValid = (m.find() == false);
        return bValid;
    }

    @Override public Map.Entry<ContentReceiver.FetchResult, String> downloadHelperFiles(Context ctx, String sURL, Map<String, String> hParams) {
        return null;
    }
}
