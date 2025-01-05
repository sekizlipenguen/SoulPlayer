package com.sekizlipenguen.soulplayer.cast;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.android.gms.cast.CastMediaControlIntent;

public class MediaRouteButtonManager extends SimpleViewManager<MediaRouteButton> {

    public static final String REACT_CLASS = "MediaRouteButton";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected MediaRouteButton createViewInstance(ThemedReactContext reactContext) {
        // MediaRouter temasıyla bağlanmış bir context oluştur
        int themeId = androidx.mediarouter.R.style.Theme_MediaRouter;
        Context themedContext = new ContextThemeWrapper(reactContext, themeId);

        // MediaRouteButton'ı oluştur ve RouteSelector bağla
        MediaRouteButton mediaRouteButton = new MediaRouteButton(themedContext);

        // RouteSelector ayarla
        MediaRouteSelector routeSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent.categoryForCast(
                                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                        )
                )
                .build();
        mediaRouteButton.setRouteSelector(routeSelector);

        return mediaRouteButton;
    }
}
