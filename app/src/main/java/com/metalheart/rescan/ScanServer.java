package com.metalheart.rescan;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Created by metalheart on 30.06.2016.
 */
public class ScanServer {
    public class ScanClient {
        public static final long READ_TIMEOUT = 30000;
        public final Socket s;
        public final String id;
        private long lastSuccessfllIO_;

        private final BufferedInputStream bufferedInputStream_;
        private ArrayDeque<String> data_ = new ArrayDeque<String>();

        ScanClient(final Socket s, final String id) throws IOException {
            lastSuccessfllIO_ = System.currentTimeMillis();
            this.s = s;
            this.id = id;
            this.bufferedInputStream_ = new BufferedInputStream(s.getInputStream());
        }

        private void ping() throws IOException {
            try{
                OutputStream os = s.getOutputStream();
                JsonWriter writer = new JsonWriter(new OutputStreamWriter(os));
                writer.beginObject();
                writer.name("type").value("ping");
                writer.endObject();
                writer.flush();
            } catch (Exception e) {
                try {
                    s.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        private void readData(final long timeout) throws IOException{
            synchronized (data_) {

                boolean msgRead = false;
                final long timeoutTime = System.currentTimeMillis() + timeout;
                int bracketCount = 0;

                String msg = new String();

                while (!msgRead && System.currentTimeMillis() < timeoutTime) {
                    if (bufferedInputStream_.available() > 0) {
                        int ch = bufferedInputStream_.read();
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
                    throw new RuntimeException();
                }

                lastSuccessfllIO_ = System.currentTimeMillis();

                data_.push(msg);
            }
        }

        String popData() {
            synchronized (data_) {
                if (data_.size() > 0) {
                    return data_.pop();
                }
            }

            return null;
        }

        boolean isAlive() {
            return s!= null && s.isConnected() && (System.currentTimeMillis() - lastSuccessfllIO_) < READ_TIMEOUT;
        }
    }

    public interface IScanServerListener
    {
        void onClientEvent(ScanClient client);
    }

    private static ScanServer instance_ = null;
    private final Map<String, ScanClient> clients_ = java.util.Collections.synchronizedMap(new HashMap<String, ScanClient>());

    private ScanServer() {

    }

    public static ScanServer getInstance() {
        if(instance_ == null)
        {
            synchronized (ScanServer.class) {
                if (instance_ == null) {
                    instance_ = new ScanServer();
                }
            }
        }

        return instance_;
    }

    private AsyncTask<Void, Void, Void> listenerTask_ = null;
    private AsyncTask<Void, Void, Void> communicationTask_ = null;

    private IScanServerListener listener_ = null;

    public boolean init(final Context ctx)
    {
        listenerTask_ = new ScanClientFinder(ctx);
        communicationTask_ = new ScanClientListener(ctx);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            listenerTask_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            communicationTask_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            listenerTask_.execute();
            communicationTask_.execute();
        }
        return true;
    }

    public void setListener(IScanServerListener listener) {
        listener_ = listener;
    }

    public void fini()
    {
        communicationTask_.cancel(true);
        communicationTask_ = null;

        listenerTask_.cancel(true);
        listenerTask_ = null;
    }

    private class ScanClientFinder extends AsyncTask<Void, Void, Void> {
        private final Context context_;

        public ScanClientFinder(Context ctx) {
            context_ = ctx;
        }
        @Override
        protected Void doInBackground(Void... params) {

            WifiManager wifi;
            wifi = (WifiManager) context_.getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock mLock = wifi.createMulticastLock("lock");

            try {
                mLock.acquire();

                DatagramSocket brsocket = new DatagramSocket(6666);
                try {
                    //brsocket.setBroadcast(true);
                    brsocket.setSoTimeout(100);

                    while (!isCancelled()) {
                        byte[] recvBuf = new byte[255];
                        DatagramPacket pkt = new DatagramPacket(recvBuf, recvBuf.length);

                        try {
                            try {
                                brsocket.receive(pkt);
                            } catch (Exception e) {
                                continue;
                            }

                            String str = new String(recvBuf, StandardCharsets.US_ASCII);
                            JsonReader reader = new JsonReader(new StringReader(str));
                            String id = "", ip = "", version = "";

                            try {
                                reader.beginObject();
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }
                            while (reader.hasNext()) {
                                String name = reader.nextName();
                                if (name.equals("id")) {
                                    id = reader.nextString();
                                } else if (name.equals("version")) {
                                    version = reader.nextString();
                                } else if (name.equals("ip")) {
                                    ip = reader.nextString();
                                }
                            }

                            if (!id.isEmpty() && !ip.isEmpty()) {
                                ScanClient client =  clients_.get(id);
                                if (client != null && client.isAlive()) {
                                    continue;
                                }

                                InetAddress serverAddr = InetAddress.getByName(ip);
                                SocketAddress sockaddr = new InetSocketAddress(serverAddr, 12345);

                                final Socket s = new Socket();
                                try {
                                    s.connect(sockaddr, 1000);
                                    client = new ScanClient(s, id);
                                    synchronized (clients_) {
                                        clients_.put(id, client);
                                    }
                                    if (listener_ != null) {
                                        listener_.onClientEvent(client);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    brsocket.close();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } finally {
                mLock.release();
            }

            return null;
        }
    };

    private class ScanClientListener extends AsyncTask<Void, Void, Void> {
        private final Context context_;

        public ScanClientListener(Context ctx) {
            context_ = ctx;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                while (!isCancelled()) {
                    Set<String> keys = null;

                    synchronized (clients_) {
                        keys = clients_.keySet();
                    }

                    //TODO: make cleaner code
                    for (String key : keys) {
                        ScanClient client = null;
                        synchronized (clients_) {
                            client = clients_.get(key);
                        }

                        try {
                            if (client.isAlive()) {
                                client.ping();
                                client.readData(100);
                            } else {
                                synchronized (clients_) {
                                    clients_.remove(key);
                                }
                            }
                            listener_.onClientEvent(client);
                        } catch (Exception e) {
                        }
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
            }

            return null;
        }
    };
}
