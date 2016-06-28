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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.EventListener;
import java.util.List;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Created by m_antipov on 22.06.2016.
 */
public class BindScanTask extends AsyncTask<Void, Void, Void> {
    public interface IBindScanTaskListener extends EventListener {
        void onComplete(boolean success);
        void onError();
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
        writer.beginArray();
        writer.beginObject();
        writer.name("SSID").value("DIR-300NRUB6");
        writer.name("PWD").value("1qaz2wsx3edc");
        writer.endObject();
        writer.endArray();

        writer.close();
    }

    private boolean processDeviceResponse(final long timeout, BufferedInputStream bis) throws IOException{
        boolean msgRead = false;
        final long timeoutTime = System.currentTimeMillis() + timeout;
        int bracketCount = 0;

        bis.mark(0);
        long read = 0;

        while (!msgRead && System.currentTimeMillis() < timeoutTime) {
            if (bis.available() > read) {
                int ch = bis.read();
                ++read;

                if (ch == '[') {
                    ++bracketCount;
                } else if (ch == ']') {
                    if (--bracketCount == 0) {
                        msgRead = true;
                        break;
                    }

                }
            }
        }

        if (!msgRead) {
            return false;
        }
        bis.reset();

        JsonReader reader = new JsonReader(new InputStreamReader(bis));

        long id = -1, version = -1;

        reader.beginArray();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                id = reader.nextLong();
            } else if (name.equals("version")) {
                version = reader.nextLong();
            }
        }

        return true;
    }
    @Override
    protected Void doInBackground(Void... params) {

        boolean done = false;

        while (runCount_ < maxConnectRetries_ && !done) {
            ++runCount_;

            try {
                List<WifiApControl.Client> clients = ap_.getClients();
                if (clients != null) {
                    //we wont do it in separate processes, just lessen socket timeout
                    for (WifiApControl.Client c : clients) {
                        InetAddress serverAddr = InetAddress.getByName(c.ipAddr);
                        SocketAddress sockaddr = new InetSocketAddress(serverAddr, 12345);

                        Socket s = new Socket();
                        try {
                            s.connect(sockaddr, 1000);
                            OutputStream os = s.getOutputStream();
                            InputStream is = s.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);

                            Thread.sleep(500);
                            if (processDeviceResponse(1000, bis)) {
                                sendSettingsToDevice(os);
                            }

                            Thread.sleep(500);
                            bis.reset();
                            if (is.available() >= 4) {
                                byte[] data = new byte[4];
                                is.read(data, 0, 4);

                                done = true;

                                break;
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

        listener_.onComplete(done);

        return null;
    }

    long runCount() {
        return runCount_;
    }
};