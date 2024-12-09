package com.sekizlipenguen.soulplayer;

import android.content.Context;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class CastModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    /**
     * Videoyu Chromecast'e gönderir.
     */
    @ReactMethod
    public void startCasting(String url, String title, String subtitle, Promise promise) {
        try {
            CastContext castContext = CastContext.getSharedInstance(reactContext);
            CastSession castSession = castContext.getSessionManager().getCurrentCastSession();

            if (castSession != null && castSession.isConnected()) {
                RemoteMediaClient mediaClient = castSession.getRemoteMediaClient();

                if (mediaClient != null) {
                    MediaInfo mediaInfo = new MediaInfo.Builder(url)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setContentType("video/mp4")
                            .build();

                    MediaLoadRequestData requestData = new MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .build();

                    mediaClient.load(requestData);
                    promise.resolve("Casting started successfully");
                } else {
                    promise.reject("Error", "Media client is null");
                }
            } else {
                promise.reject("Error", "No Cast session available");
            }
        } catch (Exception e) {
            promise.reject("Error", e.getMessage());
        }
    }

    /**
     * Casting işlemini durdurur.
     */
    @ReactMethod
    public void stopCasting(Promise promise) {
        try {
            CastContext castContext = CastContext.getSharedInstance(reactContext);
            SessionManager sessionManager = castContext.getSessionManager();
            sessionManager.endCurrentSession(true);
            promise.resolve("Casting stopped successfully");
        } catch (Exception e) {
            promise.reject("Error", e.getMessage());
        }
    }

    /**
     * Videoyu belirli bir zamana atlar.
     */
    @ReactMethod
    public void seekTo(double seconds, Promise promise) {
        try {
            CastContext castContext = CastContext.getSharedInstance(reactContext);
            CastSession castSession = castContext.getSessionManager().getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient mediaClient = castSession.getRemoteMediaClient();
                if (mediaClient != null) {
                    mediaClient.seek((long) (seconds * 1000));
                    promise.resolve("Seek completed");
                } else {
                    promise.reject("Error", "Media client is null");
                }
            } else {
                promise.reject("Error", "No Cast session available");
            }
        } catch (Exception e) {
            promise.reject("Error", e.getMessage());
        }
    }

    /**
     * Oynatma hızını ayarlar.
     */
    @ReactMethod
    public void setPlaybackRate(float rate, Promise promise) {
        try {
            CastContext castContext = CastContext.getSharedInstance(reactContext);
            CastSession castSession = castContext.getSessionManager().getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient mediaClient = castSession.getRemoteMediaClient();
                if (mediaClient != null) {
                    mediaClient.setPlaybackRate(rate);
                    promise.resolve("Playback rate updated");
                } else {
                    promise.reject("Error", "Media client is null");
                }
            } else {
                promise.reject("Error", "No Cast session available");
            }
        } catch (Exception e) {
            promise.reject("Error", e.getMessage());
        }
    }
}
