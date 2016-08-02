package com.metalheart.rescan.view.binding;

import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableArrayMap;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.metalheart.rescan.R;
import com.metalheart.rescan.model.DBItemBase;
import com.metalheart.rescan.model.DeviceRecord;

/**
 * Created by metalheart on 30.07.2016.
 */
public class DeviceRecordListBinder {
    @BindingAdapter("bind:imageRes")
    public static void bindImage(ImageView view, boolean active) {
        view.setImageResource(active ? R.drawable.ic_connected : R.drawable.ic_not_connected);
    }

    @BindingAdapter("bind:devices")
    public static void bindDevices(GridView view, ObservableArrayList<DeviceRecord>  list) {
        ListAdapter adapter = new DeviceRecordListAdapter(list);
        view.setAdapter(adapter);
    }
}
