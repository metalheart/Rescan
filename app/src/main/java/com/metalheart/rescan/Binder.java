package com.metalheart.rescan;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.EventListener;
import java.util.List;

import cc.mvdan.accesspoint.WifiApControl;

/**
 * Created by m_antipov on 21.06.2016.
 */
public class Binder implements BindScanTask.IBindScanTaskListener{
    public interface IBindListener extends EventListener {
        void onComplete(String sn);
        void onError();
    }

    private static volatile Binder instance_ = null;
    private boolean inited_ = false;
    private boolean isBinding_ = false;
    private IBindListener bindResultListener_ = null;
    private WifiManager wifiManager_ = null;
    private WifiApControl apControl_ = null;
    private WifiConfiguration apConfiguration_ = null;

    private final Handler bindScanThreadHandler_ = new Handler();

    private Binder() {

    }

    public void init(Context ctx) {
        synchronized (Binder.class) {
            apControl_ = WifiApControl.getInstance(ctx);
            if (apControl_ != null) {
                wifiManager_ = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                apConfiguration_ = new WifiConfiguration();
                apConfiguration_.SSID = "Rescan Bind AP";
                apConfiguration_.preSharedKey = "\"123456789\"";
                apConfiguration_.hiddenSSID = false;
                apConfiguration_.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                apConfiguration_.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                apConfiguration_.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                apConfiguration_.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                apConfiguration_.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                apConfiguration_.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                apConfiguration_.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                inited_ = true;
            }
            /*boolean enabled = apControl_.isEnabled();
            int state = apControl_.getState();

            WifiConfiguration config = apControl_.getConfiguration();
            Inet4Address addr4 = apControl_.getInet4Address();
            Inet6Address addr6 = apControl_.getInet6Address();

            // These are cached and may no longer be connected, see
            // WifiApControl.getReachableClients(int, ReachableClientListener)
            List<WifiApControl.Client> clients = apControl_.getClients()

            // Wifi must be disabled to enable the access point


            apControl_.disable();
            wifiManager_.setWifiEnabled(true);*/
        }
    }

    public static Binder getInstance() {
        if(instance_ == null)
        {
            synchronized (Binder.class) {
                if (instance_ == null) {
                    instance_ = new Binder();
                }
            }
        }

        return instance_;
    }

    private BindScanTask bindScanTask_ = null;

    public boolean startBind(final IBindListener listener) {
        synchronized (this) {
            if (isBinding_ || !inited_) {
                return false;
            }

            wifiManager_.setWifiEnabled(false);
            apControl_.setWifiApEnabled(apConfiguration_, true);

            bindResultListener_ = listener;
            bindScanTask_ = new BindScanTask(apControl_, 100, this);
            bindScanTask_.execute();

            isBinding_ = true;
        }

        return true;
    }

    @Override
    public void onComplete(boolean success) {
        synchronized (this) {
            if (success) {
                bindResultListener_.onComplete("");
            } else {
                bindResultListener_.onError();
            }
            stopBind();
        }
    }

    @Override
    public void onError() {
        stopBind();
    }

    public void stopBind() {
        synchronized (this) {
            //bindScanThreadHandler_.removeCallbacks(bindScanTask_);

            bindScanTask_ = null;
            isBinding_ = false;

            apControl_.disable();
        }
    }
}
