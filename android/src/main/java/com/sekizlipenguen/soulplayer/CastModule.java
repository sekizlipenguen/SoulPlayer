package com.sekizlipenguen.soulplayer;

import android.content.Context;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter.RouteGroup;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class CastModule extends ReactContextBaseJavaModule {
    private static final String TAG = "CastModule";

    public CastModule(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void showCastPicker(final Callback successCallback, final Callback errorCallback) {
        try {
            Context context = getReactApplicationContext();
            MediaRouter mediaRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);

            if (mediaRouter == null) {
                errorCallback.invoke("MediaRouter is not available on this device.");
                return;
            }

            // Emülatör olup olmadığını kontrol et
            if ("generic".equals(android.os.Build.BRAND.toLowerCase())) {
                errorCallback.invoke("Casting is not supported on the emulator.");
                return;
            }

            if (mediaRouter != null) {
                mediaRouter.addCallback(
                    MediaRouter.ROUTE_TYPE_LIVE_VIDEO,
                    new MediaRouter.Callback() {
                        @Override
                        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo route) {
                            Log.d(TAG, "Route selected: " + route.getName());
                            successCallback.invoke("Casting started on: " + route.getName());
                        }

                          @Override
                          public void onRouteUnselected(MediaRouter router, int type, RouteInfo route) {
                              Log.d(TAG, "Route unselected: " + route.getName());
                          }


                        @Override
                        public void onRouteAdded(MediaRouter router, RouteInfo route) {
                            Log.d(TAG, "Route added: " + route.getName());
                        }

                        @Override
                        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                            Log.d(TAG, "Route removed: " + route.getName());
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
                    },
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
                );
            } else {
                errorCallback.invoke("MediaRouter is not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing cast picker: " + e.getMessage(), e);
            errorCallback.invoke("Error: " + e.getMessage());
        }
    }
}
