package com.example.test_gatt_1015;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.test_gatt_1015.Module.RecyclerViewAdapter;
import com.example.test_gatt_1015.Module.ScannedData;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private String Tag = MainActivity.class.getSimpleName()+"my";
    BluetoothAdapter BleAdapter = BluetoothAdapter.getDefaultAdapter();
    private int REQUEST_FINE_LOCATION_PERMISSION = 100;

    private int REQEST_ENABLE_BT = 2;
    private  boolean isScanning = false;
    private boolean check_stautus=false;
    ArrayList<ScannedData> findDevice = new ArrayList<>();
    RecyclerViewAdapter mAdapter;

    Button scan_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scan_btn = (Button) findViewById(R.id.scan_btn);
        bluetooth_scanner();
    }

    private void checkPermission() {
        /**確認手機版本是否在API18以上，否則退出程式*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            /**確認是否已開啟取得手機位置功能以及權限*/
            int hasGone = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasGone != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            /**確認手機是否支援藍牙BLE*/
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this,"Not support Bluetooth", Toast.LENGTH_SHORT).show();
            }
            /**開啟藍芽適配器*/
            if(!BleAdapter.isEnabled()){
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent,REQEST_ENABLE_BT);
                Log.i("Tag","請求開幾bt");
            }
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                openGPSDialog();
            }

        }else finish();
    }

    private void bluetooth_scanner(){
        BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BleAdapter = bleManager.getAdapter();
        //開始掃描
        BleAdapter.stopLeScan(mLeScanCallback);
        isScanning=false;
        scan_btn.setText("開始掃描");
        //show recyleview
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.my_recyle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(mAdapter);

        Button scan_btn = (Button) findViewById(R.id.scan_btn);
        scan_btn.setOnClickListener((v)->{

                if (isScanning) {
                    /**關閉掃描*/
                    isScanning = false;
                    scan_btn.setText("開始掃描");
                    BleAdapter.stopLeScan(mLeScanCallback);
                }else{
                    checkPermission();
                    /**開啟掃描*/
                    isScanning = true;
                    scan_btn.setText("停止掃描");
                    findDevice.clear();
                    BleAdapter.startLeScan(mLeScanCallback);
                    //mAdapter.clearDevice();
                }
            });
        }
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            new Thread(()->{
            if(device.getName()!=null){
                findDevice.add(new ScannedData(device.getName(),
                        device.getAddress(),
                    byteArrayToHexStr(scanRecord),
                        String.valueOf(rssi)));
                ArrayList newList = getSingle(findDevice);
                runOnUiThread(()->{
                mAdapter.addDevice(newList);
                });
            }
            }).start();
        }
    };
    /**濾除重複的藍牙裝置(以Address判定)*/
    private ArrayList getSingle(ArrayList list) {
        ArrayList tempList = new ArrayList<>();
        try {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (!tempList.contains(obj)) {
                    tempList.add(obj);
                } else {
                    tempList.set(getIndex(tempList, obj), obj);
                }
            }
            return tempList;
        } catch (ConcurrentModificationException e) {
            return tempList;
        }
    }

    protected void onStart() {
        super.onStart();
        checkPermission();
        final Button btScan = findViewById(R.id.scan_btn);
        isScanning = false;
        btScan.setText("開始掃描");
        findDevice.clear();
        BleAdapter.stopLeScan(mLeScanCallback);
        mAdapter.clearDevice();
    }
    //**避免跳轉後掃描程序係續浪費效能，因此離開頁面後即停止掃描
    protected void onStop() {
        super.onStop();
        final Button btScan = findViewById(R.id.scan_btn);
        /**關閉掃描*/
        isScanning = false;
        btScan.setText("開始掃描");
        BleAdapter.stopLeScan(mLeScanCallback);
    }
        /**
         * 以Address篩選陣列->抓出該值在陣列的哪處
         */
    private int getIndex(ArrayList temp, Object obj) {
            for (int i = 0; i < temp.size(); i++) {
                if (temp.get(i).toString().contains(obj.toString())) {
                    return i;
                }
            }
            return -1;
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

    /**取得欲連線之裝置後跳轉頁面*/
    private RecyclerViewAdapter.OnItemClick itemClick = new RecyclerViewAdapter.OnItemClick() {
        @Override
        public void onItemClick(ScannedData selectedDevice) {
            Log.i("Tag","give data");
        }
    };
    private void openGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請開啟GPS連結")
                .setMessage("為了提高定位的精準度，更好的為您服務，請開啟GPS")
                .setPositiveButton("設置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //跳轉到手機打開GPS頁面
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //设置完成完後回到原本畫面
                        startActivityForResult(intent,0);
                    }
                })/*
            .setNeutralButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            })*/.show();
    }
}