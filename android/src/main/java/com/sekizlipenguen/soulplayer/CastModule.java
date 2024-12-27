package com.sekizlipenguen.soulplayer;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.framework.SessionManagerListener;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import android.content.Context;
import android.net.wifi.WifiManager;

public class CastModule extends ReactContextBaseJavaModule {

    private static final String TAG = "CastModule";
    private WifiManager.MulticastLock multicastLock;

    public CastModule(ReactApplicationContext reactContext) {
        super(reactContext);
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
                        Log.d(TAG, "Yerel IP Adresi: " + address.getHostAddress());
                        return address;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Yerel IP adresi alınırken hata oluştu.", e);
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
                    Log.e(TAG, "Yerel IP adresi bulunamadı.");
                    promise.reject("NETWORK_ERROR", "Local IP address not found.");
                    return;
                }

                Log.d(TAG, "JmDNS başlatılıyor. Yerel IP: " + localAddress.getHostAddress());
                jmdns = JmDNS.create(localAddress);
                jmdns.addServiceListener("_googlecast._tcp.local.", new ServiceListener() {
                    @Override
                    public void serviceAdded(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast cihazı bulundu: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "Google Cast cihazı kaldırıldı: " + event.getName());
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
                            Log.d(TAG, "Google Cast cihazı çözüldü: " + device.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Google Cast cihazını JSON'a eklerken hata oluştu.", e);
                        }
                    }
                });

                Thread.sleep(timeout);
                jmdns.close();

                promise.resolve(googleCastDevices.toString());

            } catch (Exception e) {
                Log.e(TAG, "Cihazları tararken hata oluştu.", e);
                promise.reject("SCAN_ERROR", e.getMessage());
            } finally {
                if (multicastLock != null && multicastLock.isHeld()) {
                    multicastLock.release();
                }
            }
        }).start();
    }

    @ReactMethod
    public void connectToDevice(String deviceAddress, Promise promise) {
        try {
            CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
            CastSession currentSession = castContext.getSessionManager().getCurrentCastSession();

            // Eğer belirtilen cihaz zaten bağlıysa
            if (currentSession != null && currentSession.isConnected()) {
                String connectedDeviceAddress = currentSession.getCastDevice().getIpAddress();
                if (deviceAddress.equals(connectedDeviceAddress)) {
                    Log.d(TAG, "Zaten bağlı: " + deviceAddress);
                    promise.resolve("Device already connected: " + deviceAddress);
                    return;
                } else {
                    Log.d(TAG, "Başka bir cihaz bağlı: " + connectedDeviceAddress);
                    promise.reject("DEVICE_ALREADY_CONNECTED", "Another device is connected: " + connectedDeviceAddress);
                    return;
                }
            }

            // Yeni bir cihaz bağlanıyor
            Log.d(TAG, "Yeni cihaz bağlanıyor: " + deviceAddress);
            castContext.getSessionManager().addSessionManagerListener(new SessionManagerListener<CastSession>() {
                @Override
                public void onSessionStarted(CastSession session, String sessionId) {
                    if (session.getCastDevice().getIpAddress().equals(deviceAddress)) {
                        Log.d(TAG, "Cihaz bağlandı: " + deviceAddress);
                        promise.resolve("Device connected: " + deviceAddress);
                    }
                }

                @Override
                public void onSessionEnded(CastSession session, int error) {
                    Log.e(TAG, "Oturum sonlandırıldı. Hata kodu: " + error);
                    promise.reject("SESSION_ENDED", "Session ended with error code: " + error);
                }

                @Override
                public void onSessionResumed(CastSession session, boolean wasSuspended) {
                    Log.d(TAG, "Oturum yeniden başlatıldı.");
                }
            }, CastSession.class);

            // Oturumu başlatma işlemi
            castContext.getSessionManager().startSession();
        } catch (Exception e) {
            Log.e(TAG, "Cihaza bağlanırken hata oluştu.", e);
            promise.reject("CONNECT_DEVICE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void sendMediaToDevice(String deviceAddress, String mediaUrl, Promise promise) {
        new Thread(() -> {
            try {
                CastContext castContext = CastContext.getSharedInstance(getReactApplicationContext());
                CastSession castSession = castContext.getSessionManager().getCurrentCastSession();

                if (castSession != null && castSession.isConnected() && castSession.getCastDevice().getIpAddress().equals(deviceAddress)) {
                    RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
                    if (remoteMediaClient != null) {
                        MediaInfo mediaInfo = new MediaInfo.Builder(mediaUrl)
                                .setContentType("video/mp4")
                                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                                .build();

                        remoteMediaClient.load(mediaInfo, true, 0);
                        Log.d(TAG, "Medya gönderildi: " + mediaUrl);
                        promise.resolve("Media started playing on device: " + deviceAddress);
                    } else {
                        Log.e(TAG, "RemoteMediaClient is null.");
                        promise.reject("REMOTE_MEDIA_CLIENT_ERROR", "RemoteMediaClient is null.");
                    }
                } else {
                    Log.e(TAG, "Cihaz bağlantısı yok veya yanlış cihaz: " + deviceAddress);
                    promise.reject("CAST_SESSION_ERROR", "No active Cast session or wrong device.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Medya gönderimi sırasında hata oluştu.", e);
                promise.reject("SEND_MEDIA_ERROR", e.getMessage());
            }
        }).start();
    }
}
