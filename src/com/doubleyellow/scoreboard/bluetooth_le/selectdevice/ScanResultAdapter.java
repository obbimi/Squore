package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.doubleyellow.scoreboard.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder>
{
    private List<ScanResult> items = null;
    public ScanResultAdapter(List<ScanResult> items) {
        this.items = items;
    }

    private DeviceSelector onClickListener = null;
    public void setListener(DeviceSelector listener) {
        this.onClickListener = listener;
    }

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
                btnUseAsA.setOnClickListener(onClickListener);
                btnUseAsA.setTag(address);
                onClickListener.adjustEnableOrVisibility(btnUseAsA);

                btnUseAsB.setTag(address);
                btnUseAsB.setOnClickListener(onClickListener);
                onClickListener.adjustEnableOrVisibility(btnUseAsB);
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
