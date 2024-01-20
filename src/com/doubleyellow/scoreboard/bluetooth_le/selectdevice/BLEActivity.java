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

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.doubleyellow.scoreboard.R;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Activity in which user can start scanning for BLE devices and select a devices for player A and B.
 */
public class BLEActivity extends XActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    BluetoothAdapter   bluetoothAdapter = null;
    BluetoothLeScanner bleScanner       = null;

    private final List<ScanResult>   scanResults          = new ArrayList<>();
    private final Map<String, Long>  mAddress2LastSeen    = new HashMap<>();
    private final Map<Player,String> mSelectedSeenDevices = new HashMap<>();
    private       ScanResultAdapter  scanResultAdapter    = null;

    private boolean m_bIsScanning  = false;
    private Pattern pMustMatch     = null;
    private String  sMustStartWith = null;
    private double  fRssiValueAt1M = -50.0f;
    private Button  btnGo;
    private Button  btnScan;
    private    int  m_iProgress = -1;

    private static final long lUpdateInterval     = 1000L; // must be smaller then 2 values below
    private static final long lCleanupCheckEvery  = 2000L;
    private static final long lCleanupIfUnseenFor = 3000L;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if ( bleScanner == null ) {
            Toast.makeText(this, "Is bluetooth enabled?", Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(R.layout.ble_activity);

        btnGo = findViewById(R.id.ble_start_button);
        btnGo.setOnClickListener(v -> {
            bleScanner.stopScan(scanCallback);
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            for( Player p: Player.values() ) {
                String sDevice = mSelectedSeenDevices.get(p);
                if ( StringUtil.isNotEmpty(sDevice) ) {
                    intent.putExtra(p.toString(), sDevice);
                }
            }

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

        btnScan = findViewById(R.id.ble_scan_button);
        btnScan.setOnClickListener(v -> {
            if ( bleScanner == null ) {
                Toast.makeText(this, "Is bluetooth enabled?", Toast.LENGTH_LONG).show();
                return;
            }
            if ( m_bIsScanning ) {
                m_bIsScanning = false;
                bleScanner.stopScan(scanCallback);
            } else {
                m_bIsScanning = startScan(true);
            }
            updateScanButton(m_iProgress);
        });

        RecyclerView recyclerView = findViewById(R.id.scan_results_recycler_view);
        recyclerView.setAdapter(scanResultAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // why?
        recyclerView.setNestedScrollingEnabled(false);

        sMustStartWith = config.optString(BLEUtil.Keys.DeviceNameStartsWith.toString());
        String sRegExp = config.optString(BLEUtil.Keys.DeviceNameMustMatch     .toString());
        if ( StringUtil.isNotEmpty(sRegExp) ) {
            try {
                pMustMatch = Pattern.compile(sRegExp);
            } catch (Exception e) {
                Toast.makeText(this, "Devices pattern is not valid: " + sRegExp, Toast.LENGTH_LONG).show();
            }
        }

        //ColorPrefs.setColors(this, findViewById(R.id.ble_activity_root));
    }

    private CountDownTimer dcUpdateGui;
    private long           m_lStopScanningAfterXSeconds = 60 * 10; // TODO: preference?
    private boolean startScan(boolean bCheckPermissions) {

        if ( scanResults.size() > 0 ) {
            scanResults.clear();
            scanResultAdapter.notifyDataSetChanged();
            btnGo.setEnabled(false);
        }
        // TODO: check permissions BLUETOOTH_SCAN
        if ( ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_SCAN)  ) {
            String[] permissions = BLEUtil.getPermissions();
            ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal());
        } else {
            if ( ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED ) {
                bleScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
            } else {
                String sMsg = "The app has not been granted the permission to find nearby devices...";
                //Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
                (new AlertDialog.Builder(this)).setTitle("Permission").setIcon(android.R.drawable.stat_sys_data_bluetooth).setMessage(sMsg).show();
                return false;
            }
        }

        dcUpdateGui = new CountDownTimer(m_lStopScanningAfterXSeconds * 1000, 500) {
            @Override public void onTick(long millisUntilFinished) {
                checkForUnseen();
                btnGo.setEnabled(mSelectedSeenDevices.size() == 2);
                updateScanButton(m_iProgress++);
            }
            @Override public void onFinish() {
                m_bIsScanning = false;
                if ( bleScanner != null ) {
                    bleScanner.stopScan(scanCallback);
                }
                updateScanButton(0);
            }
        };
        dcUpdateGui.start();
        return true;
    }

    private long lLastCleanupOfDevicesNoLongerBroadcasting = 0L;

    private void checkForUnseen() {
        long lNow = System.currentTimeMillis();
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
                    if (MapUtil.isNotEmpty(mSelectedSeenDevices)) {
                        List<Player> lPlayersSelectedToUseDevice = new ArrayList<>();
                        for (Player p : Player.values()) {
                            String sSelectedForP = mSelectedSeenDevices.get(p);
                            if ( sSelectedForP.equalsIgnoreCase(previousFoundAddress) ) {
                                lPlayersSelectedToUseDevice.add(p);
                            }
                        }
                        if (ListUtil.isNotEmpty(lPlayersSelectedToUseDevice) ) {
                            for(Player p: lPlayersSelectedToUseDevice ) {
                                mSelectedSeenDevices.remove(p);
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "Selected Seen Devices : " + mSelectedSeenDevices);
            lLastCleanupOfDevicesNoLongerBroadcasting = lNow;
        }
    }
    private void updateScanButton( int iProgress ) {
        btnScan.setText(m_bIsScanning ? R.string.ble_stop_scan : R.string.ble_start_scan);

        // some stupid implementation to show scan is still busy
        if ( m_bIsScanning && iProgress > 0 ) {
            final String C_PROGRESS = ".....";
            String sCaption = getString(R.string.ble_scanning);
            int iSubStr = iProgress % C_PROGRESS.length();
            String sProgress = C_PROGRESS.substring(0,iSubStr) + " " + C_PROGRESS.substring(iSubStr+1);
            btnScan.setText(sCaption + " " + sProgress);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // 225, BLEUtil.permissions, [-1,-1,0,0]
        boolean bStartScan = true;
        for (int i = 0; i < permissions.length; i++) {
            if ( grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                bStartScan = false;
                break;
            }
        }
        if ( bStartScan ) {
            m_bIsScanning = startScan(false);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if ( bleScanner != null ) {
            bleScanner.stopScan(scanCallback);
        }
        if ( dcUpdateGui != null ) {
            dcUpdateGui.cancel();
        }
    }

    @Override protected void onPause() {
        super.onPause();
    }

    private final ScanCallback scanCallback = new ScanCallback()
    {
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

        }

        @Override public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scanning failed : " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

}
