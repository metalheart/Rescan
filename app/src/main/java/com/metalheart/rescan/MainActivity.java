package com.metalheart.rescan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

import com.metalheart.rescan.model.DBHelper;

public class MainActivity extends AppCompatActivity implements ScanServer.IScanServerListener {
    public DBHelper dbHelper_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper_ = new DBHelper(this.getApplicationContext());

        ScanServer.getInstance().setListener(this);
        ScanServer.getInstance().init(getApplication());

        setContentView(R.layout.main_screen);
        GridView gv = (GridView) findViewById(R.id.gridView);
        gv.setAdapter(ScanServer.getInstance().getClientsListAdapter());
    }

    @Override
    public void onClientsChange() {
        GridView gv = (GridView) findViewById(R.id.gridView);
        gv.invalidate();
    }
}
