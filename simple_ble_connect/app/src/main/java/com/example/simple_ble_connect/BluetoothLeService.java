package com.example.simple_ble_connect;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BluetoothLeService extends Service {
    private static String Tag="communicationBluetooth";
    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt bleGatt;
    private  int my_ConnectionsState = BluetoothProfile.STATE_DISCONNECTED;
    private byte[] sendValue;//儲存要送出的資訊
    private static final int STATE_DISCONNECTED = 0;//設備無法連接
    private static final int STATE_CONNECTING = 1;//設備正在連接
    private static final int STATE_CONNECTED = 2;//設備連接完畢

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";//已連接到GATT服務器
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";//未連接GATT服務器
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";//未發現GATT服務
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";//接收到來自設備的數據，可通過讀取或操作獲得
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"; //其他數據
    private boolean lockCharacteristicRead = false;//由於送執會觸發onCharacteristicRead並造成干擾，故做一個互鎖
    private final IBinder mBinder =new LocalBinder();

    public ArrayList<String> getPropertiesTagArray(int properties) {
        int addPro = properties;
        ArrayList<String> arrayList = new ArrayList<>();
        int[] bluetoothGattCharacteristicCodes = new int[]{
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_BROADCAST
        };
        String[] bluetoothGattCharacteristicName = new String[]{
                "EXTENDED_PROPS",
                "SIGNED_WRITE",
                "INDICATE",
                "NOTIFY",
                "WRITE",
                "WRITE_NO_RESPONSE",
                "READ",
                "BROADCAST"
        };
        for (int i = 0; i < bluetoothGattCharacteristicCodes.length; i++) {
            int code = bluetoothGattCharacteristicCodes[i];
            if (addPro >= code) {
                addPro -= code;
                arrayList.add(bluetoothGattCharacteristicName[i]);
            }
        }
        return arrayList;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bleGatt == null) return null;
        return bleGatt.getServices();
    }


    public class LocalBinder extends Binder{
        public  BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public boolean onUnbind(Intent intent){
        close();
        return super.onUnbind(intent);
    }
    public boolean initialize(){
        if(bleManager == null){
            bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(bleManager == null){
                return false;
            }
        }
        bleAdapter = bleManager.getAdapter();
        if(bleAdapter == null){
            return false;
        }
        return true;
    }
    public boolean connect(String address) {
        if (bleAdapter == null || address == null) {
            return false;
        }
        if (address.equals(mBluetoothDeviceAddress) && bleGatt != null) {
            if (bleGatt.connect()) {
                my_ConnectionsState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        BluetoothDevice device = bleAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }
        bleGatt = device.connectGatt(this, false, mGattCallback);
        Log.i("bleGatt","status"+bleGatt.getDevice().getAddress());
        mBluetoothDeviceAddress = address;
        my_ConnectionsState = STATE_CONNECTING;
        return true;
    }
    public void disconnect() {
        if (bleAdapter == null || bleGatt == null) {
            return;
        }
        bleGatt.disconnect();
    }

    public void close(){
        if(bleGatt == null){
            return;
        }
        bleGatt.close();
        bleGatt = null;
    }
    public BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("callback",newState+""+status);
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                my_ConnectionsState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(Tag, "Connected to GATT server.");
                Log.i(Tag, "Attempting to start service discovery:" +
                        bleGatt.discoverServices());
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                my_ConnectionsState = STATE_DISCONNECTED;
                Log.i(Tag,"Disconnected from GATT Server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.i(Tag,"onServicesDiscovered received: "+status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status== BluetoothGatt.GATT_SUCCESS){
                if(!lockCharacteristicRead){
                    broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
                }
                lockCharacteristicRead = false;
                Log.d(Tag,"onCharacteristicRead: "+ascii2String(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
            if (bleAdapter == null || bleGatt == null) {
                Log.w(Tag, "BluetoothAdapter not initialized");
                return;
            }
            lockCharacteristicRead = true;
            bleGatt.readCharacteristic(characteristic);
            String record = characteristic.getStringValue(0);
            byte[] a = characteristic.getValue();
            Log.d(Tag, "readCharacteristic:回傳 " + record);
            Log.d(Tag, "readCharacteristic: 回傳byte[] " + byteArrayToHexStr(a));

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(Tag,"送出資: Byte: "+byteArrayToHexStr(sendValue)
                    +",String: "+ascii2String(sendValue));
            BluetoothGattCharacteristic Rxchar = descriptor.getCharacteristic();
            bleGatt.writeCharacteristic(Rxchar);
        }
    };
    public void broadcastUpdate(String action){
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private  void broadcastUpdate(String action,BluetoothGattCharacteristic characteristic){
        Intent intent = new Intent(action);
        final  byte[] data = characteristic.getValue();
        if(data != null && data.length>0){
            StringBuilder stringBuilder = new StringBuilder((data.length));
            for(byte bytechar : data)
                stringBuilder.append(String.format("%02X",bytechar));
            intent.putExtra(EXTRA_DATA,characteristic.getValue());
        }
        sendBroadcast(intent);
    }
    /**
     * 將byte[] ASCII 轉為字串的方法
     */
    public static String ascii2String(byte[] in) {
        final StringBuilder stringBuilder = new StringBuilder(in.length);
        for (byte byteChar : in)
            stringBuilder.append(String.format("%02X ", byteChar));
        String output = null;
        try {
            output = new String(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return output;
    }
    /**
     * Byte轉16進字串工具
     */
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        return gethex;

    }
}
