package com.sekizlipenguen.soulplayer;

import android.util.Log;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private CastSession currentSession;
    private String pendingUrl = null;
    private String pendingTitle = null;
    private String pendingMimeType = null;
    private static double lastKnownCurrentTime = 0.0;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(TAG, "CastModule initialized.");

        UiThreadUtil.runOnUiThread(() -> {
            CastContext.getSharedInstance(reactContext).getSessionManager().addSessionManagerListener(
                sessionManagerListener, CastSession.class
            );
        });
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    @ReactMethod
    public void showCastDialog() {
        UiThreadUtil.runOnUiThread(() -> {
            try {
                CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
                castContext.getSessionManager().endCurrentSession(true); // Aktif oturum varsa sonlandır

                // MediaRouteChooserDialog aç
                androidx.mediarouter.app.MediaRouteChooserDialog dialog = new androidx.mediarouter.app.MediaRouteChooserDialog(getCurrentActivity());
                dialog.setRouteSelector(new androidx.mediarouter.media.MediaRouteSelector.Builder().addControlCategory(com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)).build());
                dialog.show();
                Log.d(TAG, "Cast dialog opened using MediaRouteChooserDialog.");
            } catch (Exception e) {
                Log.e(TAG, "Error showing Cast dialog", e);
            }
        });
    }

    @ReactMethod
    public void showControllerDialog() {
        UiThreadUtil.runOnUiThread(() -> {
            try {
                // MediaRouteControllerDialog'u aç
                androidx.mediarouter.app.MediaRouteControllerDialog dialog = new androidx.mediarouter.app.MediaRouteControllerDialog(getCurrentActivity());
                dialog.show();
                Log.d(TAG, "Controller dialog opened using MediaRouteControllerDialog.");
            } catch (Exception e) {
                Log.e(TAG, "Error showing Controller dialog", e);
            }
        });
    }


    @ReactMethod
    public void addListener(String eventName) {
        // Dinleyici eklemek için kullanılan boş metod (gereklidir)
        Log.d(TAG, "addListener: Event listener added for event: " + eventName);
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Dinleyici kaldırmak için kullanılan boş metod (gereklidir)
        Log.d(TAG, "removeListeners: Removed " + count + " listeners");
    }

    private final SessionManagerListener<CastSession> sessionManagerListener = new SessionManagerListener<CastSession>() {
        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            Log.d(TAG, "Cast session started.");
            currentSession = session;
            sendEvent("onSessionStarted", createSessionParams(session));

            if (pendingUrl != null) {
                playMedia(pendingUrl, pendingTitle, pendingMimeType);
                clearPendingMedia();
            }
        }

        @Override
        public void onSessionEnded(CastSession session, int error) {
            Log.d(TAG, "Cast session ended.");
            currentSession = null;
            sendEvent("onSessionEnded", createSessionParams(session));
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
            Log.d(TAG, "Cast session suspended.");
            sendEvent("onSessionSuspended", createSessionParams(session));
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            Log.d(TAG, "Cast session resumed.");
            currentSession = session;
            sendEvent("onSessionResumed", createSessionParams(session));
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
            Log.e(TAG, "Cast session resume failed.");
            sendEvent("onSessionResumeFailed", createErrorParams(error));
        }

        @Override
        public void onSessionStarting(CastSession session) {
            Log.d(TAG, "Cast session starting.");
            sendEvent("onSessionStarting", createSessionParams(session));
        }

        @Override
        public void onSessionEnding(CastSession session) {
            Log.d(TAG, "Cast session ending.");
            sendEvent("onSessionEnding", createSessionParams(session));
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
            Log.e(TAG, "Cast session start failed.");
            sendEvent("onSessionStartFailed", createErrorParams(error));
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
            Log.d(TAG, "Cast session resuming.");
            sendEvent("onSessionResuming", createSessionParams(session));
        }
    };

    @ReactMethod
    public void getCurrentTime(Promise promise) {
        UiThreadUtil.runOnUiThread(() -> {
            try {
                CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                        .getSessionManager()
                        .getCurrentCastSession();

                if (castSession != null) {
                    RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                    if (remoteMediaClient != null) {
                        long currentTimeMillis = remoteMediaClient.getApproximateStreamPosition();
                        double currentTimeSeconds = currentTimeMillis / 1000.0; // Saniyeye çevir
                        Log.d(TAG, "Current playback time: " + currentTimeSeconds + " seconds");
                        promise.resolve(currentTimeSeconds);
                    } else {
                        Log.e(TAG, "RemoteMediaClient is null.");
                        promise.reject("NO_MEDIA_CLIENT", "RemoteMediaClient is not available.");
                    }
                } else {
                    Log.e(TAG, "No active Cast session.");
                    promise.reject("NO_CAST_SESSION", "No active Cast session.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting current time", e);
                promise.reject("ERROR", e.getMessage());
            }
        });
    }

    @ReactMethod
    public void playMedia(String url, String title, String mimeType) {
        UiThreadUtil.runOnUiThread(() -> {
            CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                    .getSessionManager()
                    .getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                if (remoteMediaClient != null) {
                    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                    mediaMetadata.putString(MediaMetadata.KEY_TITLE, title);

                    MediaInfo mediaInfo = new MediaInfo.Builder(url)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setContentType(mimeType)
                            .setMetadata(mediaMetadata)
                            .build();

                    remoteMediaClient.load(mediaInfo, true, 0);
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                }
            } else {
                Log.d(TAG, "No active Cast session. Saving media info for later.");
                pendingUrl = url;
                pendingTitle = title;
                pendingMimeType = mimeType;
            }
        });
    }

    @ReactMethod
    public void seekTo(double positionInSeconds) {
        UiThreadUtil.runOnUiThread(() -> {
            CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                    .getSessionManager()
                    .getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                if (remoteMediaClient != null) {
                    long positionInMillis = (long) (positionInSeconds * 1000); // Milisaniye cinsine çevir
                    remoteMediaClient.seek(positionInMillis);
                    Log.d(TAG, "SeekTo command sent to position: " + positionInMillis + "ms");
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                }
            } else {
                Log.e(TAG, "No active Cast session.");
            }
        });
    }

    @ReactMethod
    public void togglePlayPause() {
        UiThreadUtil.runOnUiThread(() -> {
            CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                    .getSessionManager()
                    .getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                if (remoteMediaClient != null) {
                    if (remoteMediaClient.isPlaying()) {
                        remoteMediaClient.pause();
                        Log.d(TAG, "Pause command sent.");
                    } else {
                        remoteMediaClient.play();
                        Log.d(TAG, "Play command sent.");
                    }
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                }
            } else {
                Log.e(TAG, "No active Cast session.");
            }
        });
    }


    @ReactMethod
    public void play() {
        UiThreadUtil.runOnUiThread(() -> {
            CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                    .getSessionManager()
                    .getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                if (remoteMediaClient != null) {
                    remoteMediaClient.play();
                    Log.d(TAG, "Play command sent.");
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                }
            } else {
                Log.e(TAG, "No active Cast session.");
            }
        });
    }

    @ReactMethod
    public void pause() {
        UiThreadUtil.runOnUiThread(() -> {
            CastSession castSession = CastContext.getSharedInstance(getReactApplicationContext())
                    .getSessionManager()
                    .getCurrentCastSession();

            if (castSession != null) {
                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();

                if (remoteMediaClient != null) {
                    remoteMediaClient.pause();
                    Log.d(TAG, "Pause command sent.");
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                }
            } else {
                Log.e(TAG, "No active Cast session.");
            }
        });
    }

    private void clearPendingMedia() {
        pendingUrl = null;
        pendingTitle = null;
        pendingMimeType = null;
    }

    private void sendEvent(String eventName, @Nullable JSONObject params) {
        if (getReactApplicationContext().hasActiveCatalystInstance()) {
            getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params != null ? params.toString() : null);
        }
    }

