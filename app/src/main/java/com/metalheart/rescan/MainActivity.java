package com.metalheart.rescan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity implements ScanServer.IScanServerListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
