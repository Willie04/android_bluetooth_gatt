package com.example.simple_ble_connect;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BluetoothLeScanner ble_scanner;
    BluetoothAdapter ble_Adapter;
    Button scan_btn;
    ArrayList<ScannedData> findDevice = new ArrayList<>();
    private int REQUEST_FINE_LOCATION_PERMISSION = 100;
    private int REQEST_ENABLE_BT = 2;
    RecycleViewAdapter madapter ;
    boolean isscaning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scan_btn = (Button) findViewById(R.id.btn_scan);
        scan_btn.setText("開始掃描");
        scan_btn.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ble_Adapter = BluetoothAdapter.getDefaultAdapter();
        checkPermission();
    }

    @Override
    public void onClick(View v) {
        int num = v.getId();
        switch (num){
            case R.id.btn_scan:
                checkPermission();
                start_scan();
                break;
        }
    }
    private void start_scan(){
        ble_scanner = ble_Adapter.getBluetoothLeScanner();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.store_device);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        madapter = new RecycleViewAdapter(this);
        recyclerView.setAdapter(madapter);
        findDevice.clear();
        if(isscaning){
            isscaning = false;
            ble_scanner.stopScan(myscancallback);
            Log.i("test","stop");
            scan_btn.setText("開始掃描");
        }else {
            isscaning = true;
            ble_scanner.startScan(myscancallback);
            Log.i("test", "start");
            madapter.clearDevice();
            scan_btn.setText("停止掃描");
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        isscaning = false;
        scan_btn.setText("開始掃描");
        ble_scanner.stopScan(myscancallback);
        madapter.clearDevice();
    }


    public ScanCallback myscancallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            new Thread(()->{
                if(result.getDevice().getName() != null){
                    findDevice.add(new ScannedData(result.getDevice().getName(),
                            result.getDevice().getAddress(),
                            byteArrayToHexStr(result.getScanRecord().getBytes()),
                            String.valueOf(result.getRssi())));
                    ArrayList newList = getSingle(findDevice);
                    Log.i("test",newList.toString());
                    runOnUiThread(()->{
                        madapter.addDevice(newList);
                    });
                }
            }).start();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i("into","2");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i("into","3"+errorCode);
        }
    };

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

    private int getIndex(ArrayList temp, Object obj) {
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).toString().contains(obj.toString())) {
                return i;
            }
        }
        return -1;
    }
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
    //畔對GPS and bluetooth
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
            if(!ble_Adapter.isEnabled()){
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent,REQEST_ENABLE_BT);
                Log.i("Tag","請求開幾bt");
            }
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                openGPSDialog();
            }

        }else finish();
    }
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
                }).show();
    }

}