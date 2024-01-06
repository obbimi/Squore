package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.R;

import java.util.Map;

/**
 * View representing a selectable BLE peripheral
 */
public class DeviceSelector implements View.OnClickListener
{
    private static final String TAG = "SB." + DeviceSelector.class.getSimpleName();

    private final Map<Player,String> mSelectedSeenDevices;
    DeviceSelector(Map<Player,String> mSelectedSeenDevices, Context context) {
        this.mSelectedSeenDevices = mSelectedSeenDevices;
    }

    boolean adjustEnableOrVisibility(View v, String sAddress, Map<Player, String> mSelectedPrefDevices) {
        String sDevice1 = mSelectedPrefDevices.get(Player.A);
        String sDevice2 = mSelectedPrefDevices.get(Player.B);
        int visibility = (
                          ( sAddress.equalsIgnoreCase(sDevice1) && (v.getId() == R.id.use_as_device_a ) )
                       || ( sAddress.equalsIgnoreCase(sDevice2) && (v.getId() == R.id.use_as_device_b) )
                         ) ? View.INVISIBLE : View.VISIBLE;
        v.setVisibility(visibility);
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
        v.setVisibility(View.INVISIBLE);
    }
}
