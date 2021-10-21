package com.example.simple_ble_connect;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.ViewHolder>{

    private List<ScannedData> arrayList = new ArrayList<>();
    private Activity activity;


    public RecycleViewAdapter(MainActivity mainActivity) {
        this.activity= mainActivity;
    }
    public void clearDevice(){
        this.arrayList.clear();
        notifyDataSetChanged();
    }
    public void addDevice(ArrayList<ScannedData> arrayList){
        this.arrayList = arrayList;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_device,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.name_tv.setText(arrayList.get(position).getDeviceName());
        holder.mac_tv.setText("裝置地址 :"+arrayList.get(position).getAddress());
        holder.record_tv.setText("裝置夾帶資訊 : \n"+arrayList.get(position).getScanrecord());
        holder.rssi_tv.setText("訊號強度 :"+ arrayList.get(position).getRssi());
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(activity, after_connect.class);
            intent.putExtra(after_connect.INTENT_KEY,arrayList.get(position));
            activity.startActivity(intent);
            Log.i("成功","拉");
        });
    }
    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView name_tv,mac_tv,record_tv,rssi_tv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name_tv = itemView.findViewById(R.id.device_name_tv);
            mac_tv = itemView.findViewById(R.id.device_address_tv);
            rssi_tv = itemView.findViewById(R.id.device_Rssi_tv);
            record_tv = itemView.findViewById(R.id.device_record_tv);
        }
    }
}
