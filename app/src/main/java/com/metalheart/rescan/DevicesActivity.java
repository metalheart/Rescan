package com.metalheart.rescan;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;

import com.metalheart.rescan.databinding.ActivityDeviceListBinding;
import com.metalheart.rescan.model.DB;
import com.metalheart.rescan.model.DBHelper;
import com.metalheart.rescan.model.DeviceRecord;
import com.metalheart.rescan.model.DeviceRecordList;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

public class DevicesActivity extends AppCompatActivity implements ScanServer.IScanServerListener {
    public DBHelper dbHelper_ = null;
    private DeviceRecordList devices_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DB.init();

        dbHelper_ = new DBHelper(this.getApplicationContext());
        devices_ = new DeviceRecordList(dbHelper_.getReadableDatabase());

        ScanServer.getInstance().setListener(this);
        ScanServer.getInstance().init(getApplicationContext());

        ActivityDeviceListBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_device_list);
        binding.setDevices(devices_);

        Toolbar toolbar = (Toolbar) findViewById(R.id.devices_toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onClientEvent(ScanServer.ScanClient client) {
        DeviceRecord device = devices_.find(client.id);
        if (device == null) {
            device = new DeviceRecord();

            device.setHumanReadableName("New device");
            device.setSerial(client.id);
            device.setConnected(true);

            try {
                device = (DeviceRecord)DB.storeItem(dbHelper_.getWritableDatabase(), device);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            devices_.add(device);
        } else {
            device.setConnected(client.isAlive());
        }

        processDevice(client, device);
    }

    void processDevice(ScanServer.ScanClient client, DeviceRecord device) {
        String clientData = client.popData();
        if (clientData != null) {
            JsonReader reader = new JsonReader(new StringReader(clientData));
            String type = "";
            String data = "";
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("id")) {
                    } else if (name.equals("type")) {
                        type = reader.nextString();
                    } else if (name.equals("data")) {
                        data = reader.nextString();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (type == "SCAN") {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_devices, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_add) {
            DeviceRecord device = new DeviceRecord();
            device.setHumanReadableName("1212121212");
            device.setSerial("112212");
            devices_.add(device);
        }

        return super.onOptionsItemSelected(item);
    }
}
