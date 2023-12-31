package com.doubleyellow.scoreboard.bluetooth_le;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BLEUtil
{
    private final static String TAG = "SB.BLEUtil";

    /** only to get BLE services/characteristics info about a device in nicely readable format */
    public static void printGattTable(BluetoothGatt gatt) {
        StringBuilder sb = new StringBuilder();
        sb.append(gatt.getDevice().getName()).append(" ").append(gatt.getDevice().getAddress()).append("\n");
        if ( gatt.getServices().isEmpty() ) {
            Log.d("BluetoothGatt","No service and characteristic available, call discoverServices() first?");
            return;
        }
        for(BluetoothGattService service: gatt.getServices()) {
            sb.append("Service " ).append(service.getUuid()).append("\n");
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic c: characteristics) {
                sb.append("|-- " ).append(c.getUuid());
                int p = c.getProperties();
                if ((p & BluetoothGattCharacteristic.PROPERTY_READ             ) != 0) sb.append(" READABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE            ) != 0) sb.append(" WRITABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) sb.append(" WRITABLE WITHOUT RESPONSE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_INDICATE         ) != 0) sb.append(" INDICATABLE");
                if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY           ) != 0) sb.append(" NOTIFIABLE");
                if (p == 0) sb.append(" EMPTY");
                sb.append("\n");

                List<BluetoothGattDescriptor> descriptors = c.getDescriptors();
                for(BluetoothGattDescriptor d: descriptors) {
                    sb.append("|---- " ).append(d.getUuid());
                    int p2 = d.getPermissions();
                    if ((p2 & BluetoothGattDescriptor.PERMISSION_READ ) != 0) sb.append(" READABLE");
                    if ((p2 & BluetoothGattDescriptor.PERMISSION_WRITE) != 0) sb.append(" WRITABLE");
                    if (p2 == 0) sb.append(" EMPTY");
                    sb.append("\n");
                }
            }
        }
        Log.d(TAG, sb.toString());
    }

    /** transforms json like in bluetooth_le_config to easier iterable format */
    public static Map<String, List<String>> getServiceUUID2CharUUID(JSONObject mServicesAndCharacteristicsConfig) {
        Map<String, List<String>> mReturn = new HashMap<>();
        Iterator<String> itServiceUUID = mServicesAndCharacteristicsConfig.keys();
        while(itServiceUUID.hasNext() ) {
            String sServiceUUID = itServiceUUID.next();
            try {
                UUID.fromString(sServiceUUID); // to only process valid UUID's
                List<String> lCharUUID = new ArrayList<>();
                JSONObject mCharacteristicUUID = mServicesAndCharacteristicsConfig.getJSONObject(sServiceUUID);
                Iterator<String> itCharUUID = mCharacteristicUUID.keys();
                while ( itCharUUID.hasNext() ) {
                    String sCharacteristicUUID = itCharUUID.next();
                    lCharUUID.add(sCharacteristicUUID);
                }
                mReturn.put(sServiceUUID, lCharUUID);
            } catch (Exception e) {
                Log.w(TAG, "Skipping " + sServiceUUID);
            }
        }
        return mReturn;
    }
}
