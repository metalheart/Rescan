package com.metalheart.rescan.model;
import android.database.Cursor;
import android.util.Pair;
/**
 * Created by m_antipov on 27.07.2016.
 */
public class DBItemBase {
    public long id;

    public DBItemBase() {
        this.id = -1;
    }

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

    protected static DBItemFieldDesc[] concatenateFields(DBItemFieldDesc[] fields1, DBItemFieldDesc[] fields2) {
        DBItemFieldDesc[] result = new DBItemFieldDesc[fields1.length + fields2.length];

        System.arraycopy(fields1, 0, result, 0, fields1.length);
        System.arraycopy(fields2, 0, result, fields1.length, fields2.length);

        return result;
    }

    public static DBItemFieldDesc[] getFields() { return new DBItemFieldDesc[]{new DBItemFieldDesc("id", Cursor.FIELD_TYPE_INTEGER, 0)}; }
    public static String getTableName() { return ""; }
}
