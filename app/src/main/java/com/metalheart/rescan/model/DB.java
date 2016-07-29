package com.metalheart.rescan.model;

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

    /*private class FieldSetter {
        public final DBItemBase.DBItemFieldDesc desc;
        public FieldSetter(DBItemBase.DBItemFieldDesc desc) {
            this.desc = desc;
        }

        private setField(Object obj, ) {

        }

        public void setValue(Object obj, Cursor c) {
            switch (desc.fieldType) {
                case Cursor.FIELD_TYPE_INTEGER: obj.
            }
        }
    }*/

    public static Object loadItem(String alias, SQLiteDatabase db) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        Class<? extends DBItemBase> clazz = registeredItems_.get(alias);
        if (clazz != null) {
            Cursor c = db.query(alias, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                Object instance = clazz.newInstance();

                Method method = clazz.getMethod(DBItemBase.Contract.GET_FIELDS_METHOD);
                DBItemBase.DBItemFieldDesc descs[] = (DBItemBase.DBItemFieldDesc[])method.invoke(null);

                for (DBItemBase.DBItemFieldDesc desc : descs) {
                    Field field = clazz.getDeclaredField(desc.fieldName);
                    field.set(c.);
                }
            }
        }

        return null;
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
