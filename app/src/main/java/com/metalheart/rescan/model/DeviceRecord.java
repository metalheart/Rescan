package com.metalheart.rescan.model;

import android.database.Cursor;
import android.databinding.Bindable;
import android.databinding.ObservableArrayList;

import com.metalheart.rescan.BR;

/**
 * Created by m_antipov on 27.07.2016.
 */
public class DeviceRecord extends DBItemBase {
    long serial;
    public String humanReadableName;
    private boolean connected;

    public static DBItemFieldDesc[] getFields() {
        return concatenateFields(
                DBItemBase.getFields(),
                new DBItemFieldDesc[]{
                        new DBItemFieldDesc("serial", Cursor.FIELD_TYPE_INTEGER, 0),
                        new DBItemFieldDesc("humanReadableName", Cursor.FIELD_TYPE_STRING, 0)
                });
    }

    @Bindable
    public boolean getConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        notifyPropertyChanged(BR.connected);
    }

    @Bindable
    public String getHumanReadableName() {
        return this.humanReadableName;
    }

    public void setHumanReadableName(String str) {
        this.humanReadableName = str;
        notifyPropertyChanged(BR.connected);
    }

    @Bindable
    public String getSerial() {
        return Long.toString(this.serial);
    }

    public void setSerial(String serial) {
        this.serial = Long.parseLong(serial);
        notifyPropertyChanged(BR.connected);
    }


    public static final String TABLE_NAME = "devices";
    public static String getTableName() { return TABLE_NAME; }
    public DeviceRecord() {
        super();
    }
}
