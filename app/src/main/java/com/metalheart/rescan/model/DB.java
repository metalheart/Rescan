package com.metalheart.rescan.model;

import android.database.Cursor;
import android.util.Pair;

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

    public static Object loadItem(String alias) {
        Class<? extends DBItemBase> clazz = registeredItems_.get(alias);
        if (clazz != null) {

        }

        return null;
    }

    private static String convertCursorTypeToString(int type) {
        switch (type) {
            case Cursor.FIELD_TYPE_INTEGER: return "integer";
            default: throw new InvalidParameterException();
        }
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
