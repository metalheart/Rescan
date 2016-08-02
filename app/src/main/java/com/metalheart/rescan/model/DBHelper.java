package com.metalheart.rescan.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by m_antipov on 27.07.2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    final String LOG_TAG = "myLogs";

    public DBHelper(Context context) {
        super(context, "scanner_db", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, "--- onCreate database ---");
        // создаем таблицу с полями

        try {
            List<Pair<String, String>> tables = DB.generateDBCreateQuery();
            for (Pair<?,?> p : tables) {
                Log.d(LOG_TAG, "creating table" + (String)p.first);
                db.execSQL((String) p.second);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "--- onUpgrade database ---");
        // создаем таблицу с полями

        try {
            List<Pair<String, String>> tables = DB.generateDBCreateQuery();
            for (Pair<?,?> p : tables) {
                db.execSQL("drop table if exists " + (String)p.first + ";");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "--- onDowngrade database ---");
        // создаем таблицу с полями

        try {
            List<Pair<String, String>> tables = DB.generateDBCreateQuery();
            for (Pair<?,?> p : tables) {
                db.execSQL("drop table if exists " + (String)p.first + ";");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        onCreate(db);
    }
}
