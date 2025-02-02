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
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.doubleyellow.prefs.RWValues;
import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.bluetooth.BTUtil;
import com.doubleyellow.scoreboard.dialog.MyDialogBuilder;
import com.doubleyellow.scoreboard.R;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.bluetooth.BTMessage;
import com.doubleyellow.scoreboard.bluetooth_le.BLEReceiverManager;
import com.doubleyellow.scoreboard.bluetooth_le.BLEState;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Enums;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Activity in which user can start scanning for BLE devices and select devices for player A and B.
 */
public class BLEActivity extends XActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    BluetoothAdapter   bluetoothAdapter = null;
    BluetoothLeScanner bleScanner       = null;

    private final List<ScanResult>   scanResults          = new ArrayList<>();
    private final Map<String, Long>  mAddress2LastSeen    = new HashMap<>();
    private final Map<Player,String> mSelectedSeenDevices = new HashMap<>();
    private       ScanResultAdapter  scanResultAdapter    = null;

    private JSONObject m_bleConfig;

    private boolean   m_bIsScanning        = false;
    private Pattern   pMustMatch           = null;
    private String    sMustStartWith       = null;
    private double    fRssiValueAt1M       = -50.0f;
    private int       iNrOfDevicesRequired = -1;
    private ViewGroup vgScan;
    private ViewGroup vgVerify;
    private Button    btnGo;
    private Button    btnVerify;
    private Button    btnScan;
    private ProgressBar pbVerify = null;
    private    int    m_iProgress = -1;

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
        String sBLEConfig = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Config, R.string.pref_BluetoothLE_Config_default, this);

        btnGo = findViewById(R.id.ble_start_button);
        btnGo.setOnClickListener(v -> {
            bleScanner.stopScan(scanCallback);

            // if verify was used, close the connection
            if ( m_bleReceiverManager != null ) {
                m_bleReceiverManager.closeConnection();
            }

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
        btnGo.setText(R.string.ble_select_devices_to_use_for_scoring);

        btnVerify = findViewById(R.id.ble_connect_and_verify_button);
        btnVerify.setOnClickListener(v -> {
            initForVerify();
        });
        btnVerify.setVisibility(View.INVISIBLE);

        vgScan = findViewById(R.id.cl_scan_and_select);
        vgVerify = findViewById(R.id.cl_verify);

        vgVerify.findViewById(R.id.ble_cav_swap_devices).setOnClickListener(v -> {
            String sNewDeviceForB = mSelectedSeenDevices.get(Player.A);
            String sNewDeviceForA = mSelectedSeenDevices.get(Player.B);
            mSelectedSeenDevices.put(Player.A, sNewDeviceForA);
            mSelectedSeenDevices.put(Player.B, sNewDeviceForB);
            initForVerify();
        });
        vgVerify.findViewById(R.id.ble_cav_rescan_for_devices).setOnClickListener(v -> {
            initForScan(true);
        });
        pbVerify = vgVerify.findViewById(R.id.ble_cav_progressbar);

        m_bleConfig = BLEUtil.getActiveConfig(this);
        if ( m_bleConfig == null ) {
            Toast.makeText(this, "Could not obtain config for BLE. Check your settings.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            String sShortDescription = m_bleConfig.getString(BLEUtil.Keys.ShortDescription.toString());
            setTitle(getString(R.string.ble_devices) + " : " + sBLEConfig + " : " + sShortDescription);
        } catch (Exception e) {
        }

        fRssiValueAt1M       = m_bleConfig.optDouble(BLEUtil.Keys.RssiValueAt1M.toString(), fRssiValueAt1M);
        int iManufacturerData_BatteryLevelAtPos = m_bleConfig.optInt(BLEUtil.Keys.ManufacturerData_BatteryLevelAtPos.toString(), -1);
        iNrOfDevicesRequired = m_bleConfig.optInt(BLEUtil.Keys.NrOfDevices.toString(), iNrOfDevicesRequired);

        String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, this);
        String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, this);
        Map mTmp = new HashMap();
        if ( StringUtil.isNotEmpty(sBluetoothLEDevice1) ) mTmp.put(Player.A, sBluetoothLEDevice1);
        if ( StringUtil.isNotEmpty(sBluetoothLEDevice2) ) mTmp.put(Player.B, sBluetoothLEDevice2);
        Map<Player, String> mSelectedPrefDevices = Collections.unmodifiableMap(mTmp);
        scanResultAdapter = new ScanResultAdapter(scanResults, fRssiValueAt1M, iManufacturerData_BatteryLevelAtPos, mSelectedSeenDevices, mSelectedPrefDevices);

        btnScan = findViewById(R.id.ble_scan_button);
        btnScan.setOnClickListener(v -> {
            if ( bleScanner == null ) {
                Toast.makeText(this, "Is bluetooth enabled?", Toast.LENGTH_LONG).show();
                return;
            }
            if ( m_bIsScanning ) {
                stopScanning();
            } else {
                m_bIsScanning = startScan();
            }
            updateScanButton(m_iProgress);
        });

        RecyclerView recyclerView = findViewById(R.id.scan_results_recycler_view);
        recyclerView.setAdapter(scanResultAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // why?
        recyclerView.setNestedScrollingEnabled(false);

        sMustStartWith = m_bleConfig.optString(BLEUtil.Keys.DeviceNameStartsWith.toString());
        String sRegExp = m_bleConfig.optString(BLEUtil.Keys.DeviceNameMustMatch     .toString());
        if ( StringUtil.isNotEmpty(sRegExp) ) {
            try {
                pMustMatch = Pattern.compile(sRegExp);
            } catch (Exception e) {
                Toast.makeText(this, "Devices pattern is not valid: " + sRegExp, Toast.LENGTH_LONG).show();
            }
        }

        m_cdUpdateGui = new CountDownTimer(m_lStopScanningAfterXSeconds * 1000, 500) {
            @Override public void onTick(long millisUntilFinished) {
                m_iProgress++;
                checkForUnseen(m_iProgress);
                int iDifferentDevices = (new LinkedHashSet<>(mSelectedSeenDevices.values())).size();
                int iNrOfPlayer2DevicesSelectionsMade = mSelectedSeenDevices.size();
                String sCaptionGo     = getResources().getQuantityString(R.plurals.ble_start_scoring_with_devices, iDifferentDevices);
                String sCaptionVerify = getResources().getQuantityString(R.plurals.ble_connect_and_verify        , iDifferentDevices);
                if ( iDifferentDevices == 0 || iNrOfPlayer2DevicesSelectionsMade != 2 ) {
                    sCaptionGo = getString(R.string.ble_select_devices_to_use_for_scoring);
                }
                if ( iNrOfDevicesRequired == -1 ) {
                    // 1 or 2 devices selected for both players is allowed
                    boolean enabled = iNrOfPlayer2DevicesSelectionsMade == 2;
                    btnGo.setEnabled(enabled);
                } else {
                    // exact nr of required devices selected
                    boolean enabled = iNrOfPlayer2DevicesSelectionsMade == 2 && (iDifferentDevices == iNrOfDevicesRequired);
                    sCaptionGo     = getResources().getQuantityString(R.plurals.ble_start_scoring_with_devices, iNrOfDevicesRequired);
                    sCaptionVerify = getResources().getQuantityString(R.plurals.ble_connect_and_verify        , iNrOfDevicesRequired);
                    btnGo.setEnabled(enabled);
                }
                btnVerify.setText(sCaptionVerify);
                btnVerify.setVisibility(btnGo.isEnabled()? View.VISIBLE:View.INVISIBLE);

                btnGo.setText(sCaptionGo);
                updateScanButton(m_iProgress);
            }
            @Override public void onFinish() {
                stopScanning();
                m_bIsScanning = false;
                if ( bleScanner != null ) {
                    bleScanner.stopScan(scanCallback);
                }
                updateScanButton(0);
            }
        };

        //ColorPrefs.setColors(this, findViewById(R.id.ble_activity_root));
        initForScan(false);
    }

    private CountDownTimer m_cdUpdateGui;
    private long           m_lStopScanningAfterXSeconds = 60; // TODO: preference?
    private boolean startScan() {
        if ( scanResults.size() > 0 ) {
            scanResults.clear();
            scanResultAdapter.notifyDataSetChanged();
            btnGo.setEnabled(false);
        }
        String[] permissions = BTUtil.getPermissions();
        Set<RWValues.Permission> lPerms = new LinkedHashSet<>();
        for( String sPermName: permissions ) {
            RWValues.Permission permission = PreferenceValues.getPermission(this, PreferenceKeys.UseBluetoothLE, sPermName);
            lPerms.add(permission);
        }
        if ( lPerms.size() == 1 && lPerms.iterator().next().equals(RWValues.Permission.Granted) ) {
            // all good
        } else if ( lPerms.contains(RWValues.Permission.Denied) ) {
            if ( PreferenceValues.currentDateIsTestDate() ) {
                ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal()); // API28 : Can request only one set of permissions at a time
                return false;
            }
            (new AlertDialog.Builder(this)).setTitle("Permissions").setIcon(android.R.drawable.stat_sys_data_bluetooth).setMessage(R.string.ble_permission_not_granted).show();
            return false;
        } else {
            // request missing
            Log.w(TAG, "Trying to request permission for scanning");
            ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal()); // API28 : Can request only one set of permissions at a time
            return false;
        }

        bleScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
        m_cdUpdateGui.start();
        return true;
    }

    private long lLastCleanupOfDevicesNoLongerBroadcasting = 0L;

    private void checkForUnseen(int iProgress) {
        long lNow = System.currentTimeMillis();
        if ( lNow - lLastCleanupOfDevicesNoLongerBroadcasting > lCleanupCheckEvery ) {
            if ( ListUtil.isNotEmpty(scanResults) ) {
                Log.d(TAG, "Checking for unseen devices");
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
                            for ( Player p : mSelectedSeenDevices.keySet() ) {
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
                Log.d(TAG, "Selected Seen Devices : " + mSelectedSeenDevices);
            } else {
                if ( mDiscardedBecauseOfRegexp.size() == 0 && iProgress % 4 == 0 ) {
                    Log.w(TAG, "Most likely 'Location' permissions are set to 'Ask every time'.. actual scanning not functional");
                }
            }
            lLastCleanupOfDevicesNoLongerBroadcasting = lNow;
        }
    }
    private void updateScanButton( int iProgress ) {
        if ( btnScan == null ) { return; }
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
        boolean bStartScan = permissions.length > 0;
        for (int i = 0; i < permissions.length; i++) {
            if ( grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                bStartScan = false;
                break;
            }
        }
        if ( bStartScan ) {
            m_bIsScanning = startScan();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        stopScanning();
    }

    private void stopScanning() {
        if ( bleScanner != null ) {
            bleScanner.stopScan(scanCallback);
        }
        if ( m_bleReceiverManager != null ) {
            m_bleReceiverManager.closeConnection();
        }
        if ( m_cdUpdateGui != null ) {
            m_cdUpdateGui.cancel();
        }
        m_bIsScanning = false;
        updateScanButton(0);
    }

    @Override protected void onPause() {
        super.onPause();
    }

    private final Map<String, String> mDiscardedBecauseOfRegexp = new HashMap<>();
    private final ScanCallback scanCallback = new ScanCallback()
    {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            if ( pMustMatch != null || StringUtil.isNotEmpty(sMustStartWith) ) {
                String name = result.getDevice().getName();

                if ( pMustMatch != null ) {
                    if ( (name == null) || pMustMatch.matcher(name).matches() == false ) {
                        if ( mDiscardedBecauseOfRegexp.containsKey(name) == false ) {
                            mDiscardedBecauseOfRegexp.put(name, result.getDevice().getAddress());
                            Log.d(TAG, String.format("Skipping %s based on regexp %s", name, pMustMatch.pattern()));
                        }
                        return;
                    }
                }
                if ( sMustStartWith != null ) {
                    if ( (name == null) || name.startsWith(sMustStartWith) == false ) {
                        if ( mDiscardedBecauseOfRegexp.containsKey(name) == false ) {
                            mDiscardedBecauseOfRegexp.put(name, result.getDevice().getAddress());
                            Log.d(TAG, String.format("Skipping %s based on startswith %s", name, sMustStartWith));
                        }
                        return;
                    }
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
                    //Log.d(TAG, "updating " + address);
                    scanResults.set(indexQuery, result);
                    scanResultAdapter.notifyItemChanged(indexQuery); // ensures onBindViewHolder() is called again
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
    private final Map<Player, Integer> m_playerToVerifyButtonResId = MapUtil.getMap(Player.A, R.id.ble_cav_device_1, Player.B, R.id.ble_cav_device_2);
    private final Map<Player, Integer> m_playerToBatterLevelResId = MapUtil.getMap(Player.A, R.id.ble_cav_device_batterylevel_1, Player.B, R.id.ble_cav_device_batterylevel_2);
    private final Map<Player, Integer> m_playerToRenameResId = MapUtil.getMap(Player.A, R.id.ble_cav_device_rename_1, Player.B, R.id.ble_cav_device_rename_2);

    private BLEReceiverManager m_bleReceiverManager = null;
    private void initForScan(boolean bStartScan) {
        if ( m_bleReceiverManager != null ) {
            m_bleReceiverManager.closeConnection();
        }
        vgScan.setVisibility(View.VISIBLE);
        vgVerify.setVisibility(View.INVISIBLE);
        m_iProgress = -1;
        updateScanButton(m_iProgress);
        if ( bStartScan ) {
            m_bIsScanning = startScan();
        }
    }
    private void initForVerify() {
        stopScanning();

        vgScan.setVisibility(View.INVISIBLE);
        vgVerify.setVisibility(View.VISIBLE);
        pbVerify.setVisibility(View.VISIBLE);
        ImageView view = vgVerify.findViewById(R.id.ble_cav_image);
        if ( view != null ) {
            String sImageUrl = m_bleConfig.optString(BLEUtil.Keys.DeviceImageURL.toString());
            if ( StringUtil.isNotEmpty(sImageUrl) ) {
                PreferenceValues.downloadAvatar(this, view, sImageUrl );
            } else {
                view.setVisibility(View.GONE);
            }
        }

        int[] iaInitiallyDisable = new int[] { R.id.ble_cav_device_1, R.id.ble_cav_device_2, R.id.ble_cav_device_both,R.id.ble_cav_device_rename_1, R.id.ble_cav_device_rename_2, R.id.ble_cav_device_rename_both, R.id.ble_cav_swap_devices };
        for(int iResId: iaInitiallyDisable ) {
            View vBtn = vgVerify.findViewById(iResId);
            if ( vBtn == null ) { continue; }
            vBtn.setEnabled(false);
        }
        String sBatterLevelUnknown = "? %";
            // sBatterLevelUnknown = getString(R.string.ble_request_batterylevel);
        int iDifferentDevices = (new LinkedHashSet<>(mSelectedSeenDevices.values())).size();
        final JSONObject joRenameConfig = m_bleConfig.optJSONObject(BLEUtil.Keys.RenameConfig.toString());
        for(Player p: Player.values() ) {
            Integer iResId = m_playerToVerifyButtonResId.get(p);
            Button btn = vgVerify.findViewById(iResId);
            String sDeviceName = ""; // retrieve when actually connected
            String sPlayerName = ScoreBoard.getMatchModel().getName(p);
            String sTxt = getString(R.string.ble_cav_poke_device_x, sDeviceName, sPlayerName);
            btn.setText(sTxt);
            btn.setOnClickListener(v -> {
                m_bleReceiverManager.writeToBLE(p, BLEUtil.Keys.PokeConfig, null);
            });
            if ( joRenameConfig != null ) {
                //btn.setOnLongClickListener(v -> renameDeviceDialog(p, joRenameConfig));

                Integer iResIdRN = m_playerToRenameResId.get(p);
                Button btnRN = vgVerify.findViewById(iResIdRN);
                btnRN.setText(R.string.cmd_rename);
                btnRN.setOnClickListener(v -> {
                    renameDeviceDialog(p, joRenameConfig);
                });
                btnRN.setVisibility(iDifferentDevices==2 ? View.VISIBLE : View.GONE);
            }
            btn.setVisibility(iDifferentDevices==2 ? View.VISIBLE : View.GONE);

            Integer iResIdBL = m_playerToBatterLevelResId.get(p);
            Button btnBL = vgVerify.findViewById(iResIdBL);
            btnBL.setText(sBatterLevelUnknown);
            btnBL.setOnClickListener(v -> {
                m_bleReceiverManager.readBatteryLevel(p);
            });
            btnBL.setVisibility(iDifferentDevices==2 ? View.VISIBLE : View.GONE);
        }
        View btnSwapDevices               = vgVerify.findViewById(R.id.ble_cav_swap_devices);
        btnSwapDevices.setVisibility(iDifferentDevices==2 ? View.VISIBLE : View.GONE);

        Button btnBoth = vgVerify.findViewById(R.id.ble_cav_device_both);
        btnBoth.setOnClickListener(v -> {
            m_bleReceiverManager.writeToBLE(null, BLEUtil.Keys.PokeConfig, null);
        });
        if ( joRenameConfig != null ) {
            //btnBoth.setOnLongClickListener(v -> renameDeviceDialog(null, joRenameConfig));

            Button btnRNBoth = vgVerify.findViewById(R.id.ble_cav_device_rename_both);
            btnRNBoth.setText(R.string.cmd_rename);
            btnRNBoth.setOnClickListener(v -> {
                renameDeviceDialog(null, joRenameConfig);
            });
            btnRNBoth.setVisibility(iDifferentDevices==1 ? View.VISIBLE : View.GONE);
        }
        btnBoth.setVisibility(iDifferentDevices==1 ? View.VISIBLE : View.GONE);

        Button btnBLBoth = vgVerify.findViewById(R.id.ble_cav_device_batterylevel_both);
        btnBLBoth.setText(sBatterLevelUnknown);
        btnBLBoth.setOnClickListener(v -> {
            m_bleReceiverManager.readBatteryLevel(null);
        });
        btnBLBoth.setVisibility(iDifferentDevices==1 ? View.VISIBLE : View.GONE);

        if ( m_bleReceiverManager != null ) {
            // e.g. if swap devices is clicked
            m_bleReceiverManager.closeConnection();
        }
        m_bleReceiverManager = new BLEReceiverManager(this, BluetoothAdapter.getDefaultAdapter(), mSelectedSeenDevices.get(Player.A), mSelectedSeenDevices.get(Player.B), m_bleConfig);
        m_bleReceiverManager.setHandler(new BLEVerifyHandler(this));
        m_bleReceiverManager.startReceiving();
    }
    private class BLEVerifyHandler extends Handler {
        private Context m_context = null;
        BLEVerifyHandler(Context context) {
            m_context = context;
        }
        @Override public void handleMessage(Message msg) {
            BTMessage btMessage = BTMessage.values()[msg.what];
            //Log.d(TAG, "msg.what : " + msg.what + " : " + btMessage);
            String sMsg = String.valueOf(msg.obj);
/*
            Log.d(TAG, "msg.obj  : " + msg.obj); // device mac
            Log.d(TAG, "msg.arg1 : " + msg.arg1);
            Log.d(TAG, "msg.arg2 : " + msg.arg2);
*/
            switch (btMessage) {
                case READ:
                    try {
                        Toast.makeText(m_context, sMsg, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG, "Message could not be understood :" + sMsg);
                    }
                    break;
                case READ_RESULT_BATTERY: {
                    int iBatteryLevel = msg.arg1;
                    int iPlayer       = msg.arg2;
                    String sDevice    = sMsg;
                    Player p = null;
                    if (0 <= iPlayer && iPlayer <= 1) {
                        p = Player.values()[iPlayer];
                    }
                    int iResId = R.id.ble_cav_device_batterylevel_both;
                    if ( p != null ) {
                        iResId = m_playerToBatterLevelResId.get(p);
                    }
                    TextView txtView = vgVerify.findViewById(iResId);
                    txtView.setText(iBatteryLevel + "%");
                }
                break;
                case INFO: {
                    int iNrOfDevices = msg.arg2;
                    try {
                        String sMsg2 = m_context.getString(msg.arg1, iNrOfDevices, sMsg, Brand.getShortName(m_context));
                        Toast.makeText(m_context, sMsg2, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.w(TAG, "Error while constructing message : " + sMsg);
                        throw new RuntimeException(e);
                    }
                }
                break;
                case STATE_CHANGE: {
                    BLEState btState = BLEState.values()[msg.arg1];
                    int iNrOfDevices = msg.arg2;
                    String sDevice = (String) msg.obj;
                    if ( sDevice == null ) {
                        sDevice = "No device mac for state change";
                    }
                    Map<Player, String> usedForPlayers = MapUtil.filterValues(mSelectedSeenDevices, sDevice, Enums.Match.Keep);
                    Log.d(TAG, String.format("[Verify] device %s, state %s, players connected to device %d (%s)", sDevice, btState, MapUtil.size(usedForPlayers), usedForPlayers.keySet()));
                    //Log.d(TAG, "new state  : " + btState);
                    Log.d(TAG, "iNrOfDevices: " + iNrOfDevices);
                    TextView btnVerifySingleDeviceForBoth = vgVerify.findViewById(R.id.ble_cav_device_both);
                    TextView btnRenameSingleDeviceForBoth = vgVerify.findViewById(R.id.ble_cav_device_rename_both);
                    TextView btnSwapDevices               = vgVerify.findViewById(R.id.ble_cav_swap_devices);
                    switch (btState) {
                        case CONNECTED_DiscoveringServices:
                        case CONNECTED_TO_1_of_2:
                        case CONNECTED_ALL:
                            btnVerifySingleDeviceForBoth.setEnabled(true);
                            String sDeviceName = m_bleReceiverManager.getDeviceName(null);
                            String sTxt        = getString(R.string.ble_cav_poke_device_both, sDeviceName);
                            btnVerifySingleDeviceForBoth.setText(sTxt);

                            btnRenameSingleDeviceForBoth.setEnabled(true);
                            for(Object o: usedForPlayers.keySet() ) {
                                Player p = Player.valueOf(String.valueOf(o));
                                int iResId = m_playerToVerifyButtonResId.get(p);
                                TextView btnVerify = vgVerify.findViewById(iResId);
                                String sPlayerName = ScoreBoard.getMatchModel().getName(p);
                                       sDeviceName = m_bleReceiverManager.getDeviceName(p);
                                       sTxt        = getString(R.string.ble_cav_poke_device_x, sDeviceName, sPlayerName);
                                btnVerify.setText(sTxt);

                                btnVerify.setEnabled(true);
                                iResId = m_playerToRenameResId.get(p);
                                TextView viewById = vgVerify.findViewById(iResId);
                                viewById.setEnabled(true);
                            }

                            // does not seem to work if done directly
                            if ( btState.equals(BLEState.CONNECTED_ALL) ) {
                                RequestBatteryLevelCountDown countDown = new RequestBatteryLevelCountDown(iNrOfDevices);
                                countDown.start();
                            }

                            if ( iNrOfDevices == 2 ) {
                                btnSwapDevices.setEnabled(true);
                            }
                            break;
                        case DISCONNECTED:
                            break;
                        case CONNECTING:
                            break;
                        case DISCONNECTED_Gatt:
                            // if one of the two devices disconnects
                            for(Object o: usedForPlayers.keySet() ) {
                                Player p = Player.valueOf(String.valueOf(o));
                                int iResId = m_playerToVerifyButtonResId.get(p);
                                vgVerify.findViewById(iResId).setEnabled(false);
                                iResId = m_playerToRenameResId.get(p);
                                TextView btn = vgVerify.findViewById(iResId);
                                btn.setEnabled(false);
                            }
                            btnSwapDevices.setEnabled(false);
                            btnVerifySingleDeviceForBoth.setEnabled(false);
                            btnRenameSingleDeviceForBoth.setEnabled(false);
                            break;
                        default:
                            Toast.makeText(m_context, sMsg, Toast.LENGTH_LONG).show();
                            break;
                    }
                }
                break;
            }
        }
    }

    private static final long lDurationRequestingBatteryLevel = 3000;
    private class RequestBatteryLevelCountDown extends CountDownTimer {
        int iNrOfDevices;
        int iTicks;
        RequestBatteryLevelCountDown(int iNrOfDevices) {
            super(lDurationRequestingBatteryLevel, lDurationRequestingBatteryLevel / 3);
            this.iNrOfDevices = iNrOfDevices;
            this.iTicks = 0;
        }
        @Override public void onTick(long millisUntilFinished) {
            iTicks++;
            if ( iTicks == 1 ) {
                pbVerify.setVisibility(View.VISIBLE);
                return;
            } // skip first tick... to fast
            if ( iNrOfDevices == 2 ) {
                m_bleReceiverManager.readBatteryLevel(Player.B);
            } else {
                m_bleReceiverManager.readBatteryLevel(null);
            }
        }

        @Override public void onFinish() {
            if ( iNrOfDevices == 2 ) {
                m_bleReceiverManager.readBatteryLevel(Player.A);
            }
            pbVerify.setVisibility(View.INVISIBLE);
        }
    }

    private boolean renameDeviceDialog(Player p, final JSONObject joRenameConfig) {
        String sCurrentName = m_bleReceiverManager.getDeviceName(p);
        String sFixedPrefix = joRenameConfig.optString(BLEUtil.Keys.FixedPrefix.toString());
        if ( StringUtil.isNotEmpty(sFixedPrefix) && sCurrentName.startsWith(sFixedPrefix) ) {
            sCurrentName = sCurrentName.substring(sFixedPrefix.length());
        }
        final EditText txtNewName = new EditText(this);
        txtNewName.setText(sCurrentName);
        int iMaxLength = joRenameConfig.optInt(BLEUtil.Keys.FreeTextMaxLength.toString(), -1);
        if ( iMaxLength > 0 ) {
            txtNewName.setFilters(new InputFilter[] { new InputFilter.LengthFilter(iMaxLength) });
        }
        AlertDialog.Builder ab = new MyDialogBuilder(this);
        ab.setIcon          (android.R.drawable.ic_menu_edit)
          .setTitle         (R.string.ble_rename_device)
          .setView(txtNewName)
          .setNegativeButton(R.string.cmd_cancel, null)
          .setPositiveButton(R.string.cmd_rename, (dialog, which) -> renameDevice(p, txtNewName, joRenameConfig))
          .show();
        return true;
    }
    private void renameDevice(Player p, EditText txtNewName, final JSONObject joRenameConfig) {
        Editable text = txtNewName.getText();
        if ( text == null ) { return; }
        String sNewName = text.toString();
        if ( StringUtil.isEmpty(sNewName) ) { return; }

        String sCleanOutCharactersRegExp = joRenameConfig.optString(BLEUtil.Keys.CleanOutCharactersRegExp.toString());
        if ( StringUtil.isNotEmpty(sCleanOutCharactersRegExp) ) {
            String sNewCleanedName = sNewName.replaceAll(sCleanOutCharactersRegExp, "");
            if ( sNewCleanedName.equals(sNewName) == false ) {
                Toast.makeText(this, "removed some disallowed characters before rename", Toast.LENGTH_SHORT).show();
                sNewName = sNewCleanedName;
            }
        }
        m_bleReceiverManager.writeToBLE(p, BLEUtil.Keys.RenameConfig, sNewName);
    }
}