private JSONObject createSessionParams(CastSession session) {
    JSONObject params = new JSONObject();
    try {
        if (session != null) {
            if (session.getCastDevice() != null) {
                params.put("deviceName", session.getCastDevice().getFriendlyName());
                params.put("deviceId", session.getCastDevice().getDeviceId());
                params.put("deviceModel", session.getCastDevice().getModelName());
            }
            params.put("sessionId", session.getSessionId());
            params.put("isConnected", session.isConnected());

            // currentTime bilgisi ekleniyor
            RemoteMediaClient remoteMediaClient = session.getRemoteMediaClient();
            if (remoteMediaClient != null) {
                Log.d(TAG, "RemoteMediaClient is not null.");
                Log.d(TAG, "RemoteMediaClient isPlaying: " + remoteMediaClient.isPlaying());
                Log.d(TAG, "RemoteMediaClient ApproximateStreamPosition: " + remoteMediaClient.getApproximateStreamPosition());
                Log.d(TAG, "RemoteMediaClient MediaStatus: " + remoteMediaClient.getMediaStatus());

                if (remoteMediaClient.isPlaying()) {
                    long currentTime = remoteMediaClient.getApproximateStreamPosition();
                    params.put("currentTime", currentTime / 1000.0); // Saniye cinsine çevir
                } else {
                    params.put("currentTime", 0);
                }
            } else {
                Log.d(TAG, "RemoteMediaClient is null.");
                params.put("currentTime", 0); // Eğer RemoteMediaClient yoksa 0 olarak ekle
            }
        }
    } catch (JSONException e) {
        Log.e(TAG, "Error creating session params", e);
    }
    return params;
}



    private JSONObject createErrorParams(int errorCode) {
        JSONObject params = new JSONObject();
        try {
            params.put("errorCode", errorCode);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error params", e);
        }
        return params;
    }
}
