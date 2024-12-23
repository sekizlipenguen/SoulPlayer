package com.sekizlipenguen.soulplayer;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private WifiManager.MulticastLock multicastLock;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    private InetAddress getLocalInetAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        Log.d(TAG, "Yerel IP Adresi: " + address.getHostAddress());
                        return address;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Yerel IP adresi alınırken hata oluştu.", e);
        }
        return null;
    }

    @ReactMethod
    public void scanForDevices(final int timeout, final Promise promise) {
        JSONObject result = new JSONObject();
        JSONArray googleCastDevices = new JSONArray();
        JSONArray airPlayDevices = new JSONArray();

        // MulticastLock oluştur ve etkinleştir
        try {
            WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("soulplayer_multicast_lock");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
                Log.d(TAG, "MulticastLock başarıyla etkinleştirildi.");
            } else {
                Log.e(TAG, "WifiManager alınamadı. MulticastLock oluşturulamadı.");
                promise.reject("WIFI_MANAGER_ERROR", "WifiManager alınamadı.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "MulticastLock işlemi sırasında hata oluştu.", e);
            promise.reject("MULTICAST_LOCK_ERROR", e.getMessage());
            return;
        }

        new Thread(() -> {
            JmDNS jmdns = null;
            try {
                InetAddress localAddress = getLocalInetAddress();
                if (localAddress == null) {
                    Log.e(TAG, "Yerel IP adresi alınamadı, JmDNS başlatılamadı.");
                    promise.reject("NETWORK_ERROR", "Yerel IP adresi alınamadı.");
                    return;
                }

                jmdns = JmDNS.create(localAddress);
                Log.d(TAG, "JmDNS başlatıldı: " + localAddress.getHostAddress());

                // Google Cast cihazlarını tarama
                jmdns.addServiceListener("_googlecast._tcp.local.", new ServiceListener() {
                    @Override
                    public void serviceAdded(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast cihazı bulundu: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast cihazı kaldırıldı: " + event.getName());
                    }

                    @Override
                    public void serviceResolved(javax.jmdns.ServiceEvent event) {
                        ServiceInfo info = event.getInfo();
                        JSONObject device = new JSONObject();
                        try {
                            device.put("name", info.getName());
                            device.put("address", info.getHostAddresses()[0]);
                            device.put("port", info.getPort());
                            googleCastDevices.put(device);
                            Log.d(TAG, "Google Cast cihazı çözüldü: " + info.getName());
                        } catch (Exception e) {
                            Log.e(TAG, "Google Cast cihazını JSON'a eklerken hata oluştu.", e);
                        }
                    }
                });

                // AirPlay cihazlarını tarama
                jmdns.addServiceListener("_airplay._tcp.local.", new ServiceListener() {
                    @Override
                    public void serviceAdded(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "AirPlay cihazı bulundu: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "AirPlay cihazı kaldırıldı: " + event.getName());
                    }

                    @Override
                    public void serviceResolved(javax.jmdns.ServiceEvent event) {
                        ServiceInfo info = event.getInfo();
                        JSONObject device = new JSONObject();
                        try {
                            device.put("name", info.getName());
                            device.put("address", info.getHostAddresses()[0]);
                            device.put("port", info.getPort());
                            airPlayDevices.put(device);
                            Log.d(TAG, "AirPlay cihazı çözüldü: " + info.getName());
                        } catch (Exception e) {
                            Log.e(TAG, "AirPlay cihazını JSON'a eklerken hata oluştu.", e);
                        }
                    }
                });

                Thread.sleep(timeout);
                jmdns.close();

                result.put("googleCastDevices", googleCastDevices);
                result.put("airPlayDevices", airPlayDevices);

            } catch (Exception e) {
                Log.e(TAG, "Cihazları tararken hata oluştu.", e);
                try {
                    result.put("error", e.getMessage());
                } catch (Exception jsonException) {
                    Log.e(TAG, "Hata mesajını JSON'a eklerken hata oluştu.", jsonException);
                }
            } finally {
                if (multicastLock != null && multicastLock.isHeld()) {
                    multicastLock.release();
                    Log.d(TAG, "MulticastLock serbest bırakıldı.");
                }
                promise.resolve(result.toString());
            }
        }).start();
    }
}
