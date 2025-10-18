/*
 * Copyright (C) 2025  Iddo Hoeve
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

package com.doubleyellow.scoreboard.mqtt;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.util.MapUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MQTTStatusDialog extends BaseAlertDialog {
    public MQTTStatusDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        return false;
    }

    @Override public boolean init(Bundle outState) {
        return false;
    }

    @Override public void show() {
        Set<String> lSubscriptionTopics = scoreBoard.m_MQTTHandler.getSubscriptionTopics();
        Set<String> lJoinedDevices = scoreBoard.getJoinedDevices();

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        String subTopics = ListUtil.join(lSubscriptionTopics, "\n");
        String sJoinedDevices = ListUtil.join(lJoinedDevices, "\n");
        //String sPublishStats = MapUtil.toNiceString(scoreBoard.m_MQTTHandler.getStats(), 0, true, MapUtil.Sort.ByKey);
        List<String> lStats = MapUtil.map2listOfStrings(scoreBoard.m_MQTTHandler.getStats(), "%s = %s");

        int iPrefixLength = 1;
        String sPrefix = "";
        do {
            String sPrefixTst = lStats.get(0).substring(0,iPrefixLength);
            Collection lSamePrefix = ListUtil.filter(lStats, "\\Q" + sPrefixTst + "\\E.+", Enums.Match.Keep);
            if ( ListUtil.size(lSamePrefix) < ListUtil.size(lStats) ) {
                break;
            }
            sPrefix = sPrefixTst;
            iPrefixLength++;
        } while (true);

        lStats = ListUtil.replace(lStats, "\\Q" + sPrefix + "\\E(.+)", "$1");

        String sPublishStats = ListUtil.join(lStats, "\n");
        String mqttOtherDeviceId = PreferenceValues.getMQTTOtherDeviceId(context);
        adb.setIcon   (R.drawable.mqtt)
           .setTitle(getString(R.string.pref_Category_MQTT) + ": " + PreferenceValues.getLiveScoreDeviceId(context) + ": " + sPrefix)
           .setMessage("Subscriptions:\n" + subTopics + "\n\nStats:\n" + sPublishStats + "\n\nDevices:\n" + sJoinedDevices + "\n\nOther: " + mqttOtherDeviceId)
           .setPositiveButton(R.string.cmd_ok       , dialogClickListener)
           .setNeutralButton (R.string.cmd_stop_temp, dialogClickListener)
           .setNegativeButton(R.string.cmd_stop_mqtt, dialogClickListener)
           .setOnKeyListener(getOnBackKeyListener())
           .setView(ll);

        // in a try catch to prevent crashing if somehow scoreBoard is not showing any longer
        try {
            dialog = adb.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> handleButtonClick(which);
    public static final int NOTHING     = DialogInterface.BUTTON_POSITIVE;
    public static final int STOP_RESTARTABLE_MQTT = DialogInterface.BUTTON_NEUTRAL;
    public static final int STOP_MQTT   = DialogInterface.BUTTON_NEGATIVE;

    @Override public void handleButtonClick(int which) {
        switch (which) {
            case NOTHING:
                break;
            case STOP_MQTT:
                scoreBoard.stopMQTT();
                PreferenceValues.setBoolean(PreferenceKeys.UseMQTT, context, false);
                break;
            case STOP_RESTARTABLE_MQTT:
                scoreBoard.stopMQTT();
                break;
        }
        scoreBoard.triggerEvent(ScoreBoard.SBEvent.dialogClosed, this);
        super.handleButtonClick(which);
    }
}
