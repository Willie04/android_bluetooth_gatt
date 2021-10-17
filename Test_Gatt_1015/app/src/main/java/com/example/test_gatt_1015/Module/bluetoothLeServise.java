package com.example.test_gatt_1015.Module;

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

public class bluetoothLeServise extends Service{
    private static String Tag="communicationBluetooth";
    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private String mybluetooth_macAddress;
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

    private final IBinder my_os_Binder =new LocalBinder();

    public class LocalBinder extends Binder{
         public bluetoothLeServise getService(){
             return bluetoothLeServise.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
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


    public boolean connect(String address){
        if(bleAdapter == null && address == null){
            return false;
        }
        if(address.equals(mybluetooth_macAddress) && bleGatt != null){
            if(bleGatt.connect()){
                my_ConnectionsState = STATE_CONNECTING;
                return  true;
            }else {
                return  false;
            }
        }
        BluetoothDevice device = bleAdapter.getRemoteDevice(address);
        if(device == null){
            return false;
        }
        bleGatt = device.connectGatt(this,false,mGattcallback);
        mybluetooth_macAddress = address;
        my_ConnectionsState = STATE_CONNECTING;
        return true;
    }
    /**斷開連線*/
    public void disconnect() {
        if (bleAdapter == null || bleGatt == null) {
            return;
        }
        bleGatt.disconnect();
    }

    public void close() {
        if (bleGatt == null) {
            return;
        }
        bleGatt.close();
        bleGatt = null;
    }

    /**送字串模組*/
    public boolean sendValue(String value, BluetoothGattCharacteristic characteristic) {
        try {

            this.sendValue = value.getBytes();
            setCharacteristicNotification(characteristic, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**送byte[]模組*/
    public boolean sendValue(byte[] value,BluetoothGattCharacteristic characteristic){
        try{
            this.sendValue = value;
            setCharacteristicNotification(characteristic, true);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bleAdapter == null || bleGatt == null) {
            return;
        }
        if (characteristic != null) {
            for (BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                if (enabled) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    dp.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                /**送出
                 * @see onDescriptorWrite()*/
                bleGatt.writeDescriptor(dp);
            }

            bleGatt.setCharacteristicNotification(characteristic, true);
            bleGatt.readCharacteristic(characteristic);
        }

    }

    /**初始化藍芽*/
    public boolean initialize() {
        if (bleManager == null) {
            bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bleManager == null) {
                return false;
            }
        }

        bleAdapter = bleManager.getAdapter();
        if (bleAdapter == null) {
            return false;
        }

        return true;
    }
    /**將搜尋到的服務傳出*/
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bleGatt == null) return null;
        return bleGatt.getServices();
    }
    private BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        /**當連接狀態發生改變*/
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {//當設備已連接
                intentAction = ACTION_GATT_CONNECTED;
                my_ConnectionsState = STATE_CONNECTED;
                broadcastUpdate(intentAction); //更新action
                Log.i(Tag, "Connected to GATT server.");
                Log.i(Tag, "Attempting to start service discovery:" +
                        bleGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//當設備無法連接
                intentAction = ACTION_GATT_DISCONNECTED;
                my_ConnectionsState = STATE_DISCONNECTED;
                Log.i(Tag, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);

            }
        }

        /**當發現新的服務器*/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(Tag, "onServicesDiscovered received: " + status);
            }
        }
        /**Descriptor寫出資訊給藍芽*/
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(Tag, "送出資訊: Byte: " + byteArrayToHexStr(sendValue)
                    + ", String: " + ascii2String(sendValue));
            BluetoothGattCharacteristic RxChar = descriptor.getCharacteristic();
            RxChar.setValue(sendValue);
            bleGatt.writeCharacteristic(RxChar);
        }
        /**讀取屬性(像是DeviceName、System ID等等)*/
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!lockCharacteristicRead){
                    broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);

                }
                lockCharacteristicRead = false;
                Log.d(Tag, "onCharacteristicRead: "+ascii2String(characteristic.getValue()));
            }
        }

        /**如果特性有變更(就是指藍芽有傳值過來)*/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

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
    };
    private void  broadcastUpdate(String action){
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(String action,BluetoothGattCharacteristic characteristic){
        Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();
        if(data != null && data.length > 0){
            StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte showbyte : data)
                stringBuilder.append(String.format("%02X",showbyte));
            intent.putExtra(EXTRA_DATA,characteristic.getValue());
        }
        sendBroadcast(intent);
    }
}
