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
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        // MulticastLock oluştur ve etkinleştir
        WifiManager wifiManager = (WifiManager) reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("soulplayer_multicast_lock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void scanForCastDevices(Promise promise) {
        // Google Cast cihazlarını ana thread'de tarama
        new Handler(Looper.getMainLooper()).post(() -> {
            JSONObject result = new JSONObject();

            try {
                Log.d(TAG, "Google Cast cihazlarını tarama başlıyor...");
                CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
                SessionManager sessionManager = castContext.getSessionManager();
                CastSession castSession = sessionManager.getCurrentCastSession();

                if (castSession != null && castSession.isConnected()) {
                    RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                    if (remoteMediaClient != null) {
                        Log.d(TAG, "Google Cast cihazları tarandı.");
                        result.put("googleCast", "Google Cast cihazları başarıyla bulundu.");
                    } else {
                        Log.e(TAG, "RemoteMediaClient bulunamadı.");
                        result.put("googleCastError", "RemoteMediaClient bulunamadı.");
                    }
                } else {
                    Log.e(TAG, "Google Cast cihazı bağlanmadı.");
                    result.put("googleCastError", "Google Cast cihazı bağlanmadı.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Google Cast cihazlarını tararken bir hata oluştu: ", e);
                try {
                    result.put("googleCastError", e.getMessage());
                } catch (Exception jsonException) {
                    Log.e(TAG, "Google Cast hata mesajı JSON'a eklenirken hata oluştu: ", jsonException);
                }
            }

            // AirPlay cihazlarını arka planda tarama
            new Thread(() -> {
                try {
                    Log.d(TAG, "AirPlay cihazlarını tarama başlıyor...");
                    JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
                    JSONArray airPlayDevices = new JSONArray();

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
                                Log.e(TAG, "AirPlay cihazı JSON oluşturulurken hata oluştu: ", e);
                            }
                        }
                    });

                    Thread.sleep(5000); // Tarama için bekleme süresi
                    result.put("airPlayDevices", airPlayDevices);
                } catch (Exception e) {
                    Log.e(TAG, "AirPlay cihazlarını tararken hata oluştu: ", e);
                    try {
                        result.put("airPlayError", e.getMessage());
                    } catch (Exception jsonException) {
                        Log.e(TAG, "AirPlay hata mesajı JSON'a eklenirken hata oluştu: ", jsonException);
                    }
                } finally {
                    promise.resolve(result.toString());
                }
            }).start();
        });
    }
}
