package com.sekizlipenguen.soulplayer;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.MediaInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import android.content.Intent;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private WifiManager.MulticastLock multicastLock;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(TAG, "CastModule initialized.");
    }

    @NonNull
    @Override
    public String getName() {
        return "CastModule";
    }

    private InetAddress getLocalInetAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        Log.d(TAG, "Found local IP address: " + address.getHostAddress());
                        return address;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while getting local IP address.", e);
        }
        return null;
    }

    @ReactMethod
    public void scanForDevices(final int timeout, final Promise promise) {
        JSONArray googleCastDevices = new JSONArray();

        new Thread(() -> {
            JmDNS jmdns = null;
            try {
                InetAddress localAddress = getLocalInetAddress();
                if (localAddress == null) {
                    Log.e(TAG, "Local IP address not found.");
                    promise.reject("NETWORK_ERROR", "Local IP address not found.");
                    return;
                }

                WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    multicastLock = wifiManager.createMulticastLock("soulplayer_multicast_lock");
                    multicastLock.setReferenceCounted(true);
                    multicastLock.acquire();
                    Log.d(TAG, "MulticastLock acquired.");
                } else {
                    Log.e(TAG, "WifiManager is null. Cannot acquire MulticastLock.");
                    promise.reject("WIFI_MANAGER_ERROR", "WifiManager is null.");
                    return;
                }

                Log.d(TAG, "Starting JmDNS with IP: " + localAddress.getHostAddress());
                jmdns = JmDNS.create(localAddress);
                jmdns.addServiceListener("_googlecast._tcp.local.", new ServiceListener() {
                    @Override
                    public void serviceAdded(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast device added: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast device removed: " + event.getName());
                    }

                    @Override
                    public void serviceResolved(javax.jmdns.ServiceEvent event) {
                        ServiceInfo info = event.getInfo();
                        try {
                            JSONObject device = new JSONObject();
                            device.put("name", info.getName());
                            device.put("address", info.getHostAddresses()[0]);
                            device.put("port", info.getPort());
                            googleCastDevices.put(device);
                            Log.d(TAG, "Google Cast device resolved: " + device.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding Google Cast device to JSON.", e);
                        }
                    }
                });

                Thread.sleep(timeout);
                jmdns.close();

                promise.resolve(googleCastDevices.toString());
                Log.d(TAG, "Device scan complete.");

            } catch (Exception e) {
                Log.e(TAG, "Error during device scan.", e);
                promise.reject("SCAN_ERROR", e.getMessage());
            } finally {
                if (multicastLock != null && multicastLock.isHeld()) {
                    multicastLock.release();
                    Log.d(TAG, "MulticastLock released.");
                }
            }
        }).start();
    }

@ReactMethod
public void connectToDevice(String deviceAddress, Promise promise) {
    getReactApplicationContext().runOnUiQueueThread(() -> {
        try {
            Log.d(TAG, "Connecting to device: " + deviceAddress);

            // CastContext'i UI iş parçacığında alın
            CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
            CastSession currentSession = castContext.getSessionManager().getCurrentCastSession();

            if (currentSession != null && currentSession.isConnected()) {
                String connectedDeviceAddress = currentSession.getCastDevice().getIpAddress().getHostAddress();
                if (deviceAddress.equals(connectedDeviceAddress)) {
                    Log.d(TAG, "Already connected to device: " + deviceAddress);
                    promise.resolve("Device already connected: " + deviceAddress);
                    return;
                } else {
                    Log.d(TAG, "Different device connected: " + connectedDeviceAddress);
                    castContext.getSessionManager().endCurrentSession(true);
                }
            }
             // Yeni bir Intent oluştur
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra("DEVICE_ADDRESS", deviceAddress);

            // Yeni oturumu başlat
            Log.d(TAG, "Starting a new Cast session...");
            castContext.getSessionManager().startSession(intent);
            castContext.getSessionManager().addSessionManagerListener(new SessionManagerListener<CastSession>() {
                @Override
                public void onSessionStarting(CastSession session) {
                    Log.d(TAG, "Session is starting: " + session);
                }

                @Override
                public void onSessionStartFailed(CastSession session, int error) {
                    Log.e(TAG, "Session start failed with error code: " + error);
                    promise.reject("SESSION_START_FAILED", "Failed to start session with error code: " + error);
                }

                @Override
                public void onSessionStarted(CastSession session, String sessionId) {
                    Log.d(TAG, "Connected to device: " + deviceAddress);
                    promise.resolve("Device connected: " + deviceAddress);
                }

                @Override
                public void onSessionEnding(CastSession session) {
                    Log.d(TAG, "Session is ending: " + session);
                }

                @Override
                public void onSessionEnded(CastSession session, int error) {
                    Log.e(TAG, "Session ended with error code: " + error);
                }

                @Override
                public void onSessionResuming(CastSession session, String sessionId) {
                    Log.d(TAG, "Session is resuming: " + sessionId);
                }

                @Override
                public void onSessionResumeFailed(CastSession session, int error) {
                    Log.e(TAG, "Session resume failed with error code: " + error);
                }

                @Override
                public void onSessionResumed(CastSession session, boolean wasSuspended) {
                    Log.d(TAG, "Session resumed.");
                }

                @Override
                public void onSessionSuspended(CastSession session, int reason) {
                    Log.d(TAG, "Session suspended. Reason: " + reason);
                }
            }, CastSession.class);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device.", e);
            promise.reject("CONNECT_DEVICE_ERROR", e.getMessage());
        }
    });
}


    @ReactMethod
    public void sendMediaToDevice(String deviceAddress, String mediaUrl, Promise promise) {
        getReactApplicationContext().runOnUiQueueThread(() -> {
            try {
                Log.d(TAG, "Sending media to device: " + deviceAddress);

                // CastContext ve mevcut oturum kontrolü
                CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
                CastSession castSession = castContext.getSessionManager().getCurrentCastSession();

                if (castSession == null || !castSession.isConnected()) {
                    Log.e(TAG, "No active session. Starting a new session.");
                    connectToDevice(deviceAddress, promise);
                    return;
                }

                if (!castSession.getCastDevice().getIpAddress().getHostAddress().equals(deviceAddress)) {
                    Log.e(TAG, "Device mismatch. Starting new session.");
                    connectToDevice(deviceAddress, promise);
                    return;
                }

                RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                if (remoteMediaClient != null) {
                    // HLS formatı için uygun MediaInfo
                    MediaInfo mediaInfo = new MediaInfo.Builder(mediaUrl)
                            .setContentType("application/x-mpegURL") // HLS içerik türü
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .build();

                    // Medya yükleme
                    remoteMediaClient.load(mediaInfo, true, 0);
                    Log.d(TAG, "Media sent to device: " + mediaUrl);
                    promise.resolve("Media started playing on device: " + deviceAddress);
                } else {
                    Log.e(TAG, "RemoteMediaClient is null.");
                    promise.reject("REMOTE_MEDIA_CLIENT_ERROR", "RemoteMediaClient is null.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending media to device.", e);
                promise.reject("SEND_MEDIA_ERROR", e.getMessage());
            }
        });
    }

}
