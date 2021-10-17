package com.example.test_gatt_1015.Module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test_gatt_1015.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private OnItemClick onItemClick;
    private ArrayList<ScannedData> array_list = new ArrayList<>();
    private Activity activity;


    public RecyclerViewAdapter(Activity activity){
        this.activity = activity;
    }

    public void clearDevice(){
        this.array_list.clear();
        notifyDataSetChanged();
    }
    public void addDevice(ArrayList<ScannedData> arrayList){
        this.array_list = arrayList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.scanned_item,parent,false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.name_tv.setText(array_list.get(position).getDevicename());
        holder.mac_tv.setText("裝置地址 :"+array_list.get(position).getDevice_mac());
        holder.record_tv.setText("裝置夾帶資訊 : \n"+array_list.get(position).getDevice_record());
        holder.rssi_tv.setText("訊號強度 :"+ array_list.get(position).getDevice_rssi());

    }

    @Override
    public int getItemCount() {
        return array_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView name_tv,mac_tv,record_tv,rssi_tv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name_tv = itemView.findViewById(R.id.Device_name_tv);
            mac_tv = itemView.findViewById(R.id.Device_adress_tv);
            record_tv = itemView.findViewById(R.id.Device_scanRecord_tv);
            rssi_tv = itemView.findViewById(R.id.Device_rssi_tv);
        }
    }
    public interface OnItemClick{
        void onItemClick(ScannedData selectedDevice);
    }

}
