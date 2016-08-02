package com.metalheart.rescan.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.metalheart.rescan.model.DBItemBase.DBItemFieldDesc.FLAG_PK;
import static com.metalheart.rescan.model.DBItemBase.DBItemFieldDesc.FLAG_AUTO_INCREMENT;

/**
 * Created by m_antipov on 29.07.2016.
 */
public class DB {
    private static final HashMap<String, Class<? extends DBItemBase>> registeredItems_ = new HashMap<String, Class<? extends DBItemBase>>();

    public static void init() {
        DB.registerItem(DeviceRecord.TABLE_NAME, DeviceRecord.class);
    }

    public static boolean registerItem(String alias, Class<? extends DBItemBase> clazz) {
        registeredItems_.put(alias, clazz);
        return true;
    }

    private static String convertCursorTypeToString(int type) {
        switch (type) {
            case Cursor.FIELD_TYPE_INTEGER: return "integer";
            case Cursor.FIELD_TYPE_STRING: return "text";
            default: throw new InvalidParameterException();
        }
    }

    private static class FieldSetter {
        public final DBItemBase.DBItemFieldDesc desc_;
        public final Field classField_;

        public FieldSetter(DBItemBase.DBItemFieldDesc desc, Class clazz) throws NoSuchFieldException {
            this.desc_ = desc;
            this.classField_ = clazz.getField(desc.fieldName);
        }

        public void setValue(Object obj, Cursor c) throws NoSuchFieldException, IllegalAccessException, InvalidParameterException {
            int columnIndex = c.getColumnIndex(desc_.fieldName);
            switch (desc_.fieldType) {
                case Cursor.FIELD_TYPE_INTEGER: classField_.setInt(obj, c.getInt(columnIndex)); break;
                case Cursor.FIELD_TYPE_STRING: classField_.set(obj, c.getString(columnIndex)); break;
                default: throw new InvalidParameterException();
            }
        }
    }

    public static List<Object> loadItems(SQLiteDatabase db, String tableName, String[] columns, String where, String[] args, String limit) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        List<Object> result = new ArrayList<>();

        Class<? extends DBItemBase> clazz = registeredItems_.get(tableName);
        if (clazz != null) {
            Cursor c = db.query(tableName, columns, where, args, null, null, null);
            if (c.moveToFirst()) {
                do {
                    Object item = clazz.newInstance();

                    Method method = clazz.getMethod(DBItemBase.Contract.GET_FIELDS_METHOD);
                    DBItemBase.DBItemFieldDesc descs[] = (DBItemBase.DBItemFieldDesc[]) method.invoke(null);

                    for (DBItemBase.DBItemFieldDesc desc : descs) {
                        FieldSetter setter = new FieldSetter(desc, clazz);
                        setter.setValue(item, c);
                    }

                    result.add(item);
                } while (c.moveToNext());
            }
        }

        return result;
    }

    private static class FieldGetter {
        public final DBItemBase.DBItemFieldDesc desc_;
        public final Field classField_;

        public FieldGetter(DBItemBase.DBItemFieldDesc desc, Class clazz) throws NoSuchFieldException {
            this.desc_ = desc;
            this.classField_ = clazz.getField(desc.fieldName);
        }

        public void getValue(Object obj, ContentValues cv) throws NoSuchFieldException, IllegalAccessException, InvalidParameterException {
            switch (desc_.fieldType) {
                case Cursor.FIELD_TYPE_INTEGER: cv.put(desc_.fieldName, classField_.getLong(obj)); break;
                case Cursor.FIELD_TYPE_STRING: cv.put(desc_.fieldName, (String) classField_.get(obj)); break;
                default: throw new InvalidParameterException();
            }
        }
    }

    public static DBItemBase storeItem(SQLiteDatabase db, DBItemBase item) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        Class clazz = item.getClass();
        Method getFieldsMethod = clazz.getMethod(DBItemBase.Contract.GET_FIELDS_METHOD);
        Method getTableNameMathod = clazz.getMethod(DBItemBase.Contract.GET_TABLE_NAME_METHOD);

        DBItemBase.DBItemFieldDesc descs[] = (DBItemBase.DBItemFieldDesc[]) getFieldsMethod.invoke(null);
        String tableName = (String) getTableNameMathod.invoke(null);

        ContentValues cv = new ContentValues();

        for (DBItemBase.DBItemFieldDesc desc : descs) {
            //skip ID field
            //TODO: implement in more clean way
            if (desc.fieldName == DBItemBase.getFields()[0].fieldName) {
                continue;
            }

            FieldGetter getter = new FieldGetter(desc, clazz);
            getter.getValue(item, cv);
        }

        if (item.id < 0) {
            item.id = db.insert(tableName, null, cv);
        } else {
            db.update(tableName, cv, "id = ", new String[]{Long.toString(item.id)});
        }
        return item;
    }


    private static String parseFlags(int flags) {
        String result = "";
        if ((flags & FLAG_PK) != 0) {
            result += " primary key";
        }
        if ((flags & FLAG_AUTO_INCREMENT) != 0) {
            result += " autoincrement";
        }
        return result;
    }

    private static String generateCreateQuery(Class<? extends DBItemBase> clazz, String table) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = clazz.getMethod(DBItemBase.Contract.GET_FIELDS_METHOD);
        DBItemBase.DBItemFieldDesc descs[] = (DBItemBase.DBItemFieldDesc[])method.invoke(null);

        String result = "CREATE TABLE " + table + " (";

        for (int i = 0; i < descs.length; ++i) {
            result += descs[i].fieldName
                    + " " + convertCursorTypeToString(descs[i].fieldType)
                    + " " + parseFlags(descs[i].flags)
                    + (i < (descs.length - 1) ? "," : "");
        }

        result += ");";

        return result;
    }

    public static List<Pair<String, String>> generateDBCreateQuery() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();

        for (Map.Entry<String, Class<? extends DBItemBase>> entry : registeredItems_.entrySet())
        {
            String query = generateCreateQuery(entry.getValue(), entry.getKey());
            result.add(new Pair<>(entry.getKey(), query));
        }

        return result;
    }
}
