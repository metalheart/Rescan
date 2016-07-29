package com.metalheart.rescan.model;
import android.database.Cursor;
import android.util.Pair;
/**
 * Created by m_antipov on 27.07.2016.
 */
public class DBItemBase {
    public long id;

    public DBItemBase(long id) {
        this.id = id;
    }

    public static class DBItemFieldDesc
    {
        public static final int FLAG_PK = 1;
        public static final int FLAG_AUTO_INCREMENT = 1 << 1;

        public String fieldName;
        public int fieldType;
        public int flags;

        public DBItemFieldDesc(String fieldName, int fieldType, int flags) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.flags = flags;
        }
    }

    public static class Contract {
        public static final String GET_FIELDS_METHOD = "getFields";
        public static final String GET_TABLE_NAME_METHOD = "getTableName";
    }

    public static DBItemFieldDesc[] getFields() { return new DBItemFieldDesc[]{new DBItemFieldDesc("id", Cursor.FIELD_TYPE_INTEGER, 0)}; }
    public static String getTableName() { return ""; }
}
