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
import android.bluetooth.le.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.bluetooth_le.BLEUtil;
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
    private List<ScanResult> items;
    private double           fRssiValueAt1M;

    public ScanResultAdapter(List<ScanResult> items, double fRssiValueAt1M, Map<Player,String> mSelectedSeenDevices, Map<Player, String> mSelectedPrefDevices) {
        this.items = items;
        this.mSelectedSeenDevices = mSelectedSeenDevices;
        this.mSelectedPrefDevices = mSelectedPrefDevices;
        this.fRssiValueAt1M       = fRssiValueAt1M;
    }

    private final boolean bUseEnableDisable = true;

    private boolean adjustEnableOrVisibility(View v, String sAddress) {
        String sDevice1 = mSelectedPrefDevices.get(Player.A);
        String sDevice2 = mSelectedPrefDevices.get(Player.B);
        if ( mSelectedSeenDevices.size() == mSelectedPrefDevices.size() ) {
            sDevice1 = mSelectedSeenDevices.get(Player.A);
            sDevice2 = mSelectedSeenDevices.get(Player.B);
        }
        int visibility = ( ( sAddress.equalsIgnoreCase(sDevice1) && (v.getId() == R.id.use_as_device_a ) )
                        || ( sAddress.equalsIgnoreCase(sDevice2) && (v.getId() == R.id.use_as_device_b) )
                         ) ? View.INVISIBLE
                           : View.VISIBLE;
        if ( bUseEnableDisable ) {
            v.setEnabled(visibility == View.VISIBLE);
        } else {
            v.setVisibility(visibility);
        }
        return visibility==View.INVISIBLE;
    }

    @Override public void onClick(View v) {
        String sAddress = (String) v.getTag();
        if ( v.getId() == R.id.use_as_device_a ) {
            mSelectedSeenDevices.put(Player.A, sAddress);
        } else if ( v.getId() == R.id.use_as_device_b ) {
            mSelectedSeenDevices.put(Player.B, sAddress);
        }

        Log.i(TAG, "Marked device " + sAddress + "  : " + mSelectedSeenDevices);
        if ( bUseEnableDisable ) {
            v.setEnabled(false);
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
        Button[]  btnUseAsX = new Button[Player.values().length];
        ViewHolder(View view) {
            super(view);
            tvName    = view.findViewById(R.id.device_name);
            tvAddr    = view.findViewById(R.id.mac_address);
            tvSgnl    = view.findViewById(R.id.signal_strength);
            tvDist    = view.findViewById(R.id.device_distance);
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
            
            tvName.setText(name);
            tvAddr.setText(address);
            tvSgnl.setText(result.getRssi() + " dBm");

            float fEnvironmentalValue = 3.1f;   // (between 2 and 4?)
            double fDistance = Math.pow(10, (fRssiValueAt1M - result.getRssi())/(10 * fEnvironmentalValue));
            tvDist.setText(String.format("%.2f m", fDistance));

            for( Player p : Player.values() ) {
                Button btn = btnUseAsX[p.ordinal()];
                if ( btn == null ) { continue; }
                btn.setTag(address);
                btn.setOnClickListener(ScanResultAdapter.this);
                if ( adjustEnableOrVisibility(btn, address) ) {
                    mSelectedSeenDevices.put(p, address);
                    Log.i(TAG, "Marked " + p + " as pref selected device " + address + "  : " + mSelectedSeenDevices);
                }
            }
        }
    }

    @NonNull
    @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.ble_scan_result, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult item = items.get(position);
        holder.bind(item);
    }

    @Override public int getItemCount() {
        return items.size();
    }
}
