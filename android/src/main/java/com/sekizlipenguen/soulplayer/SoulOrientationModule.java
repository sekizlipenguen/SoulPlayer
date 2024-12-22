package com.sekizlipenguen.soulplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.util.Log; // Loglama işlemleri için

public class SoulOrientationModule extends ReactContextBaseJavaModule {
    private final Activity activity;
    private static final String TAG = "SoulOrientationModule";

    public SoulOrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.e(TAG, "Constructor çağrıldı!");
        this.activity = getCurrentActivity();
    }

    @Override
    public String getName() {
        Log.e("SoulOrientationModule", "getName çağrıldı!");
        return "SoulOrientationModule"; // React Native'de kullanılacak benzersiz isim
    }

    @ReactMethod
    public void lockToPortrait() {
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @ReactMethod
    public void lockToLandscape() {
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @ReactMethod
    public void unlockAllOrientations() {
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
}
