package com.metalheart.rescan.model;

/**
 * Created by metalheart on 30.07.2016.
 */
import android.database.sqlite.SQLiteDatabase;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableArrayMap;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public class DeviceRecordList {
    public ObservableArrayList<DeviceRecord> list = new ObservableArrayList<>();

    public DeviceRecordList(SQLiteDatabase db) {
        try {
            List<Object> devices = DB.loadItems(db, DeviceRecord.TABLE_NAME, null, "", null, "" );
            for (Object item : devices) {
                list.add((DeviceRecord)item);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public DeviceRecord find(String serial) {
        for (DeviceRecord device : list) {
            String strSerial = device.getSerial();
            if ( strSerial.equals(serial)) {
                return device;
            }
        }

        return null;
    }

    // Called on add button click
    public void add(View v) {
        //list.add(device);
    }

    // Called on remove button click
    public void remove(View v) {
        //if (!list.isEmpty()) {
        //    list.remove(0);
        //}
    }

    public void add(DeviceRecord device) {
        list.add(device);
    }
}
