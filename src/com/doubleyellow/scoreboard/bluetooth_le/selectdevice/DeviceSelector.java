package com.doubleyellow.scoreboard.bluetooth_le.selectdevice;

import android.content.Context;
import android.view.View;

import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.R;

public class DeviceSelector implements View.OnClickListener
{
    private String sDevice1 = null;
    private String sDevice2 = null;
    private Context context = null;
    DeviceSelector(String s1, String s2, Context context) {
        this.sDevice1 = s1;
        this.sDevice2 = s2;
        this.context = context;
    }

    void adjustEnableOrVisibility(View v) {
        String sAddress = (String) v.getTag();
        adjustEnableOrVisibility(v, sAddress, sDevice1, sDevice2);
    }

    private void adjustEnableOrVisibility(View v, String sAddress, String sDevice1, String sDevice2) {
        int visibility = (
                          ( sAddress.equalsIgnoreCase(sDevice1) && (v.getId() == R.id.use_as_device_a ) )
                       || ( sAddress.equalsIgnoreCase(sDevice2) && (v.getId() == R.id.use_as_device_b) )
                         ) ? View.INVISIBLE : View.VISIBLE;
        v.setVisibility(visibility);
    }
    
    @Override public void onClick(View v) {
        String sAddress = (String) v.getTag();
        if ( v.getId() == R.id.use_as_device_a ) {
            sDevice1 = sAddress;
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral1, context, sDevice1);
        } else if ( v.getId() == R.id.use_as_device_b ) {
            sDevice2 = sAddress;
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral2, context, sDevice2);
        }
        adjustEnableOrVisibility(v);
    }
}
