package com.sekizlipenguen.soulplayer;

import android.content.Context; // Context işlemleri için
import android.media.MediaRouter; // MediaRouter sınıfı
import android.media.MediaRouter.RouteInfo; // MediaRouter cihaz bilgisi
import android.media.MediaRouter.RouteGroup; // MediaRouter cihaz grupları
import android.net.wifi.WifiManager; // Multicast kilidi için
import android.os.Handler; // Ana thread üzerinde çalışmak için
import android.os.Looper; // Looper, Handler ile birlikte kullanılır
import android.util.Log; // Loglama işlemleri için

import androidx.annotation.NonNull; // NonNull anotasyonu için

import com.facebook.react.bridge.Promise; // React Native köprüsü
import com.facebook.react.bridge.ReactApplicationContext; // React Native uygulama bağlamı
import com.facebook.react.bridge.ReactContextBaseJavaModule; // React Native modül temeli
import com.facebook.react.bridge.ReactMethod; // React Native yöntem tanımı

import com.google.android.gms.cast.framework.CastContext; // Google Cast Framework
import com.google.android.gms.cast.framework.CastSession; // Mevcut oturum bilgisi
import com.google.android.gms.cast.framework.SessionManager; // Oturum yönetimi
import com.google.android.gms.cast.framework.media.RemoteMediaClient; // Medya kontrolü için

import org.json.JSONArray; // JSON array işlemleri için
import org.json.JSONObject; // JSON object işlemleri için

import java.util.ArrayList; // List işlemleri için
import java.util.List; // Java koleksiyonları


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
    new Handler(Looper.getMainLooper()).post(() -> {
        try {
            // Google Cast cihazlarını tarama
            CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
            SessionManager sessionManager = castContext.getSessionManager();
            CastSession castSession = sessionManager.getCurrentCastSession();
            Log.d(TAG, "castSession: " + castSession);

            JSONObject result = new JSONObject();

            if (castSession != null && castSession.isConnected()) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                if (remoteMediaClient != null) {
                    Log.d(TAG, "Google Cast cihazları taranıyor.");
                    result.put("googleCast", "Google Cast cihazları başarıyla taranıyor.");
                } else {
                    Log.e(TAG, "RemoteMediaClient bulunamadı.");
                    result.put("googleCastError", "RemoteMediaClient bulunamadı.");
                }
            } else {
                Log.e(TAG, "Google Cast cihazı bulunamadı.");
                result.put("googleCastError", "Google Cast cihazı bulunamadı.");
            }

            // AirPlay cihazlarını tarama
            try {
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
                        } catch (Exception e) {
                            Log.e(TAG, "AirPlay cihazı JSON oluşturulurken hata oluştu: ", e);
                        }
                        Log.d(TAG, "AirPlay cihazı çözüldü: " + info.getName());
                    }
                });

                // Tarama sonuçlarını döndür
                result.put("airPlayDevices", airPlayDevices);
            } catch (Exception e) {
                Log.e(TAG, "AirPlay cihazlarını tararken hata oluştu: ", e);
                result.put("airPlayError", e.getMessage());
            }

            // Sonuçları promise ile döndür
            promise.resolve(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Google Cast ve AirPlay cihazlarını tararken bir hata oluştu: ", e);
            promise.reject("ERROR", e.getMessage());
        }
    });
}



}
