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
                CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
                SessionManager sessionManager = castContext.getSessionManager();
                CastSession castSession = sessionManager.getCurrentCastSession();

                if (castSession != null && castSession.isConnected()) {
                    RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                    if (remoteMediaClient != null) {
                        Log.d(TAG, "Google Cast cihazları taranıyor.");
                        promise.resolve("Google Cast cihazları başarıyla taranıyor.");
                    } else {
                        Log.e(TAG, "RemoteMediaClient bulunamadı.");
                        promise.reject("REMOTE_MEDIA_CLIENT_NULL", "RemoteMediaClient bulunamadı.");
                    }
                } else {
                    Log.e(TAG, "Google Cast cihazı bulunamadı.");
                    promise.reject("CAST_SESSION_NOT_CONNECTED", "Google Cast cihazı bulunamadı.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Google Cast cihazlarını tararken bir hata oluştu: ", e);
                promise.reject("ERROR", e.getMessage());
            }
        });
    }


}
