package com.sekizlipenguen.soulplayer;

import android.content.Context;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;

public class MediaController {

    private final SimpleExoPlayer player;

    public MediaController() {
        Context context = YourApp.getContext(); // Uygulamanızın context'ini alın
        player = new SimpleExoPlayer.Builder(context).build();
    }

    public void seekForward(int seconds) {
        long newPosition = player.getCurrentPosition() + seconds * 1000;
        player.seekTo(newPosition);
    }

    public void seekBackward(int seconds) {
        long newPosition = player.getCurrentPosition() - seconds * 1000;
        if (newPosition < 0) newPosition = 0;
        player.seekTo(newPosition);
    }

    public void setPlaybackSpeed(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        player.setPlaybackParameters(parameters);
    }

    public void changeQuality(String quality) {
        // Burada kalite değiştirme mantığı yazılabilir (örneğin, farklı HLS çözünürlükleri)
    }
}
