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
    private final static String TAG = "SB." + BLEUtil.class.getSimpleName();

    public enum Keys {
        ShortDescription,
        SharedConfig,
        DeviceNameMustMatch,
        DeviceNameStartsWith,
        DeviceImageURL,

        InitiateScoreChangeButton,

        /** one of the values of BLEDeviceButton */
        ConfirmScoreByOpponentButton,
        InitiateOpponentScoreChangeButton,
        ConfirmScoreBySelfButton,
        /** one of the values of BLEDeviceButton */
        CancelScoreByInitiatorButton,
        CancelScoreByOpponentButton,
        SingleDevice_ConfirmWithSameButton,
        RssiValueAt1M,
        ManufacturerData_BatteryLevelAtPos,

        /* Value BLE devices sends when button/buttons is/are release. If specified, app only (re)-acts when buttons are released, allowing easier clicking of multiple buttons and reacting to holding button for certain period of time */
        HandleOnReleaseValue,
        /** Array of messages (Message formats) */
        TranslateToBTMessage,
        /** if specified in the config, this exact nr of devices (1 or 2) need to be connected */
        NrOfDevices,

        PlayerTypeConfig,
        RenameConfig,
            CleanOutCharactersRegExp,
            FreeTextMaxLength,
            FixedPrefix,
        PokeConfig,
            WriteToCharacteristic,
            WriteValue,
    }

    final public static ScanSettings scanSettings;
    static {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O /* 26 */ ) {
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
            builder.setLegacy(false);
        }

        scanSettings = builder.build();
    }
    /** To present list of possible configs to user */
    public static List<CharSequence> getConfigs(Context context, int iWhat1KeyOnly2Description3Both) {
        String sJson = ContentUtil.readRaw(context, R.raw.bluetooth_le_config);
        try {
            JSONObject config = new JSONObject(sJson);
            //String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config       , R.string.pref_BluetoothLE_Config_default      , context);

            List<CharSequence> lReturn = new ArrayList<>();
            Iterator<String> keys = config.keys();
            while(keys.hasNext()) {
                String sKey = keys.next();
                if ( sKey.startsWith("-") ) { continue; }
                if ( sKey.startsWith(sKey.substring(0,3).toUpperCase())) {
                    // for now only list entries with first few characters uppercase
                    switch (iWhat1KeyOnly2Description3Both) {
                        case 1:
                            lReturn.add(sKey);
                            break;
                        case 2:
                        case 3:
                            JSONObject joDetails = config.getJSONObject(sKey);
                            String sShortDescription = joDetails.getString(Keys.ShortDescription.toString());
                            if ( iWhat1KeyOnly2Description3Both == 2) {
                                lReturn.add(sShortDescription);
                            } else {
                                lReturn.add(sKey + " : " + sShortDescription);
                            }
                            break;
                    }
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
            final JSONObject config = new JSONObject(sJson);
            String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config, R.string.pref_BluetoothLE_Config_default, context);
            JSONObject configActive = config.optJSONObject(sBLEConfig);
            if ( configActive == null ) {
                sBLEConfig = context.getString(R.string.pref_BluetoothLE_Config_default);
                configActive = config.optJSONObject(sBLEConfig);
            }
            if ( configActive == null ) {
                sBLEConfig = context.getString(R.string.pref_BluetoothLE_Config_default2);
                configActive = config.optJSONObject(sBLEConfig);
            }
            if ( (configActive != null) && configActive.has(Keys.SharedConfig.toString() ) ) {
                String sShareConfig = (String) configActive.remove(Keys.SharedConfig.toString());
                JSONObject sharedConfig = config.getJSONObject(sShareConfig);
                Iterator<String> keys = sharedConfig.keys();
                while(keys.hasNext()) {
                    String sKey = keys.next();
                    if ( configActive.has(sKey) ) { continue; }
                    Object oValue = sharedConfig.get(sKey);
                    configActive.put(sKey, oValue);
                }
            }
            return configActive;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** only to get BLE services/characteristics info about a device in nicely readable format */
    static void printGattTable(BluetoothGatt gatt) {
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

/*
    public static float getButtonFor(JSONObject mServicesAndCharacteristicsConfig, float fDefault) {
        return fDefault;
    }
*/

    static BLEDeviceButton getButtonFor(JSONObject mServicesAndCharacteristicsConfig, Keys eKey, BLEDeviceButton btnDefault) {
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
    static Map<String, List<String>> getServiceUUID2CharUUID(JSONObject mServicesAndCharacteristicsConfig) {
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
                //Log.t(TAG, "Skipping " + sServiceUUID);
            }
        }
        return mReturn;
    }
}
