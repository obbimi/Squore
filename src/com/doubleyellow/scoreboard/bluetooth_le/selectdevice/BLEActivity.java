package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.doubleyellow.scoreboard.R;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleyellow.scoreboard.activity.XActivity;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.StringUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BLEActivity extends XActivity
{
    BluetoothAdapter   bluetoothAdapter = null;
    BluetoothLeScanner bleScanner = null;

    private final List<ScanResult>  scanResults       = new ArrayList<>();
    private final ScanResultAdapter scanResultAdapter = new ScanResultAdapter(scanResults);

    boolean isScanning = false;
    private Pattern pMustMatch = null;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        setContentView(R.layout.ble_activity);

        Button button = findViewById(R.id.ble_scan_button);
        button.setOnClickListener(v -> {
            if (isScanning) {
                isScanning = false;
                bleScanner.stopScan(scanCallback);
                //stopBleScan();
            } else {
                isScanning = true;
                if ( scanResults.size() > 0 ) {
                    scanResults.clear();
                    scanResultAdapter.notifyDataSetChanged();
                }
                bleScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
            }
            ((TextView)v).setText(isScanning?"Stop Scan":"Start Scan");
        });

        RecyclerView recyclerView = findViewById(R.id.scan_results_recycler_view);
        recyclerView.setAdapter(scanResultAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // why?
        recyclerView.setNestedScrollingEnabled(false);

        String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, this);
        String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, this);
        DeviceSelector listener = new DeviceSelector(sBluetoothLEDevice1, sBluetoothLEDevice2, this);
        scanResultAdapter.setListener(listener);

        JSONObject config = BLEUtil.getActiveConfig(this);
        if ( config != null ) {
            String sRegExp = config.optString(BLEUtil.device_name_regexp);
            if ( StringUtil.isNotEmpty(sRegExp) ) {
                try {
                    pMustMatch = Pattern.compile(sRegExp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        bleScanner.stopScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback()
    {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            if ( pMustMatch != null ) {
                String name = result.getDevice().getName();
                if ( pMustMatch.matcher(String.valueOf(name)).matches() == false ) {
                    //Log.d(TAG, String.format("Skipping %s based on regexp %s", name, pMustMatch.pattern()));
                    return;
                }
            }

            int indexQuery = -1;
            for(int i=0; i< scanResults.size();i++) {
                if (scanResults.get(i).getDevice().getAddress().equalsIgnoreCase(result.getDevice().getAddress()) ) {
                    indexQuery = i;
                }
            }
            if (indexQuery != -1) {
                // A scan result already exists with the same address
                scanResults.set(indexQuery, result);
                scanResultAdapter.notifyItemChanged(indexQuery);
            } else {
                scanResults.add(result);
                scanResultAdapter.notifyItemInserted(scanResults.size() - 1);
                //scanResultAdapter.notifyDataSetChanged();
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
