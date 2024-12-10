package com.sekizlipenguen.soulplayer;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class CastModule extends ReactContextBaseJavaModule {
    private SessionManager sessionManager;
    private CastSession castSession;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        CastContext castContext = CastContext.getSharedInstance(reactContext);
        sessionManager = castContext.getSessionManager();
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void startCasting(String videoUrl, Promise promise) {
        castSession = sessionManager.getCurrentCastSession();

        if (castSession != null && castSession.isConnected()) {
            RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
            if (remoteMediaClient != null) {
                MediaInfo mediaInfo = new MediaInfo.Builder(videoUrl)
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType("video/mp4")
                        .build();

                MediaLoadOptions options = new MediaLoadOptions.Builder().build();
                remoteMediaClient.load(mediaInfo, options);
                promise.resolve("Casting started");
                return;
            }
        }
        promise.reject("NO_CAST_SESSION", "No active cast session found");
    }

    @ReactMethod
    public void stopCasting(Promise promise) {
        if (castSession != null && castSession.isConnected()) {
            castSession.endSession(true);
            promise.resolve("Casting stopped");
        } else {
            promise.reject("NO_CAST_SESSION", "No active cast session found");
        }
    }
}
