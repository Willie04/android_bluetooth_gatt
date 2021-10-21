package com.example.simple_ble_connect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class ScannedData implements Serializable {
    private String deviceName;
    private String rssi;
    private String address;
    private String scanrecord;
    public ScannedData(String deviceName, String address, String scanrecord,String rssi) {
        this.deviceName = deviceName;
        this.rssi = rssi;
        this.scanrecord = scanrecord;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getRssi() {
        return rssi;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getScanrecord(){return scanrecord;}
    @Override
    public boolean equals(@Nullable Object obj) {
        ScannedData p = (ScannedData)obj;

        return this.address.equals(p.address);
    }

    @NonNull
    @Override
    public String toString() {
        return this.address;
    }
}