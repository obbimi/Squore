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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> implements View.OnClickListener
{
    private static final String TAG = "SB." + ScanResultAdapter.class.getSimpleName();

    private final Map<Player, String> mSelectedSeenDevices;
    private final Map<Player, String> mSelectedPrefDevices;
    private final List<ScanResult>    items;
    private final double              fRssiValueAt1M;
    private final int                 iManufacturerData_BatteryLevelAtPos;

    public ScanResultAdapter(List<ScanResult> items, double fRssiValueAt1M, int iManufacturerData_BatteryLevelAtPos, Map<Player,String> mSelectedSeenDevices, Map<Player, String> mSelectedPrefDevices) {
        this.items                = items;
        this.mSelectedSeenDevices = mSelectedSeenDevices;
        this.mSelectedPrefDevices = mSelectedPrefDevices;
        this.fRssiValueAt1M       = fRssiValueAt1M;
        this.iManufacturerData_BatteryLevelAtPos = iManufacturerData_BatteryLevelAtPos;
    }

    private boolean adjustEnableOrVisibility(View v, String sAddress) {
        String sDevice1 = mSelectedPrefDevices.get(Player.A);
        String sDevice2 = mSelectedPrefDevices.get(Player.B);
        if ( mSelectedSeenDevices.containsKey(Player.A) ) {
            sDevice1 = mSelectedSeenDevices.get(Player.A);
        }
        if ( mSelectedSeenDevices.containsKey(Player.B) ) {
            sDevice2 = mSelectedSeenDevices.get(Player.B);
        }
        boolean bIsSelectedForUsage = (sAddress.equalsIgnoreCase(sDevice1) && (v.getId() == R.id.use_as_device_a))
                                   || (sAddress.equalsIgnoreCase(sDevice2) && (v.getId() == R.id.use_as_device_b));
        if ( v instanceof CheckBox ) {
            CheckBox cb = (CheckBox) v;
            if ( cb.isChecked() != bIsSelectedForUsage ) {
                cb.setChecked(bIsSelectedForUsage);
                //Log.d(TAG, "Adjusting checked for " + sAddress  + " " + bIsSelectedForUsage);
            }
        } else {
            int visibility = bIsSelectedForUsage ? View.INVISIBLE : View.VISIBLE;
            v.setVisibility(visibility);
        }
        return bIsSelectedForUsage;
    }

    @Override public void onClick(View v) {
        String sAddress = (String) v.getTag();
        if ( v.getId() == R.id.use_as_device_a ) {
            mSelectedSeenDevices.put(Player.A, sAddress);
        } else if ( v.getId() == R.id.use_as_device_b ) {
            mSelectedSeenDevices.put(Player.B, sAddress);
        }

        Log.i(TAG, "Marked device " + sAddress + "  : " + mSelectedSeenDevices);
        if ( v instanceof CheckBox ) {
            CheckBox cb = (CheckBox) v;
            cb.setChecked(true);
        } else {
            v.setVisibility(View.INVISIBLE);
        }

        for(int i=0; i < this.getItemCount(); i++) {
            this.notifyItemChanged(i);
        }
        //this.notifyDataSetChanged(); // does this redraw ALL entries, so that other A,B buttons get visible/invisible
    }

    /**
     * View representing a selectable BLE peripheral
     */
    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView tvName;
        TextView tvAddr;
        TextView tvSgnl;
        TextView tvDist;
        TextView tvBatt;
        Button[]  btnUseAsX = new Button[Player.values().length];
        ViewHolder(View view) {
            super(view);
            tvName    = view.findViewById(R.id.device_name);
            tvAddr    = view.findViewById(R.id.mac_address);
            tvSgnl    = view.findViewById(R.id.signal_strength);
            tvDist    = view.findViewById(R.id.device_distance);
            tvBatt    = view.findViewById(R.id.device_batterylevel);
            btnUseAsX[Player.A.ordinal()] = view.findViewById(R.id.use_as_device_a);
            btnUseAsX[Player.B.ordinal()] = view.findViewById(R.id.use_as_device_b);
        }
        private void bind(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if ( name == null ) {
                name = "Unnamed";
            }
            String address = device.getAddress();
            ScanRecord scanRecord = result.getScanRecord();
            int iBatteryLevel = -1;
            if ( scanRecord != null ) {
                byte[] ba = scanRecord.getManufacturerSpecificData(iManufacturerData_BatteryLevelAtPos);
                if ( ba != null ) {
                    iBatteryLevel = ba[0] & 0xFF;
                    Log.i(TAG, "battery manufacturerSpecificData[" + iManufacturerData_BatteryLevelAtPos + "]:" + iBatteryLevel);
                }
                if ( iBatteryLevel == - 1) {
                    SparseArray<byte[]> manufacturerSpecificData = scanRecord.getManufacturerSpecificData();
                    if (manufacturerSpecificData != null ) {
                        for(int i=0; i < manufacturerSpecificData.size(); i++) {
                            int    iKey = manufacturerSpecificData.keyAt(i); // 767 ?
                            byte[] bytes = manufacturerSpecificData.valueAt(i);
                            if ( bytes == null ) { continue; }
                            iBatteryLevel = bytes[0] & 0xFF;
                            Log.i(TAG, "manufacturerSpecificData[" + i + "=" + iKey + "]:" + iBatteryLevel);
                        }
                    }
                }
            }

            tvName.setText(name);
            tvAddr.setText(address);
            tvSgnl.setText(result.getRssi() + " dBm");

            float fEnvironmentalValue = 3.1f;   // (between 2 and 4?)
            double fDistance = Math.pow(10, (fRssiValueAt1M - result.getRssi())/(10 * fEnvironmentalValue));
            tvDist.setText(String.format("%.2f m", fDistance));

            if ( iBatteryLevel > 0 ) {
                tvBatt.setText(String.format("B:%d%%", iBatteryLevel));
                tvBatt.setVisibility(View.VISIBLE);
                tvSgnl.setVisibility(View.GONE);
            } else {
                tvSgnl.setVisibility(View.VISIBLE);
                tvBatt.setVisibility(View.GONE);
            }

            for( Player p : Player.values() ) {
                Button btn = btnUseAsX[p.ordinal()];
                if ( btn == null ) { continue; }
                btn.setTag(address);
                btn.setOnClickListener(ScanResultAdapter.this);
                if ( adjustEnableOrVisibility(btn, address) ) {
                    mSelectedSeenDevices.put(p, address);
                    //Log.d(TAG, "Marked " + p + " as pref selected device " + address + "  : " + mSelectedSeenDevices);
                }
            }
        }
    }

    @NonNull
    @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.ble_scan_result, parent, false);
        //Log.d(TAG, "New viewholder ...");
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult item = items.get(position);
        //Log.d(TAG, "Binding " + position);
        holder.bind(item);
    }

    @Override public int getItemCount() {
        return items.size();
    }
}
