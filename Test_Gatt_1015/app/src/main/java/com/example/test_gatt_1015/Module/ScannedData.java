package com.example.test_gatt_1015.Module;

import androidx.annotation.Nullable;

public class ScannedData {
    private String Devicename;
    private String Device_mac;
    private String Device_record;
    private String Device_rssi;

    public ScannedData(String devicename,String device_mac,String device_record, String device_rssi)
    {
        this.Devicename = devicename;
        this.Device_mac = device_mac;
        this.Device_record = device_record;
        this.Device_rssi = device_rssi;
    }

    public String getDevice_mac() {
        return Device_mac;
    }

    public String getDevice_rssi() {
        return Device_rssi;
    }

    public String getDevicename() {
        return Devicename;
    }

    public String getDevice_record() {
        return Device_record;
    }

    //濾掉重複macaddress
    public boolean equals(@Nullable Object j){
        ScannedData data = (ScannedData) j;
        return this.Device_mac.equals(data.Device_mac);
    }
    public String toString(){
        return this.Device_mac;
    }
}
