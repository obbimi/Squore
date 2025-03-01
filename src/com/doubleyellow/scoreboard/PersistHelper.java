/*
 * Copyright (C) 2020  Iddo Hoeve
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

import com.doubleyellow.scoreboard.archive.PreviousMatchSelector;
import com.doubleyellow.scoreboard.match.StaticMatchSelector;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.FileUtil;

import java.io.File;
import java.io.IOException;

public class PersistHelper {

    public static File getLastMatchFile(Context context) {
        File file = new File(PreviousMatchSelector.getArchiveDir(context), "LAST." + Brand.getSport() + ".sb");
        if ( file.exists() == false ) {
            File fPrevVersion = new File(PreviousMatchSelector.getArchiveDir(context), "LAST.sb");
            if ( fPrevVersion.exists() ) {
                fPrevVersion.renameTo(file);
            }
        }
        return file;
    }

    public static File storeAsPrevious(Context context, Model matchModel, boolean bForceStore) throws IOException {
        return storeAsPrevious(context, null, matchModel, bForceStore);
    }
    public static File storeAsPrevious(Context context, String sJson, Model matchModel, boolean bForceStore) throws IOException {
        if ( matchModel == null ) {
            matchModel = Brand.getModel();
            if ( sJson != null ) {
                matchModel.fromJsonString(sJson);
            }
        }
        if ( sJson == null ) {
            sJson = matchModel.toJsonString(context);
        }
        File fStore = matchModel.getStoreAs(PreviousMatchSelector.getArchiveDir(context));
        boolean bAtLeastOneGameFinished = matchModel.getNrOfFinishedGames() > 0;
        boolean bDurationExceeded2Minutes = matchModel.getDurationInMinutes() >= 2;

        Feature continueRecentMatch = PreferenceValues.continueRecentMatch(context);
        boolean bIsFromMyList       = StaticMatchSelector.matchIsFrom(matchModel.getSource());
        boolean bPossiblyContinueThisMatchAsRecent = ( (continueRecentMatch != Feature.DoNotUse) && bIsFromMyList);

        if ( bForceStore || bAtLeastOneGameFinished || bDurationExceeded2Minutes || bPossiblyContinueThisMatchAsRecent ) {
            FileUtil.writeTo(fStore, sJson);
        }
        return fStore;
    }

    public static void persist(Model matchModel, Context context) {
        try {
            if (matchModel == null) {
                return;
            }
            if (matchModel.isDirty() == false) {
                return;
            }

            String sJson = matchModel.toJsonString(context); // for 5 games match around 1400 characters
            //Log.d(TAG, "persist:" + sJson);
            File fLastMatch = PersistHelper.getLastMatchFile(context);
            FileUtil.writeTo(fLastMatch, sJson);

            // save named version only if it has progressed a little already
            // store name only when at least a game has been played so that 'restarting' a named games does not overwrite the stored result
            if ( PreferenceValues.saveMatchesForLaterUsage(context) ) {
                PersistHelper.storeAsPrevious(context, sJson, matchModel, false);
            }
            matchModel.setClean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
