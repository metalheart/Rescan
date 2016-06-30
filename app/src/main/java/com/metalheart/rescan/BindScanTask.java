package com.metalheart.rescan;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Created by m_antipov on 22.06.2016.
 */
public class BindScanTask extends AsyncTask<Void, Void, Void> {
    public class DeviceInfo {
        long id;
        long version;

        DeviceInfo() {
            id = -1;
            version = -1;
        }
    }

    public interface IBindScanTaskListener extends EventListener {
        void onComplete(boolean success);
        void onError();
        void onDeviceBonded(DeviceInfo info);
    }

    private final long maxConnectRetries_;
    private final IBindScanTaskListener listener_;
    private final WifiApControl ap_;
    private long runCount_ = 0;

    public BindScanTask(WifiApControl ap, long maxConnectRetries, IBindScanTaskListener listener)
    {
        listener_ = listener;
        ap_ = ap;
        maxConnectRetries_ = maxConnectRetries;
    }

    private void sendSettingsToDevice(OutputStream os) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(os));
        writer.beginObject();
        writer.name("SSID").value("DIR-300NRUB6");
        writer.name("PWD").value("1qaz2wsx3edc");
        writer.endObject();
        writer.close();
    }

    private DeviceInfo processDeviceResponse(final long timeout, BufferedInputStream bis) throws IOException{
        DeviceInfo info = new DeviceInfo();

        boolean msgRead = false;
        final long timeoutTime = System.currentTimeMillis() + timeout;
        int bracketCount = 0;

        String msg = new String();

        while (!msgRead && System.currentTimeMillis() < timeoutTime) {
            if (bis.available() > 0) {
                int ch = bis.read();
                msg += (char)ch;

                if (ch == '{') {
                    ++bracketCount;
                } else if (ch == '}') {
                    if (--bracketCount == 0) {
                        msgRead = true;
                        break;
                    }
                }
            }
        }

        if (!msgRead) {
            return info;
        }

        JsonReader reader = new JsonReader(new StringReader(msg));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                info.id = reader.nextLong();
            } else if (name.equals("version")) {
                info.version = reader.nextLong();
            }
        }

        return info;
    }
    @Override
    protected Void doInBackground(Void... params) {

        boolean done = false;

        HashSet<String> foundClients = new HashSet<String>();

        while (runCount_ < maxConnectRetries_ && !isCancelled()) {
            ++runCount_;

            try {
                List<WifiApControl.Client> clients = ap_.getClients();
                if (clients != null) {
                    //we wont do it in separate processes, just lessen socket timeout
                    for (WifiApControl.Client c : clients) {
                        if (foundClients.contains(c.ipAddr)) {
                            continue;
                        }

                        InetAddress serverAddr = InetAddress.getByName(c.ipAddr);
                        SocketAddress sockaddr = new InetSocketAddress(serverAddr, 12345);

                        Socket s = new Socket();
                        try {
                            s.connect(sockaddr, 1000);
                            OutputStream os = s.getOutputStream();
                            InputStream is = s.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);

                            Thread.sleep(500);
                            DeviceInfo info = processDeviceResponse(1000, bis);
                            if (info.id > 0) {
                                sendSettingsToDevice(os);
                                foundClients.add(c.ipAddr);
                                listener_.onDeviceBonded(info);
                            }
                        } catch (Exception e) {
                            Log.d("test", "error!");
                        } finally {
                            if (s.isConnected()) {
                                s.close();
                            }
                        }
                        //c.ipAddr;
                    }
                }
            } catch (Exception e) {
                listener_.onError();
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        listener_.onComplete(!foundClients.isEmpty());

        return null;
    }

    long runCount() {
        return runCount_;
    }
};