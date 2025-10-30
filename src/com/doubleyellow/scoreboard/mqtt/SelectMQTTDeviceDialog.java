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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.doubleyellow.android.view.SelectObjectView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SelectMQTTDeviceDialog extends BaseAlertDialog {
    private static final String TAG = "SB." + SelectMQTTDeviceDialog.class.getSimpleName();

    private String S_NONE = null;
    private String S_NONE_DETECTED = null;

    public SelectMQTTDeviceDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
        S_NONE = getString(R.string.lbl_none);
        S_NONE_DETECTED = getString(R.string.sb_MQTT_NoJoinedDevicesYet);
    }

    @Override
    public boolean storeState(Bundle outState) {
        return true;
    }

    @Override
    public boolean init(Bundle outState) {
        return true;
    }

    private LinearLayout llDevices;
    private SelectObjectView<String> sovDevices;
    private ToggleButton mqttRoleIsSlaveButton;

    @Override public void show() {
        adb.setTitle(getString(R.string.bt_select_device))
                .setIcon(R.drawable.mqtt)
                .setMessage(R.string.bt_select_device_for_scoreboard_mirroring)
                .setPositiveButton(android.R.string.ok, listener)
                .setNeutralButton(android.R.string.cancel, listener)
                .setNegativeButton(R.string.lbl_none, listener)
        ;

        // the checkboxes
        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout twoCol = new LinearLayout(context);
        twoCol.setOrientation(LinearLayout.HORIZONTAL);

        twoCol.addView(getTextView(getString(R.string.sb_ThisDeviceId)));
        twoCol.addView(getTextView(PreferenceValues.getLiveScoreDeviceId(context)));
        ll.addView(twoCol);

        mqttRoleIsSlaveButton = new ToggleButton(context);
        mqttRoleIsSlaveButton.setTextOff(MQTTRole.Master.toString()); // default setting is off
        mqttRoleIsSlaveButton.setTextOn (MQTTRole.Slave.toString());
        String sOther = PreferenceValues.getMQTTOtherDeviceId(context);
        boolean isSlave = StringUtil.isNotEmpty(sOther);
        MQTTRole defaultRole = isSlave ? MQTTRole.Slave : MQTTRole.Master;
        mqttRoleIsSlaveButton.setChecked(isSlave);
        mqttRoleIsSlaveButton.setText(defaultRole.toString());
        ll.addView(mqttRoleIsSlaveButton);
        mqttRoleIsSlaveButton.setOnCheckedChangeListener((buttonView, checkedIsSlave) -> {
            llDevices.setVisibility(checkedIsSlave ? View.VISIBLE : View.GONE);
        });

        // add a view with all possible devices and let user choose one
        llDevices = new LinearLayout(context);
        llDevices.setOrientation(LinearLayout.VERTICAL);
        sovDevices = refreshSelectList(scoreBoard.getJoinedDevices());
        llDevices.addView(sovDevices);
        scoreBoard.setJoinedDevicesListener((lNew, sAddedRemoved) -> {
            llDevices.removeView(sovDevices);
            sovDevices = refreshSelectList(lNew);
            llDevices.addView(sovDevices);
        } );
        llDevices.setVisibility(isSlave ? View.VISIBLE : View.GONE);

        int iNoIconSize = PreferenceValues.getAppealHandGestureIconSize(context); // Yeghh: used to size the text...

        ll.addView(llDevices);

        // list of brokers
        String mqttBrokerURL = PreferenceValues.getMQTTBrokerURL(context);
        String[] saBrokerUrls = context.getResources().getStringArray(R.array.MQTTBrokerUrls);

        for (int i = 0; i < saBrokerUrls.length; i++ ) {
            String sBrokerUrl = saBrokerUrls[i];
            if ( sBrokerUrl.equals(getString(R.string.MQTTBrokerURL_Custom)) ) {
                sBrokerUrl = PreferenceValues.getMQTTBrokerURL_Custom(context);
                saBrokerUrls[i] = sBrokerUrl;
            }
        }

        SelectObjectView<String> sovBrokers = new SelectObjectView<>(context, Arrays.asList(saBrokerUrls), mqttBrokerURL);
        ColorPrefs.setColors(sovBrokers, ColorPrefs.Tags.item);
        sovBrokers.setOnCheckedChangeListener((group, checkedId) -> {
            scoreBoard.stopMQTT();
            PreferenceValues.setString(PreferenceKeys.MQTTBrokerURL, context, sovBrokers.getChecked());
            scoreBoard.doDelayedMQTTReconnect("", 1, 1, MQTTStatus.BrokerChangeInDialog);
        });
        ll.addView(getTextView(R.string.sb_MQTTSelectBroker));
        ll.addView(sovBrokers);

        adb.setView(ll);
        ColorPrefs.setColor(ll);

        dialog = adb.show();
    }

    private SelectObjectView<String> refreshSelectList(Set<String> lPairedDevicesChecked) {

        // pre-select
        // - first device, or
        // - previously selected device
        String sPreviouslyConnected = null; // PreferenceValues.getMQTTOtherDeviceId(context);
        String sCheckedDevice = null;
        if ( ListUtil.isEmpty(lPairedDevicesChecked) ) {
            // improve: show message now devices yet
            lPairedDevicesChecked.add(getString(R.string.sb_MQTT_NoJoinedDevicesYet));
        } else {
            sCheckedDevice = lPairedDevicesChecked.iterator().next();
            if ( StringUtil.isNotEmpty(sPreviouslyConnected) ) {
                for (String btd : lPairedDevicesChecked) {
                    if (sPreviouslyConnected.equalsIgnoreCase(btd)) {
                        sCheckedDevice = btd;
                        break;
                    }
                }
            }
        }

        // make current device still visible and appear selected even if it has not joined (yet)
        if ( StringUtil.isNotEmpty(sPreviouslyConnected) && lPairedDevicesChecked.contains(sPreviouslyConnected) == false ) {
            lPairedDevicesChecked.add(sPreviouslyConnected);
            sCheckedDevice = sPreviouslyConnected;
        }
        //lPairedDevicesChecked.add(S_NONE);

        List<String> l = new ArrayList<>(lPairedDevicesChecked);
        sovDevices = new SelectObjectView<>(context, l, sCheckedDevice);
        ColorPrefs.setColors(sovDevices, ColorPrefs.Tags.item);

        sovDevices.setOnCheckedChangeListener((group, checkedId) -> {
            String sOtherDeviceChecked = sovDevices.getChecked();
            boolean enabled = isValidSelection(sOtherDeviceChecked);
        });

        return sovDevices;
    }

    private boolean isValidSelection(String sOtherDeviceChecked) {
        return sOtherDeviceChecked != null
                && sOtherDeviceChecked.equalsIgnoreCase(S_NONE         ) == false
                && sOtherDeviceChecked.equalsIgnoreCase(S_NONE_DETECTED) == false;
    }

    /**
     * called from main activity before adding dialog to stack. Returns resource ids of messages to display if something went wrong
     */

    private DialogInterface.OnClickListener listener = (dialog, which) -> handleButtonClick(which);

    private final int TURN_ON_MQTT = DialogInterface.BUTTON_POSITIVE;
    private final int TURN_OFF_MQTT = DialogInterface.BUTTON_NEGATIVE;
    private final int LEAVE_AS_IS   = DialogInterface.BUTTON_NEUTRAL;

    @Override public void handleButtonClick(int which) {

        switch (which) {
            case TURN_ON_MQTT:
                String sOtherDeviceChecked = sovDevices.getChecked();
                boolean selected = mqttRoleIsSlaveButton.isChecked();
                if ( selected && isValidSelection(sOtherDeviceChecked) ) {
                    PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, sOtherDeviceChecked);
                } else {
                    PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, "");
                }
                scoreBoard.stopMQTT();
                scoreBoard.doDelayedMQTTReconnect("", 1, 1, MQTTStatus.CloseSelectDeviceDialog);

                break;
            case TURN_OFF_MQTT:
                //PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, "");
                PreferenceValues.setBoolean(PreferenceKeys.UseMQTT, context, false);

                scoreBoard.stopMQTT();

                break;
            case LEAVE_AS_IS:
                break;
        }

        scoreBoard.triggerEvent(ScoreBoard.SBEvent.mirrorDeviceSelectionClosed, this);
    }
}
