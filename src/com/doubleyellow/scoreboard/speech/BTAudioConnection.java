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
    boolean bKeepLooping =  true;

    float fVolume1 = 0.05f;

    String audioUrl = null;
    Context context = null;
    BTAudioConnection(int iNrOfSecsBeforeRepeat, String audioUrl, float fVolume0to1, Context context) {
        this.iNrOfSecsBeforeRepeat = iNrOfSecsBeforeRepeat;
        this.audioUrl = audioUrl;
        this.context = context;
        this.fVolume1 = fVolume0to1;
    }

    int iPlayIfZero = iNrOfSecsBeforeRepeat;
    @Override public void run() {
        Log.d(TAG, "Starting");
        try {
            Looper.prepare(); // TODO: Only one Looper may be created per thread
        } catch (Throwable e) {
            e.printStackTrace();
        }
        playAudio(); // start by playing the white noise to wake up the blue tooth immediately

        while(bKeepLooping) {
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
                reset("About to play white noise at volume " + fVolume1);
                playAudio();
            } else {
                if ( iPlayIfZero % 10 ==0 || iPlayIfZero <= 5 ) {
                    Log.d(TAG, String.format("%d more seconds before starting white noise", iPlayIfZero));
                }
            }
        }
        Log.d(TAG, "Stopped");
    }
    public void stopLooping() {
        bKeepLooping = false;
    }

    void reset(String sContext) {
        Log.d(TAG, "Resetting to " + iNrOfSecsBeforeRepeat + " : " + sContext);
        iPlayIfZero = iNrOfSecsBeforeRepeat;
    }
    private void playAudio() {
        final MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            if ( audioUrl.startsWith("http") ) {
                // from the web
                //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); // Use of stream types is deprecated for operations other than volume control
                mediaPlayer.setDataSource(audioUrl);
            } else {
                // e.g. an audio file from the res/raw folder
                mediaPlayer.setDataSource(context, Uri.parse(audioUrl) );
            }

            mediaPlayer.setVolume(fVolume1, fVolume1);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    // automatically reset once audio has finished playing: seems not to work when running in separate Thread?
                    reset("Playing white noise finished");
                    mp.release();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.w(TAG, "Error during playback of whitenoise: what=" + what + ", extra=" + extra);
                    return false;
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.w(TAG, "Could not play white noise : " + audioUrl + " " + e.getMessage());
            mediaPlayer.release();
        }
    }
}
