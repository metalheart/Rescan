package com.metalheart.rescan.view.binding;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.metalheart.rescan.R;
import com.metalheart.rescan.databinding.DeviceRecordItemBinding;
import com.metalheart.rescan.model.*;
import com.metalheart.rescan.model.DeviceRecord;

import com.metalheart.rescan.databinding.*;

/**
 * Created by metalheart on 30.07.2016.
 */
public class DeviceRecordListAdapter  extends BaseAdapter {
    private ObservableArrayList<DeviceRecord> list = null;
    private LayoutInflater inflater = null;

    public DeviceRecordListAdapter(ObservableArrayList<DeviceRecord> l) {
        list = l;
    }

    @Override
    public int getCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        DeviceRecordItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.device_record_item, parent, false);
        binding.setDevice(list.get(position));

        return binding.getRoot();
    }
}