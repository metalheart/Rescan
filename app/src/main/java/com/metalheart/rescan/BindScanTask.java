package com.metalheart.rescan;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
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
    @Override
    protected Void doInBackground(Void... params) {
        ++runCount_;

        boolean done = false;
        try{
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
                        os.write("123".getBytes());
                        wait(1000);
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
        }
        catch (Exception e) {
            listener_.onError();
        }
        finally {
            listener_.onComplete(done);
        }

        return null;
    }

    public interface IBindScanTaskListener extends EventListener {
        void onComplete(boolean success);
        void onError();
    }

    private IBindScanTaskListener listener_ = null;
    private WifiApControl ap_ = null;
    private long runCount_ = 0;

    public BindScanTask(WifiApControl ap, IBindScanTaskListener listener)
    {
        listener_ = listener;
        ap_ = ap;
    }

    long runCount() {
        return runCount_;
    }
};