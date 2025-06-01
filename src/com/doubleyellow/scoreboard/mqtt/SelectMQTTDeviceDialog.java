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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.android.view.SelectObjectView;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.dialog.BaseAlertDialog;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.view.PreferenceCheckBox;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SelectMQTTDeviceDialog extends BaseAlertDialog {
    private static final String TAG = "SB." + SelectMQTTDeviceDialog.class.getSimpleName();

    public SelectMQTTDeviceDialog(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override
    public boolean storeState(Bundle outState) {
        return true;
    }

    @Override
    public boolean init(Bundle outState) {
        return true;
    }

    private SelectObjectView<String> sovDevices;

    @Override public void show() {
        adb.setTitle(getString(R.string.bt_select_device))
                .setIcon(R.drawable.mqtt)
                .setMessage(R.string.bt_select_device_for_scoreboard_mirroring)
                .setPositiveButton(android.R.string.ok, listener)
                .setNeutralButton(android.R.string.cancel, listener)
                .setNegativeButton(R.string.lbl_none, listener)
        ;

        // the checkboxes
        LinearLayout llCb = new LinearLayout(context);
        {
            llCb.setOrientation(LinearLayout.VERTICAL);

            // select how LR is applied on 'slave'
            boolean bKeepLRMirrored = PreferenceValues.BTSync_keepLROnConnectedDeviceMirrored(context);
            if (Brand.isNotSquash() || bKeepLRMirrored) {
                int iResDefault = PreferenceValues.getSportTypeSpecificResId(context, R.bool.BTSync_keepLROnConnectedDeviceMirrored_default__Squash);
                PreferenceCheckBox cbLRMirror = new PreferenceCheckBox(context, PreferenceKeys.BTSync_keepLROnConnectedDeviceMirrored, iResDefault);
                cbLRMirror.setText(R.string.pref_BTSync_keepLROnConnectedDeviceMirrored);
                cbLRMirror.setChecked(bKeepLRMirrored);
                cbLRMirror.setTag(ColorPrefs.Tags.header);

                llCb.addView(cbLRMirror);
            }
        }

        // add a view with all possible devices and let user choose one
        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        sovDevices = refreshSelectList(scoreBoard.getJoinedDevices());
        scoreBoard.setJoinedDevicesListener((lNew, sAddedRemoved) -> {
            ll.removeView(sovDevices);
            sovDevices = refreshSelectList(lNew);
            ll.addView(sovDevices, 3);
        } );
        ll.addView(getTextView(getString(R.string.sb_ThisDeviceId)));
        TextView tvThisDeviceId = new TextView(context);
        tvThisDeviceId.setText(PreferenceValues.getLiveScoreDeviceId(context));
        tvThisDeviceId.setTag(ColorPrefs.Tags.item.toString());
        ll.addView(tvThisDeviceId);
        ll.addView(getTextView(R.string.sb_MQTTSelectOtherDeviceId));
        ll.addView(sovDevices);
        ll.addView(llCb);

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
            scoreBoard.doDelayedMQTTReconnect("", 1, 1);
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
        String sPreviouslyConnected = PreferenceValues.getMQTTOtherDeviceId(context);
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
        lPairedDevicesChecked.add(getString(R.string.lbl_none));

        List<String> l = new ArrayList<>(lPairedDevicesChecked);
        sovDevices = new SelectObjectView<>(context, l, sCheckedDevice);
        ColorPrefs.setColors(sovDevices, ColorPrefs.Tags.item);
        return sovDevices;
    }

    /**
     * called from main activity before adding dialog to stack. Returns resource ids of messages to display if something went wrong
     */

    private DialogInterface.OnClickListener listener = (dialog, which) -> handleButtonClick(which);

    @Override public void handleButtonClick(int which) {

        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //if ( cbLRMirror != null ) {
                //    PreferenceValues.setBoolean(PreferenceKeys.BTSync_keepLROnConnectedDeviceMirrored, context, cbLRMirror.isChecked());
                //}
                String sOtherDeviceChecked = sovDevices.getChecked();
                if ( sOtherDeviceChecked != null && sOtherDeviceChecked.matches("^[A-Z0-9]{6,8}.*") && (sOtherDeviceChecked.equals(getString(R.string.lbl_none)) == false) ) {
                    PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, sOtherDeviceChecked);
                } else {
                    PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, "");
                }
                scoreBoard.stopMQTT();
                scoreBoard.doDelayedMQTTReconnect("", 1, 1);

                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                PreferenceValues.setString(PreferenceKeys.MQTTOtherDeviceId, context, "");

                scoreBoard.stopMQTT();
                scoreBoard.doDelayedMQTTReconnect("", 1, 1);

                break;
        }

        scoreBoard.triggerEvent(ScoreBoard.SBEvent.mirrorDeviceSelectionClosed, this);
    }
}
