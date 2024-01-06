package com.doubleyellow.scoreboard.bluetooth_le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.doubleyellow.scoreboard.bluetooth.BTMessage;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads and interprets message from BLE device.
 * TODO: 2 modes
 * - amateur: every correct message is a change in score
 * - pro    : a correct message from one BLE is request in change of score for requester, a confirmation message from different BLE devices is needed to actually change the score
 */
public class BLEReceiverManager
{
    private final static String TAG = "SB.BLEReceiverManager";

    private final String CCCD_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"; // Client Characteristic Configuration

    private final BluetoothLeScanner bluetoothLeScanner;
    private       BLEHandler         mHandler;
    private final Map<String, Long>  mDevicesUsed       = new HashMap<>();
    private final Map<String, Long>  mDevicesSeen       = new HashMap<>();

    private final Context context;

    private       List<String> saDeviceAddresses         = new ArrayList<>();
    private final JSONObject   mServicesAndCharacteristicsConfig;

    public BLEReceiverManager( Context context, BluetoothAdapter bluetoothAdapter
                             , String sBluetoothLEDeviceA, String sBluetoothLEDeviceB
                             , JSONObject mServicesAndCharacteristicsConfig)
    {
        this.context              = context;
        this.bluetoothLeScanner   = bluetoothAdapter.getBluetoothLeScanner();

        this.saDeviceAddresses.add(sBluetoothLEDeviceA);
        this.saDeviceAddresses.add(sBluetoothLEDeviceB);
        saDeviceAddresses = ListUtil.removeDuplicates(saDeviceAddresses);
        ListUtil.removeEmpty(this.saDeviceAddresses);

        this.mServicesAndCharacteristicsConfig = mServicesAndCharacteristicsConfig;
    }

    /*
     * @param handler A Handler to send messages back to the UI Activity
     */
    public void setHandler(BLEHandler handler) {
        this.mHandler = handler;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if ( result.isConnectable() == false ) {
                    return;
                }
            }
            ParcelUuid[] uuids = result.getDevice().getUuids();
            if ( uuids != null ) {
                for(int i=0; i < uuids.length; i++) {
                    Log.i(TAG, "uuids[" + i + "]:" + uuids[i].toString());
                }
            }

            // does not seem to give much more info
            ScanRecord scanRecord = result.getScanRecord();
            if ( scanRecord != null ) {
                SparseArray<byte[]> manufacturerSpecificData = scanRecord.getManufacturerSpecificData();
                if (manufacturerSpecificData != null ) {
                    for(int i=0; i < manufacturerSpecificData.size(); i++) {
                        byte[] bytes = manufacturerSpecificData.get(i);
                        if ( bytes == null ) { continue; }
                        String s = new String(bytes);
                        Log.i(TAG, "manufacturerSpecificData[" + i + "]:" + s);
                    }
                }
            }

            BluetoothDevice btDevice = result.getDevice();
            String devName = btDevice.getName();
            if (StringUtil.isEmpty(devName) ) {
                devName = btDevice.getAddress();
            }

            long lNow = System.currentTimeMillis();
            Long lSeen = mDevicesSeen.get(devName);
            if ( lSeen == null || lNow - lSeen > 20 * 1000L ) {
                Log.d(TAG, String.format("Scan is seeing %s ( address : %s)", devName, btDevice.getAddress()));
                mDevicesSeen.put(devName, lNow);
            }

