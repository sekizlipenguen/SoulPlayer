package com.sekizlipenguen.soulplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.util.Log;

public class SoulOrientationModule extends ReactContextBaseJavaModule {
    private Activity activity;
    private static final String TAG = "SoulOrientationModule";

    public SoulOrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Constructor initialized");
        }

        // LifecycleEventListener ile Activity referansını güncel tut
        reactContext.addLifecycleEventListener(new LifecycleEventListener() {
            @Override
            public void onHostResume() {
                activity = reactContext.getCurrentActivity(); // Resume sırasında Activity'yi al
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onHostResume: Activity updated");
                }
            }

            @Override
            public void onHostPause() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onHostPause: Activity paused");
                }
            }

            @Override
            public void onHostDestroy() {
                activity = null; // Activity yok edildiğinde temizle
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onHostDestroy: Activity set to null");
                }
            }
        });
    }

    @Override
    public String getName() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getName called");
        }
        return "SoulOrientationModule"; // React Native'de kullanılacak benzersiz isim
    }

    @ReactMethod
    public void lockToPortrait() {
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "lockToPortrait called and executed successfully");
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "lockToPortrait: Activity is null");
            }
        }
    }

    @ReactMethod
    public void lockToLandscape() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "lockToLandscape called");
        }
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "lockToLandscape executed successfully");
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "lockToLandscape: Activity is null");
            }
        }
    }

    @ReactMethod
    public void unlockAllOrientations() {
        if (activity != null) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "unlockAllOrientations called and executed successfully");
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "unlockAllOrientations: Activity is null");
            }
        }
    }
}
