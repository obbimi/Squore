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
import com.doubleyellow.scoreboard.model.Player;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder>
{
    private static final String TAG = "SB." + ScanResultAdapter.class.getSimpleName();

    private final Map<Player,String> mSelectedSeenDevices;
    private final Map<Player, String> mSelectedPrefDevices;
    private List<ScanResult> items = null;

    public ScanResultAdapter(List<ScanResult> items, Map<Player,String> mSelectedSeenDevices, Map<Player, String> mSelectedPrefDevices) {
        this.items = items;
        this.mSelectedSeenDevices = mSelectedSeenDevices;
        this.mSelectedPrefDevices = mSelectedPrefDevices;
    }

    private DeviceSelector onClickListener = null;
    public void setListener(DeviceSelector listener) {
        this.onClickListener = listener;
    }
    static List<String> lDetectedAndConfiguredDevices = new ArrayList<>();

    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView tvName;
        TextView tvAddr;
        TextView tvSgnl;
        Button   btnUseAsA = null;
        Button   btnUseAsB = null;
        ViewHolder(View view) {
            super(view);
            tvName    = view.findViewById(R.id.device_name);
            tvAddr    = view.findViewById(R.id.mac_address);
            tvSgnl    = view.findViewById(R.id.signal_strength);
            btnUseAsA = view.findViewById(R.id.use_as_device_a);
            btnUseAsB = view.findViewById(R.id.use_as_device_b);
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

            if ( onClickListener != null && btnUseAsA != null && btnUseAsB != null ) {
                btnUseAsA.setTag(address);
                btnUseAsA.setOnClickListener(onClickListener);
                if ( onClickListener.adjustEnableOrVisibility(btnUseAsA, address, mSelectedPrefDevices) ) {
                    mSelectedSeenDevices.put(Player.A, address);
                    Log.i(TAG, "Marked A as prefselected device " + address + "  : " + mSelectedSeenDevices);
                }

                btnUseAsB.setTag(address);
                btnUseAsB.setOnClickListener(onClickListener);
                if ( onClickListener.adjustEnableOrVisibility(btnUseAsB, address, mSelectedPrefDevices) ) {
                    mSelectedSeenDevices.put(Player.B, address);
                    Log.i(TAG, "Marked B as prefselected device " + address + "  : " + mSelectedSeenDevices);
                }
            }
        }
    }

    @NonNull
    @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ble_scan_result, parent, false);
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