            // connectGatt if it is matches one of the 2 device names/mac addresses
            for ( int i=0; i < saDeviceAddresses.size(); i++ ) {
                String sLookingFor = saDeviceAddresses.get(i);
                if( sLookingFor.equalsIgnoreCase(devName) || sLookingFor.equalsIgnoreCase(btDevice.getAddress()) ) {
                    if ( mDevicesUsed.containsKey(sLookingFor) ) { continue; }

                    Log.i(TAG, "Connecting to device " + devName);
                    Message message = mHandler.obtainMessage(BTMessage.STATE_CHANGE.ordinal(), "Connected " + sLookingFor);
                    message.sendToTarget();
                    btDevice.connectGatt(context,false, new MyBluetoothGattCallback(sLookingFor, Player.values()[i]));
                    mDevicesUsed.put(sLookingFor, System.currentTimeMillis());

                    if ( MapUtil.size(mDevicesUsed) == saDeviceAddresses.size() ) {
                        bluetoothLeScanner.stopScan(this);
                        Log.i(TAG, "Stopped scanning. All devices found = " + ListUtil.join(saDeviceAddresses, ","));
                    } else {
                        List<String> lOther = new ArrayList<>(saDeviceAddresses);
                        lOther.remove(sLookingFor);
                        Log.i(TAG, "Continue scanning for second device " + ListUtil.join(lOther, ","));
                        message = mHandler.obtainMessage(BTMessage.STATE_CHANGE.ordinal(), "Connected " + sLookingFor + ", looking for " + ListUtil.join(lOther, ","));
                        message.sendToTarget();
                    }
                }
            }
        }

        @Override public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(TAG, "onBatchScanResults : #"  + ListUtil.size(results));
        }
        @Override public void onScanFailed      (int errorCode) {
            super.onScanFailed(errorCode);
            Log.w(TAG, "onScanFailed : "  + errorCode); // e.g. when bluetooth is turnof on BLE peripheral emulator
        }
    };

    private final         Map<String, BluetoothGatt> mDevice2gatt = new HashMap<>();

    private class MyBluetoothGattCallback extends BluetoothGattCallback
    {
        private              int currentConnectionAttempt    = 1;
        private final static int MAXIMUM_CONNECTION_ATTEMPTS = 5;

        private final String sDevice;
        private final Player player ;
        MyBluetoothGattCallback(String sName, Player player) {
            this.sDevice = sName;
            this.player = player;
        }

        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if ( status == BluetoothGatt.GATT_SUCCESS ) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED: {
                        Log.i(TAG, "Discovering services ... " + this.sDevice);
                        Message message = mHandler.obtainMessage(BTMessage.STATE_CHANGE.ordinal(), "Discovering services " + this.sDevice);
                        message.sendToTarget();
                        gatt.discoverServices();
                        mDevice2gatt.put(this.sDevice, gatt); //this.gatt = gatt;
                    }
                    case BluetoothProfile.STATE_DISCONNECTED: {
                        Log.w(TAG, String.format("onConnectionStateChange new state %d (%s)", newState, this.sDevice));
                        if ( newState == BluetoothProfile.STATE_DISCONNECTED ) {
                            gatt.close();
                            mDevice2gatt.remove(this.sDevice);
                            if ( saDeviceAddresses.size() - MapUtil.size(mDevice2gatt) == 1 ) {
                                bluetoothLeScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
                                Log.w(TAG, "Restart scanning. Not all devices found. Devices still found = " + ListUtil.join(mDevice2gatt.keySet(), ","));
                            }
                        }
                    }
                    default: {
                        Log.w(TAG, String.format("onConnectionStateChange new state %d (%s)", newState, this.sDevice));
                    }
                }
            } else {
                Log.w(TAG, String.format("onConnectionStateChange status :%d (%s)", status, this.sDevice));
                mDevice2gatt.remove(this.sDevice);
                gatt.close();
                currentConnectionAttempt+=1;
                Log.i(TAG, String.format("Attempting to connect %d/%d", currentConnectionAttempt, MAXIMUM_CONNECTION_ATTEMPTS));
                if ( currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS ) {
                    startReceiving();
                } else {
                    Log.w(TAG, "Could not connect to ble device ... " + this.sDevice);
                    Message message = mHandler.obtainMessage(BTMessage.STATE_CHANGE.ordinal(), "Disconnected " + this.sDevice);
                    message.sendToTarget();
                }
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            BLEUtil.printGattTable(gatt);
            if ( false && sDevice.toLowerCase().startsWith("iho") == false ) {
                // for BLE emulator as peripheral this does not trigger onMtuChanged
                boolean bReq = gatt.requestMtu(517); // could remain default of 20
                Log.d(TAG, "requestMtu : " + bReq);
            } else {
                enableNotification(gatt);
            }
        }
        @Override public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            enableNotification(gatt);
        }
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            int    iValue  = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
            String sValue  = characteristic.getStringValue(0);
            byte[] baValue = characteristic.getValue();
            if ( false ) {
                Log.d(TAG, "iValue : " + iValue);
                Log.d(TAG, "sValue : " + sValue);
                for (int i = 0; i < baValue.length; i++) {
                    Log.d(TAG, "b[" + i + "] : " + baValue[i]);
                }
                Log.d(TAG, String.format("characteristic : %s, value : %s", characteristic.getUuid(), sValue));
            }
            String sMessage = sValue;
            try {
                JSONObject joChars = mServicesAndCharacteristicsConfig.optJSONObject(characteristic.getService().getUuid().toString().toLowerCase());
                if ( joChars != null ) {
                    Object oMessageFormat = joChars.get(characteristic.getUuid().toString().toLowerCase());
                    String sMessageFormat = String.valueOf(oMessageFormat);
                    if ( oMessageFormat instanceof JSONArray ) {
                        JSONArray jsonArray = (JSONArray) oMessageFormat;
                        sMessageFormat = jsonArray.getString(0);
                        if ( jsonArray.length() > 1 && saDeviceAddresses.size() == 2 ) {
                            sMessageFormat = jsonArray.getString(1);
                        }
                    }
                    sMessage = String.format(sMessageFormat, player.toString(), iValue, sValue);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Message message = mHandler.obtainMessage(BTMessage.READ.ordinal(), sMessage );
            message.sendToTarget();
        }
        @Override public void onCharacteristicChanged (@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
        }

        @Override public void onPhyUpdate             (         BluetoothGatt gatt, int txPhy, int rxPhy, int status) {super.onPhyUpdate(gatt, txPhy, rxPhy, status);}
        @Override public void onPhyRead               (         BluetoothGatt gatt, int txPhy, int rxPhy, int status) {super.onPhyRead(gatt, txPhy, rxPhy, status);}
        @Override public void onCharacteristicRead    (         BluetoothGatt gatt,          BluetoothGattCharacteristic characteristic, int status) {super.onCharacteristicRead(gatt, characteristic, status);}
        @Override public void onCharacteristicRead    (@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {super.onCharacteristicRead(gatt, characteristic, value, status);}
        @Override public void onCharacteristicWrite   (         BluetoothGatt gatt,          BluetoothGattCharacteristic characteristic, int status) {super.onCharacteristicWrite(gatt, characteristic, status);}
        @Override public void onDescriptorRead        (         BluetoothGatt gatt,          BluetoothGattDescriptor     descriptor    , int status) {super.onDescriptorRead(gatt, descriptor, status);}
        @Override public void onDescriptorRead        (@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor     descriptor    , int status, @NonNull byte[] value) {super.onDescriptorRead(gatt, descriptor, status, value);}
        @Override public void onDescriptorWrite       (         BluetoothGatt gatt,          BluetoothGattDescriptor     descriptor    , int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "onDescriptorWrite " + gatt.getDevice().getAddress() + " - " + descriptor.getUuid() + " : " + new String(descriptor.getValue()));
        }
        @Override public void onReliableWriteCompleted(         BluetoothGatt gatt, int status) {super.onReliableWriteCompleted(gatt, status);}
        @Override public void onReadRemoteRssi        (         BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i(TAG, "onReadRemoteRssi " + gatt.getDevice().getAddress());
        }
        @Override public void onServiceChanged        (@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
            Log.i(TAG, "onServiceChanged " + gatt.getDevice().getAddress());
        }

        private void enableNotification(BluetoothGatt gatt)
        {
            Map<String, List<String>> mServices2Characteristics = BLEUtil.getServiceUUID2CharUUID(mServicesAndCharacteristicsConfig);
            for(String sServiceUUID: mServices2Characteristics.keySet() ) {
                List<String> lCharacteristicUUID = mServices2Characteristics.get(sServiceUUID);
                if ( lCharacteristicUUID == null ) { continue; }
                for (String sCharacteristicUUID : lCharacteristicUUID) {
                    BluetoothGattCharacteristic characteristic = findCharacteristics(gatt, sServiceUUID, sCharacteristicUUID);
                    if ( characteristic == null ) {
                        String errorMessage = String.format("Could not find service %s with characteristic %s", sServiceUUID, sCharacteristicUUID);
                        Log.w(TAG, errorMessage);
                    } else {
                        if ( gatt.setCharacteristicNotification(characteristic, true) ) {
                            BluetoothGattDescriptor cccdDescriptor = characteristic.getDescriptor(UUID.fromString(CCCD_DESCRIPTOR_UUID));
                            if ( cccdDescriptor != null ) {
                                cccdDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(cccdDescriptor);
                            } else {
                                Log.w(TAG, String.format("Not specifically enabling %s - %s. No CCCD descriptor", sServiceUUID, sCharacteristicUUID));
                            }
                        }
                    }
                }
            }
        }
    }

    public void startReceiving() {
        //mDevicesUsed.clear();
        bluetoothLeScanner.startScan(null, BLEUtil.scanSettings, scanCallback);
    }

    public void reconnect() {
        if ( MapUtil.isEmpty(mDevice2gatt) ) { return; }
        for(BluetoothGatt gatt : mDevice2gatt.values() ) {
            gatt.connect();
        }
    }

    public void disconnect() {
        if (MapUtil.isEmpty(mDevice2gatt)) { return; }
        for(BluetoothGatt gatt : mDevice2gatt.values() ) {
            gatt.disconnect();
        }
    }

    public void closeConnection() {
        this.bluetoothLeScanner.stopScan(scanCallback);

        for(BluetoothGatt gatt : mDevice2gatt.values() ) {
            Map<String, List<String>> mServices2Characteristics = BLEUtil.getServiceUUID2CharUUID(mServicesAndCharacteristicsConfig);
            for(String sServiceUUID: mServices2Characteristics.keySet() ) {
                List<String> lCharacteristicUUID = mServices2Characteristics.get(sServiceUUID);
                if ( lCharacteristicUUID == null ) { continue; }

                for (String sCharacteristicUUID : lCharacteristicUUID) {
                    BluetoothGattCharacteristic characteristic = findCharacteristics(gatt, sServiceUUID, sCharacteristicUUID);
                    if ( characteristic != null ) {
                        disconnectCharacteristic(gatt, characteristic);
                    }
                }
            }
            gatt.close();
        }
    }

    private void disconnectCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic ){
        UUID cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID);
        if ( gatt.setCharacteristicNotification(characteristic,false) ) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
            if ( descriptor != null ) {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }
    }

    private BluetoothGattCharacteristic findCharacteristics(BluetoothGatt gatt, String serviceUUID,String characteristicsUUID){
        for(BluetoothGattService service: gatt.getServices()) {
            if ( service.getUuid().toString().equalsIgnoreCase(serviceUUID) ) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(BluetoothGattCharacteristic characteristic: characteristics) {
                    if ( characteristic.getUuid().toString().equalsIgnoreCase(characteristicsUUID)) {
                        return characteristic;
                    }
                }
            }
        }
        return null;
    }
}
