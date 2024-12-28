package com.sekizlipenguen.soulplayer.cast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouteSelector;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.UiThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class CastDeviceDiscovery {

    private static final String TAG = "CastDeviceDiscovery";

    private MediaRouter mediaRouter;
    private MediaRouteSelector routeSelector;
    private final List<CastDevice> availableDevices = new ArrayList<>();
    private WifiManager.MulticastLock multicastLock;

    public CastDeviceDiscovery(ReactApplicationContext reactContext) {
        Context context = reactContext.getApplicationContext();

        // MulticastLock ayarı
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("multicastLock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
            Log.d(TAG, "MulticastLock alındı.");
        } else {
            throw new IllegalStateException("WifiManager alınamadı");
        }

        // MediaRouter ve MediaRouteSelector ayarları
        UiThreadUtil.runOnUiThread(() -> {
            mediaRouter = MediaRouter.getInstance(context);
            routeSelector = new MediaRouteSelector.Builder()
                    .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)) // Varsayılan Cast ID
                    .build();
                         for (MediaRouter.RouteInfo route : mediaRouter.getRoutes()) {
                                Log.d(TAG, "Route: " + route.getName());
                                if (route.getExtras() != null) {
                                    Log.d(TAG, "Route Extras: " + route.getExtras().toString());
                                }
                            }
        });
    }

    public void startDiscovery() {
        Log.d(TAG, "Cihaz tarama başlatılıyor...");

        UiThreadUtil.runOnUiThread(() -> {
            mediaRouter.addCallback(routeSelector, new MediaRouter.Callback() {
                @Override
                public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "Route eklendi: " + route.getName());
                    if (route.getExtras() != null) {
                        Log.d(TAG, "Route extras: " + route.getExtras().toString());
                    } else {
                        Log.d(TAG, "Route extras null.");
                    }

                    CastDevice device = CastDevice.getFromBundle(route.getExtras());
                    if (device != null && !availableDevices.contains(device)) {
                        availableDevices.add(device);
                        Log.d(TAG, "Yeni cihaz bulundu: " + device.getFriendlyName());
                    } else {
                        Log.d(TAG, "CastDevice null veya zaten mevcut, atlanıyor: " + route.getName());
                    }
                }

                @Override
                public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "Route kaldırıldı: " + route.getName());
                    CastDevice device = CastDevice.getFromBundle(route.getExtras());

                    if (device != null) {
                        availableDevices.remove(device);
                        Log.d(TAG, "Cihaz kaldırıldı: " + device.getFriendlyName());
                    }
                }

                @Override
                public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "Route değişti: " + route.getName());
                    CastDevice device = CastDevice.getFromBundle(route.getExtras());

                    if (device != null) {
                        Log.d(TAG, "Cihaz güncellendi: " + device.getFriendlyName());
                    }
                }

                @Override
                public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "Route seçildi: " + route.getName());
                    CastDevice device = CastDevice.getFromBundle(route.getExtras());

                    if (device != null) {
                        Log.d(TAG, "Seçilen cihaz: " + device.getFriendlyName());
                    }
                }

                @Override
                public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
                    Log.d(TAG, "Route seçimi kaldırıldı: " + route.getName());
                    CastDevice device = CastDevice.getFromBundle(route.getExtras());

                    if (device != null) {
                        Log.d(TAG, "Seçimi kaldırılan cihaz: " + device.getFriendlyName());
                    }
                }
            }, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
            Log.d(TAG, "MediaRouter callback ekleme işlemi tamamlandı.");
        });
    }

    public void stopDiscovery() {
        Log.d(TAG, "Cihaz tarama durduruluyor...");
        UiThreadUtil.runOnUiThread(() -> {
            mediaRouter.removeCallback(new MediaRouter.Callback() {
            });
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
                Log.d(TAG, "MulticastLock serbest bırakıldı.");
            }
        });
    }

    public List<CastDevice> getAvailableDevices() {
        Log.d(TAG, "Bulunan cihaz sayısı: " + availableDevices.size());
        for (CastDevice device : availableDevices) {
            Log.d(TAG, "Cihaz: " + device.getFriendlyName() + " - ID: " + device.getDeviceId());
        }
        return availableDevices;
    }
}
