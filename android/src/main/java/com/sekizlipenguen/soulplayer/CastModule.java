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

import org.json.JSONArray;
import org.json.JSONObject;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(TAG, "CastModule initialized.");
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }
}
