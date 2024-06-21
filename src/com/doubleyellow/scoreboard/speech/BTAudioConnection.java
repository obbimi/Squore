package com.doubleyellow.scoreboard.speech;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

/**
 * Test to keep BTAudioConnection alive during long pauses.
 * A long pause may cause the 'Blue tooth sound connection' to go to sleep to save energy, but causing words being 'swallowed' when restarted.
 */
class BTAudioConnection extends Thread {
    private static final String TAG = "SB." + BTAudioConnection.class.getSimpleName();

    int iNrOfSecsBeforeRepeat = 20;

    String audioUrl = null;
    Context context = null;
    BTAudioConnection(int iNrOfSecsBeforeRepeat, String audioUrl, Context context) {
        this.iNrOfSecsBeforeRepeat = iNrOfSecsBeforeRepeat;
        this.audioUrl = audioUrl;
        this.context = context;
    }

    int iPlayIfZero = iNrOfSecsBeforeRepeat;
    @Override public void run() {
        try {
            Looper.prepare(); // TODO: Only one Looper may be created per thread
        } catch (Throwable e) {
            e.printStackTrace();
        }
        playAudio(); // start by playing the white noise to wake up the blue tooth immediately

        while(true) {
            synchronized (this) {
                try {
                    wait(1000); // keep this at a thousend !!
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            iPlayIfZero--;
            if ( iPlayIfZero == 0 ) {
                Log.d(TAG, String.format("Playing white noise : %s", audioUrl));
                playAudio();
                reset();
            } else {
                //Log.d(TAG, String.format("%d more seconds before starting white noise", iPlayIfZero));
            }
        }
    }

    void reset() {
        iPlayIfZero = iNrOfSecsBeforeRepeat;
    }
    private void playAudio() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            if ( audioUrl.startsWith("http") ) {
                // from the web
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(audioUrl);
            } else {
                // e.g. an audio file from the res/raw folder
                mediaPlayer.setDataSource(context, Uri.parse(audioUrl) );
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    // automatically reset once audio has finished playing
                    mediaPlayer.release();
                    reset();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Could not play white noise : " + audioUrl);
            mediaPlayer.release();
        }
    }
}
