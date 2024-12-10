package com.sekizlipenguen.soulplayer;

import android.content.Context;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter.RouteGroup;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private final MediaRouter mediaRouter;
    private final List<RouteInfo> availableRoutes = new ArrayList<>();

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mediaRouter = (MediaRouter) reactContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);

        // MulticastLock oluştur ve etkinleştir
        WifiManager wifiManager = (WifiManager) reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("soulplayer_multicast_lock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }

        if (mediaRouter != null) {
            mediaRouter.addCallback(
                MediaRouter.ROUTE_TYPE_LIVE_AUDIO | MediaRouter.ROUTE_TYPE_LIVE_VIDEO,
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteAdded(MediaRouter router, RouteInfo route) {
                        Log.d(TAG, "Route added: " + route.getName());
                        availableRoutes.add(route);
                    }

                    @Override
                    public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                        Log.d(TAG, "Route removed: " + route.getName());
                        availableRoutes.remove(route);
                    }

                    @Override
                    public void onRouteChanged(MediaRouter router, RouteInfo route) {
                        Log.d(TAG, "Route changed: " + route.getName());
                    }

                    @Override
                    public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
                        Log.d(TAG, "Route volume changed: " + route.getName());
                    }

                    @Override
                    public void onRouteGrouped(MediaRouter router, RouteInfo route, RouteGroup group, int index) {
                        Log.d(TAG, "Route grouped: " + route.getName() + " in group: " + group.getName());
                    }

                    @Override
                    public void onRouteUngrouped(MediaRouter router, RouteInfo route, RouteGroup group) {
                        Log.d(TAG, "Route ungrouped: " + route.getName() + " from group: " + group.getName());
                    }

                    @Override
                    public void onRouteSelected(MediaRouter router, int type, RouteInfo route) {
                        Log.d(TAG, "Route selected: " + route.getName());
                    }

                    @Override
                    public void onRouteUnselected(MediaRouter router, int type, RouteInfo route) {
                        Log.d(TAG, "Route unselected: " + route.getName());
                    }
                },
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
            );
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void getAvailableRoutes(Promise promise) {
        try {
            JSONArray routesArray = new JSONArray();
            for (RouteInfo route : availableRoutes) {
                JSONObject routeObject = new JSONObject();
                routeObject.put("name", route.getName().toString());
                routeObject.put("description", route.getDescription() != null ? route.getDescription().toString() : "No description");
                routesArray.put(routeObject);
            }
            promise.resolve(routesArray.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error getting available routes: ", e);
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void selectRoute(String routeName, Promise promise) {
        try {
            for (RouteInfo route : availableRoutes) {
                if (route.getName().equals(routeName)) {
                    mediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, route);
                    promise.resolve("Route selected: " + routeName);
                    return;
                }
            }
            promise.reject("ROUTE_NOT_FOUND", "Route not found: " + routeName);
        } catch (Exception e) {
            Log.e(TAG, "Error selecting route: ", e);
            promise.reject("ERROR", e.getMessage());
        }
    }
}
