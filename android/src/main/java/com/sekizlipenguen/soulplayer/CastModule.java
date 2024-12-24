package com.sekizlipenguen.soulplayer;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


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
        JSONObject result = new JSONObject();
        JSONArray googleCastDevices = new JSONArray();
        JSONArray airPlayDevices = new JSONArray();

        try {
            WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("soulplayer_multicast_lock");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
                Log.d(TAG, "MulticastLock başarıyla etkinleştirildi.");
            } else {
                Log.e(TAG, "WifiManager alınamadı. MulticastLock oluşturulamadı.");
                promise.reject("WIFI_MANAGER_ERROR", "WifiManager alınamadı.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "MulticastLock işlemi sırasında hata oluştu.", e);
            promise.reject("MULTICAST_LOCK_ERROR", e.getMessage());
            return;
        }

        new Thread(() -> {
            JmDNS jmdns = null;
            try {
                InetAddress localAddress = getLocalInetAddress();
                if (localAddress == null) {
                    Log.e(TAG, "Yerel IP adresi alınamadı, JmDNS başlatılamadı.");
                    promise.reject("NETWORK_ERROR", "Yerel IP adresi alınamadı.");
                    return;
                }
                jmdns = JmDNS.create(localAddress);
                Log.d(TAG, "JmDNS başlatıldı: " + localAddress.getHostAddress());

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
                        JSONObject device = new JSONObject();
                        try {
                            device.put("name", info.getName());
                            device.put("address", info.getHostAddresses()[0]);
                            device.put("port", info.getPort());
                            googleCastDevices.put(device);
                        } catch (Exception e) {
                            Log.e(TAG, "Google Cast cihazını JSON'a eklerken hata oluştu.", e);
                        }
                    }
                });

                jmdns.addServiceListener("_airplay._tcp.local.", new ServiceListener() {
                    @Override
                    public void serviceAdded(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "AirPlay cihazı bulundu: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(javax.jmdns.ServiceEvent event) {
                        Log.d(TAG, "AirPlay cihazı kaldırıldı: " + event.getName());
                    }

                    @Override
                    public void serviceResolved(javax.jmdns.ServiceEvent event) {
                        ServiceInfo info = event.getInfo();
                        JSONObject device = new JSONObject();
                        try {
                            String[] addresses = info.getHostAddresses();

                            String deviceAddress = null;
                            for (String addr : addresses) {
                                if (addr.contains(".")) { // Sadece IPv4 adresleri
                                    deviceAddress = addr;
                                    break;
                                }
                            }
                            if (deviceAddress != null) {
                                int devicePort = info.getPort();
                                String publicKey = fetchPublicKey(deviceAddress, devicePort);
                                Log.d(TAG, "Fetched Public Key (Base64): " + publicKey);
                                String authToken = generateAuthorizationToken(publicKey, "AirPlayClient");

                                device.put("name", info.getName());
                                device.put("address", deviceAddress);
                                device.put("port", devicePort);
                                device.put("authorizationKey", authToken);

                                airPlayDevices.put(device);
                                Log.d(TAG, "AirPlay cihazı çözüldü: " + info.getName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "AirPlay cihazını JSON'a eklerken hata oluştu.", e);
                        }
                    }
                });

                Thread.sleep(timeout);
                jmdns.close();

                result.put("googleCastDevices", googleCastDevices);
                result.put("airPlayDevices", airPlayDevices);

            } catch (Exception e) {
                Log.e(TAG, "Cihazları tararken hata oluştu.", e);
                try {
                    result.put("error", e.getMessage());
                } catch (Exception jsonException) {
                    Log.e(TAG, "Hata mesajını JSON'a eklerken hata oluştu.", jsonException);
                }
            } finally {
                if (multicastLock != null && multicastLock.isHeld()) {
                    multicastLock.release();
                }
                promise.resolve(result.toString());
            }
        }).start();
    }

   private String fetchPublicKey(String address, int port) {
       try {
           Log.d(TAG, "Fetching public key from: http://" + address + ":" + port + "/info");

           // Prepare the URL and connection
           URL url = new URL("http://" + address + ":" + port + "/info");
           HttpURLConnection connection = (HttpURLConnection) url.openConnection();
           connection.setRequestMethod("GET");
           connection.setRequestProperty("User-Agent", "AirPlay/1.0");
           connection.setRequestProperty("Accept", "application/x-apple-binary-plist");
           connection.setConnectTimeout(5000);
           connection.setReadTimeout(5000);

           int responseCode = connection.getResponseCode();
           Log.d(TAG, "Response Code: " + responseCode);

           if (responseCode == HttpURLConnection.HTTP_OK) {
               Log.d(TAG, "Response OK, trying to read InputStream...");

               try (InputStream inputStream = connection.getInputStream()) {
                   ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                   int bytesRead;
                   byte[] data = new byte[1024];

                   // Reading bytes from the input stream
                   while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                       buffer.write(data, 0, bytesRead);
                   }

                   byte[] responseBytes = buffer.toByteArray();
                   Log.d(TAG, "Response Bytes Length: " + responseBytes.length);

                   // Log the raw response as a string for debugging
                   String rawResponse = new String(responseBytes);
                   Log.d(TAG, "Raw Response (as String): " + rawResponse);

                   // Attempt to parse the plist response
                   String publicKey = parsePublicKeyFromBplist(responseBytes);

                   if (publicKey != null) {
                       Log.d(TAG, "Parsed Public Key: " + publicKey);
                       return publicKey;
                   } else {
                       Log.e(TAG, "Parsed Public Key is null");
                   }
               }
           } else {
               Log.e(TAG, "HTTP Response Code: " + responseCode);

               try (InputStream errorStream = connection.getErrorStream()) {
                   if (errorStream != null) {
                       String errorResponse = new BufferedReader(new InputStreamReader(errorStream))
                               .lines()
                               .collect(Collectors.joining("\n"));
                       Log.e(TAG, "Error Response: " + errorResponse);
                   } else {
                       Log.e(TAG, "Error stream is null");
                   }
               }
           }
       } catch (Exception e) {
           Log.e(TAG, "Error occurred while fetching the public key.", e);
       }
       return null;
   }


    private String parsePublicKeyFromBplist(byte[] responseBytes) {
        try {
            Log.d(TAG, "Parsing Public Key from Response...");
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(responseBytes);
            String xmlRepresentation = rootDict.toXMLPropertyList(); // XML'e dönüştür
            Log.d(TAG, "Root Dictionary as XML: \n" + xmlRepresentation); // XML'i logla

            if (rootDict.containsKey("pk")) {
                Object pkObject = rootDict.objectForKey("pk");
                if (pkObject instanceof com.dd.plist.NSData) {
                    com.dd.plist.NSData pkData = (com.dd.plist.NSData) pkObject;
                    String publicKey = new String(pkData.bytes(), "UTF-8");
                    Log.d(TAG, "Public Key Found: " + publicKey);
                    return publicKey;
                } else {
                    Log.e(TAG, "Public Key is not an NSData instance.");
                }
            } else {
                Log.e(TAG, "Public Key not found in plist response.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Yanıttan public key ayıklanırken hata oluştu.", e);
        }
        return null;
    }


    private String generateAuthorizationToken(String publicKeyBase64, String message) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedMessage);
        } catch (Exception e) {
            Log.e(TAG, "Authorization token oluşturulurken hata oluştu.", e);
        }
        return null;
    }
}
