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
package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.doubleyellow.scoreboard.R;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BLEActivity extends XActivity
{
    BluetoothAdapter   bluetoothAdapter = null;
    BluetoothLeScanner bleScanner = null;

    private final List<ScanResult>   scanResults          = new ArrayList<>();
    private final Map<String, Long>  mAddress2LastSeen    = new HashMap<>();
    private final Map<Player,String> mSelectedSeenDevices = new HashMap<>();
    private ScanResultAdapter scanResultAdapter = null;

    private boolean m_bIsScanning  = false;
    private Pattern pMustMatch     = null;
    private String  sMustStartWith = null;
    private double  fRssiValueAt1M = -50.0f;
    private Button  btnGo;

    private static final long lUpdateInterval     = 2000L; // must be smaller then 2 values below
    private static final long lCleanupCheckEvery  = 3000L;
    private static final long lCleanupIfUnseenFor = 4000L;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        setContentView(R.layout.ble_activity);

        btnGo = findViewById(R.id.ble_start_button);
        btnGo.setOnClickListener(v -> {
            bleScanner.stopScan(scanCallback);
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral1, this, "");
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral2, this, "");
            for(Player p : mSelectedSeenDevices.keySet() ) {
                String sAddress = mSelectedSeenDevices.get(p);
                PreferenceKeys key = p.equals(Player.A) ? PreferenceKeys.BluetoothLE_Peripheral1 : PreferenceKeys.BluetoothLE_Peripheral2;
                PreferenceValues.setString(key, this, sAddress);
            }
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        });
        btnGo.setEnabled(false);

        JSONObject config = BLEUtil.getActiveConfig(this);
        if ( config == null ) {
            Toast.makeText(this, "Could not obtain config for BLE", Toast.LENGTH_LONG).show();
            return;
        }
        fRssiValueAt1M = config.optDouble(BLEUtil.Keys.RssiValueAt1M.toString(), fRssiValueAt1M);

        String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, this);
        String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, this);
        Map mTmp = new HashMap();
        if ( StringUtil.isNotEmpty(sBluetoothLEDevice1) ) mTmp.put(Player.A, sBluetoothLEDevice1);
        if ( StringUtil.isNotEmpty(sBluetoothLEDevice2) ) mTmp.put(Player.B, sBluetoothLEDevice2);
        Map<Player, String> mSelectedPrefDevices = Collections.unmodifiableMap(mTmp);
        scanResultAdapter = new ScanResultAdapter(scanResults, fRssiValueAt1M, mSelectedSeenDevices, mSelectedPrefDevices);

        Button btnScan = findViewById(R.id.ble_scan_button);
        btnScan.setOnClickListener(v -> {
            if ( m_bIsScanning ) {
                m_bIsScanning = false;
                bleScanner.stopScan(scanCallback);
                //stopBleScan();
            } else {
                m_bIsScanning = true;
                if ( scanResults.size() > 0 ) {
                    scanResults.clear();
                    scanResultAdapter.notifyDataSetChanged();
                }
                bleScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
            }
            ((TextView)v).setText(m_bIsScanning?"Stop Scan":"Start Scan");
        });

        RecyclerView recyclerView = findViewById(R.id.scan_results_recycler_view);
        recyclerView.setAdapter(scanResultAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // why?
        recyclerView.setNestedScrollingEnabled(false);

        sMustStartWith = config.optString(BLEUtil.Keys.deviceNameStartsWith.toString());
        String sRegExp = config.optString(BLEUtil.Keys.deviceNameMustMatch     .toString());
        if ( StringUtil.isNotEmpty(sRegExp) ) {
            try {
                pMustMatch = Pattern.compile(sRegExp);
            } catch (Exception e) {
                Toast.makeText(this, "Devices pattern is not valid: " + sRegExp, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        bleScanner.stopScan(scanCallback);
    }

    @Override protected void onPause() {
        super.onPause();
    }

    private final ScanCallback scanCallback = new ScanCallback()
    {
        private long lLastCleanupOfDevicesNoLongerBroadcasting = 0L;

        private final Map mDiscardedBecauseOfRegexp = new HashMap();

        @Override public void onScanResult(int callbackType, ScanResult result) {
            if ( pMustMatch != null || StringUtil.isNotEmpty(sMustStartWith) ) {
                String name = result.getDevice().getName();

                if ( (name == null) || pMustMatch.matcher(name).matches() == false ) {
                    if ( mDiscardedBecauseOfRegexp.containsKey(name) == false ) {
                        mDiscardedBecauseOfRegexp.put(name, result.getDevice().getAddress());
                        Log.d(TAG, String.format("Skipping %s based on regexp %s", name, pMustMatch.pattern()));
                    }
                    return;
                }
                if ( (name == null) || name.startsWith(sMustStartWith) == false ) {
                    if ( mDiscardedBecauseOfRegexp.containsKey(name) == false ) {
                        mDiscardedBecauseOfRegexp.put(name, result.getDevice().getAddress());
                        Log.d(TAG, String.format("Skipping %s based on startswith %s", name, sMustStartWith));
                    }
                    return;
                }
            }

            int indexQuery = -1;
            final String address = result.getDevice().getAddress();
            long lNow = System.currentTimeMillis();

            for(int i=0; i < scanResults.size();i++) {
                String previousFoundAddress = scanResults.get(i).getDevice().getAddress();
                if ( previousFoundAddress.equalsIgnoreCase(address) ) {
                    indexQuery = i;
                    break;
                }
            }
            if ( indexQuery != -1 ) {
                // A scan result already exists with the same address
                Long lSeen = mAddress2LastSeen.get(address);
                long lDiff = lNow - lSeen;
                if ( lDiff > lUpdateInterval ) {
                    Log.i(TAG, "updating " + address);
                    scanResults.set(indexQuery, result);
                    scanResultAdapter.notifyItemChanged(indexQuery);
                    mAddress2LastSeen.put(address, lNow);
                }
            } else {
                Log.i(TAG, "adding " + address);
                scanResults.add(result);
                scanResultAdapter.notifyItemInserted(scanResults.size() - 1);
                //scanResultAdapter.notifyDataSetChanged();
                mAddress2LastSeen.put(address, lNow);
            }

            if ( lNow - lLastCleanupOfDevicesNoLongerBroadcasting > lCleanupCheckEvery ) {
                Log.i(TAG, "Checking for unseen devices");
                int iSizeBeforeClean = scanResults.size();
                for(int i = iSizeBeforeClean -1; i >=0; i--) {
                    String previousFoundAddress = scanResults.get(i).getDevice().getAddress();
                    long lLastSeen = mAddress2LastSeen.get(previousFoundAddress);
                    if ( lNow - lLastSeen > lCleanupIfUnseenFor ) {
                        Log.w(TAG, "Removing unseen device " + previousFoundAddress);
                        scanResults.remove(i);
                        scanResultAdapter.notifyItemRemoved(i);

                        mSelectedSeenDevices.remove(previousFoundAddress);
                    }
                }
                Log.i(TAG, "Selected Seen Devices : " + mSelectedSeenDevices);
                btnGo.setEnabled(mSelectedSeenDevices.size() == 2);
                lLastCleanupOfDevicesNoLongerBroadcasting = lNow;
            }
        }

        @Override public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}
