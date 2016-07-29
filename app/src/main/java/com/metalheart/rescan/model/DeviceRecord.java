package com.metalheart.rescan.model;

import android.database.Cursor;
import android.databinding.ObservableArrayList;

/**
 * Created by m_antipov on 27.07.2016.
 */
public class DeviceRecord extends DBItemBase {
    public long serial;
    public String humanReadableName;

    public static DBItemFieldDesc[] getFields() {
        return concatenateFields(
                DBItemBase.getFields(),
                new DBItemFieldDesc[]{
                        new DBItemFieldDesc("serial", Cursor.FIELD_TYPE_INTEGER, 0),
                        new DBItemFieldDesc("serial", Cursor.FIELD_TYPE_STRING, 0)
                });
    }
    public static String getTableName() { return "devices"; }

    public DeviceRecord() {
        super();
    }

    static {
        DB.registerItem(DeviceRecord.getTableName(), DeviceRecord.class);
    }
}
