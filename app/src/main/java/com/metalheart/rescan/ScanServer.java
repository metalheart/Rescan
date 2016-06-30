package com.metalheart.rescan;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by metalheart on 30.06.2016.
 */
public class ScanServer {
    public interface IScanServerListener
    {
        void onClientsChange();
    }

    public class ScanClient {
        private final Socket s_;
        ScanClient(final Socket s) {
            s_ = s;
        }
    }

    private static ScanServer instance_ = null;
    private final Collection<ScanClient> clients_ = Collections.synchronizedList(new ArrayList<ScanClient>());

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
    private IScanServerListener listener_ = null;
    private ScanClientListAdapter listAdapter_ = null;

    public boolean init(final Context ctx)
    {
        listAdapter_ = new ScanClientListAdapter(ctx);
        listenerTask_ = new ScanClientListener(ctx);

        listenerTask_.execute();
        return true;
    }

    public void setListener(IScanServerListener listener) {
        listener_ = listener;
    }

    BaseAdapter getClientsListAdapter() {
        return listAdapter_;
    }

    public void fini()
    {
        listenerTask_.cancel(true);
        listenerTask_ = null;
    }

    private class ScanClientListener extends AsyncTask<Void, Void, Void> {
        private final Context context_;

        public ScanClientListener(Context ctx) {
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
                            brsocket.receive(pkt);

                            String str = recvBuf.toString();
                            JsonReader reader = new JsonReader(new StringReader(str));
                            String id = "", ip = "", version = "";

                            reader.beginObject();
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
                                InetAddress serverAddr = InetAddress.getByName(ip);
                                SocketAddress sockaddr = new InetSocketAddress(serverAddr, 12345);

                                final Socket s = new Socket();
                                try {
                                    s.connect(sockaddr, 1000);
                                    synchronized (clients_) {
                                        clients_.add(new ScanClient(s));
                                    }
                                    if (listener_ != null) {
                                        listener_.onClientsChange();
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
                finally {
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

    public class ScanClientListAdapter extends BaseAdapter {
        private Context context_;

        public ScanClientListAdapter(Context c) {
            context_ = c;
        }

        public int getCount() {
            synchronized (clients_) {
                return clients_.size();
            }
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout itemView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                LayoutInflater layoutInflator = (LayoutInflater)context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflator.inflate(R.layout.main_screen_item, null);
                synchronized (clients_) {
                    convertView.setTag(null);
                }
            }

            return convertView;
        }
    }
}
