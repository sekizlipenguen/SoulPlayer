package com.sekizlipenguen.soulplayer;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouteSelector;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;

import com.sekizlipenguen.soulplayer.cast.CastDeviceDiscovery;

import org.json.JSONArray;
import org.json.JSONObject;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private final CastDeviceDiscovery castDeviceDiscovery;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.castDeviceDiscovery = new CastDeviceDiscovery(reactContext);
        Log.d(TAG, "CastModule initialized.");
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void discoverDevices(Promise promise) {
        try {
            Log.d(TAG, "Cihaz tarama işlemi başlatılıyor...");
            castDeviceDiscovery.startDiscovery();

            JSONArray deviceArray = new JSONArray();
            for (CastDevice device : castDeviceDiscovery.getAvailableDevices()) {
                JSONObject deviceInfo = new JSONObject();
                deviceInfo.put("name", device.getFriendlyName());
                deviceInfo.put("id", device.getDeviceId());
                deviceArray.put(deviceInfo);
            }

            Log.d(TAG, "Cihaz tarama tamamlandı. Bulunan cihazlar: " + deviceArray.toString());
            promise.resolve(deviceArray.toString());
        } catch (Exception e) {
            Log.e(TAG, "Cihaz tarama işlemi sırasında hata oluştu.", e);
            promise.reject("DISCOVERY_ERROR", e.getMessage());
        }
    }
}
