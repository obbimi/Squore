/*
 * Copyright (C) 2024  Iddo Hoeve
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
package com.doubleyellow.scoreboard.bluetooth_le;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.doubleyellow.scoreboard.R;

import com.doubleyellow.android.util.ContentUtil;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BLEUtil
{
    private final static String TAG = "SB.BLEUtil";

    @NonNull
    public static String[] getPermissions() {
        String[] permissions = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.S /* 31 */ ) {
            permissions = new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
        }
        return permissions;
    }

    public enum Keys {
        DeviceNameMustMatch,
        DeviceNameStartsWith,

        InitiateScoreChangeButton,

        /** one of the values of BLEDeviceButton */
        ConfirmScoreByOpponentButton,
        /** one of the values of BLEDeviceButton */
        CancelScoreByInitiatorButton,
        SingleDevice_ConfirmWithSameButton,
        RssiValueAt1M,

        /* Value BLE devices sends when a buttons is release. If specified, app only (re)-acts when a buttons is released, allowing easier clicking of multiple buttons */
        HandleOnReleaseValue,
        /** Array of messages (Message formats) */
        TranslateToBTMessage,
        /** if specified in the config, this exact nr of devices (1 or 2) need to be connected */
        NrOfDevices,
    }

    final public static ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

    public static List<CharSequence> getConfigs(Context context) {
        String sJson = ContentUtil.readRaw(context, R.raw.bluetooth_le_config);
        try {
            JSONObject config = new JSONObject(sJson);
            //String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config       , R.string.pref_BluetoothLE_Config_default      , context);

            List<CharSequence> lReturn = new ArrayList<>();
            Iterator<String> keys = config.keys();
            while(keys.hasNext()) {
                String sKey = keys.next();
                if ( sKey.startsWith(sKey.substring(0,3).toUpperCase())) {
                    // for now only list entries with first few characters uppercase
                    lReturn.add(sKey);
                }
            }
            return lReturn;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static JSONObject getActiveConfig(Context context) {
        String sJson = ContentUtil.readRaw(context, R.raw.bluetooth_le_config);
        try {
            JSONObject config = new JSONObject(sJson);
            String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config, R.string.pref_BluetoothLE_Config_default, context);
            config = config.getJSONObject(sBLEConfig);
            return  config;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** only to get BLE services/characteristics info about a device in nicely readable format */
    public static void printGattTable(BluetoothGatt gatt) {
        StringBuilder sb = new StringBuilder();
        sb.append(gatt.getDevice().getName()).append(" ").append(gatt.getDevice().getAddress()).append("\n");
        if ( gatt.getServices().isEmpty() ) {
            Log.d("BluetoothGatt","No service and characteristic available, call discoverServices() first?");
            return;
        }
        for(BluetoothGattService service: gatt.getServices()) {
            sb.append("Service " ).append(service.getUuid()).append("\n");
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic c: characteristics) {
                sb.append("|-- " ).append(c.getUuid());
                int p = c.getProperties();
                if ((p & BluetoothGattCharacteristic.PROPERTY_READ             ) != 0) sb.append(" READABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE            ) != 0) sb.append(" WRITABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) sb.append(" WRITABLE WITHOUT RESPONSE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_INDICATE         ) != 0) sb.append(" INDICATABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY           ) != 0) sb.append(" NOTIFIABLE");
                if (p == 0) sb.append(" EMPTY");
                sb.append("\n");

                List<BluetoothGattDescriptor> descriptors = c.getDescriptors();
                for(BluetoothGattDescriptor d: descriptors) {
                    sb.append("|---- " ).append(d.getUuid());
                    int p2 = d.getPermissions();
                    if ((p2 & BluetoothGattDescriptor.PERMISSION_READ ) != 0) sb.append(" READABLE");
                    if ((p2 & BluetoothGattDescriptor.PERMISSION_WRITE) != 0) sb.append(" WRITABLE");
                    if (p2 == 0) sb.append(" EMPTY");
                    sb.append("\n");
                }
            }
        }
        Log.d(TAG, sb.toString());
    }

    public static float getButtonFor(JSONObject mServicesAndCharacteristicsConfig, float fDefault) {
        return fDefault;
    }

    public static BLEDeviceButton getButtonFor(JSONObject mServicesAndCharacteristicsConfig, Keys eKey, BLEDeviceButton btnDefault) {
        String s = mServicesAndCharacteristicsConfig.optString(eKey.toString());
        if ( StringUtil.isEmpty(s) ) { return btnDefault; }
        BLEDeviceButton btnFromConfig = null;
        try {
            btnFromConfig = BLEDeviceButton.valueOf(s);
            return btnFromConfig;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return btnDefault;
    }

    /** transforms json like in bluetooth_le_config to easier iterable format */
    public static Map<String, List<String>> getServiceUUID2CharUUID(JSONObject mServicesAndCharacteristicsConfig) {
        Map<String, List<String>> mReturn = new HashMap<>();
        Iterator<String> itServiceUUID = mServicesAndCharacteristicsConfig.keys();
        while(itServiceUUID.hasNext() ) {
            String sServiceUUID = itServiceUUID.next();
            try {
                UUID.fromString(sServiceUUID); // to only process valid UUID's
                List<String> lCharUUID = new ArrayList<>();
                JSONObject mCharacteristicUUID = mServicesAndCharacteristicsConfig.getJSONObject(sServiceUUID);
                Iterator<String> itCharUUID = mCharacteristicUUID.keys();
                while ( itCharUUID.hasNext() ) {
                    String sCharacteristicUUID = itCharUUID.next();
                    lCharUUID.add(sCharacteristicUUID);
                }
                mReturn.put(sServiceUUID, lCharUUID);
            } catch (Exception e) {
                Log.w(TAG, "Skipping " + sServiceUUID);
            }
        }
        return mReturn;
    }
}
