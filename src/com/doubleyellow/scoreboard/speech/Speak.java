package com.doubleyellow.scoreboard.speech;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Speak
{
    private static final String TAG = "SB." + Speak.class.getSimpleName();

    private TextToSpeech textToSpeech;
    private Context      context;

    private Speak() { }
    private static final Speak instance = new Speak();
    public static Speak getInstance() {
        return instance;
    }

    /** needs to be invoked to initialize text-to-speach object */
    public boolean start(Context context) {
        this.context = context;

        textToSpeech = new TextToSpeech(context, onInitListener);

        float speechPitch = PreferenceValues.getSpeechPitch(context);
        float speechRate  = PreferenceValues.getSpeechRate(context);
        textToSpeech.setPitch     (speechPitch); // lower values for lower tone
        textToSpeech.setSpeechRate(speechRate);  // to clearly hear to score, a little lower than 1.0 is better

        Set<Voice> voices = textToSpeech.getVoices();
        Log.d(TAG, "Available voices: " + voices); // null on my Samsung

        return true;
    }

    /** invoked to free up resources */
    public void stop() {
        if ( textToSpeech != null ) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private boolean isStarted() {
        if ( textToSpeech == null              ) { return false; }
        if ( m_iStatus != TextToSpeech.SUCCESS ) { return false; }

        return true;
    }

    public void score(Model model) {
        if ( isStarted() == false ) { return; }

        Player server = model.getServer();
        int iServer = model.getScore(server);
        int iReceiver = model.getScore(server.getOther());

        addToQueue(SpeechType.ScoreServer  , iServer);
        if ( iReceiver == iServer ) {
            String x_all = context.getString(R.string.oa_n_all, iServer);
            String all  = x_all.replaceAll("[0-9]+", "").trim();
            if ( true ) {
                addToQueue(SpeechType.ScoreServer  , iServer);
                addToQueue(SpeechType.ScoreReceiver, all);
            } else {
                addToQueue(SpeechType.ScoreServer  , x_all);
            }
        } else {
            addToQueue(SpeechType.ScoreReceiver, iReceiver);
        }
    }
    public void handout(boolean bIsHandout) {
        if ( isStarted() == false ) { return; }

        if ( bIsHandout ) {
            addToQueue(SpeechType.Handout, context.getString(R.string.handout));
        } else {
            addToQueue(SpeechType.Handout, "");
        }
    }
    public void gameBall(Model model) {
        if ( isStarted() == false ) { return; }

        int iResId = Brand.getGameSetBallPoint_ResourceId();
        String sText = PreferenceValues.getOAString(context, iResId); // todo: if language not supported, text is pronounced in english voice !?
        addToQueue(SpeechType.Gameball, sText);
    }

    public void setTimerMessage(String s) {
        if ( isStarted() == false ) { return; }

        if ( s == null ) {
            s = context.getString(R.string.oa_time);
        }
        addToQueue(SpeechType.TimerRelated, s);
    }

    /** text fragments will be announced in this sequence they are defined in this enumeration */
    private enum SpeechType {
      //Call,
      //StartAnnouncement,
        Handout,
        ScoreServer,
        ScoreReceiver,
        Gameball,
      //EndAnnouncement,
        TimerRelated,
    }
    private static final int I_DELAY_START               = 500;
    private static final int I_DELAY_BETWEEN_TWO_RELATED = 500;

    private String[] m_sText = new String[SpeechType.values().length];
    private void addToQueue(SpeechType type, Object oText) {
        String sOld = m_sText[type.ordinal()];
        String sNew = String.valueOf(oText);
        if ( sOld == null || sOld.equals(sNew) == false ) {
            m_sText[type.ordinal()] = sNew;

            emptySpeechQueue_Delayed(0, I_DELAY_START);
        }
    }

    private void speak(String sText, SpeechType type) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP /* 21 */ ) {
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1); // between 0 and 1
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_PAN   , 0); // between -1 and 1
            textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, bundle, null);
        } else {
            textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, null);
        }
    }
    private int m_iStatus = TextToSpeech.STOPPED;
    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override public void onInit(int status) {
            m_iStatus = status;

            switch (status) {
                case TextToSpeech.SUCCESS:
                    List<Locale> lUnavailable = new ArrayList<>();
                    Locale[] locales = new Locale[] { PreferenceValues.announcementsLocale(context), PreferenceValues.getDeviceLocale(context), Locale.ENGLISH };
                    Locale locUsed = null;
                    for(Locale locale: locales) {
                        int iResult = textToSpeech.setLanguage(locale);
                        switch (iResult) {
                            case TextToSpeech.LANG_MISSING_DATA:
                            case TextToSpeech.LANG_NOT_SUPPORTED:
                                lUnavailable.add(locale);
                                break;
                            default:
                                locUsed = locale;
                                break;
                        }
                    }
                    if ( ListUtil.isNotEmpty(lUnavailable) ) {
                        Toast.makeText(context, String.format("Language %s not available to perform 'text to speech'. Using %s...", lUnavailable, locUsed), Toast.LENGTH_LONG).show();
                        textToSpeech.setLanguage(Locale.ENGLISH);
                    }
                    break;
                default:
                    Log.w(TAG, "Text to speech could not be initialized properly : " + status);
                    break;
            }
        }
    };

    private void emptySpeechQueue(int iStartAt) {
        for(SpeechType type: SpeechType.values() ) {
            if ( type.ordinal() < iStartAt ) { continue; }
            String sToSpeak = m_sText[type.ordinal()];
            if ( StringUtil.isEmpty(sToSpeak) ) {
                continue;
            }
            m_sText[type.ordinal()] = null;
            Log.d(TAG, String.format("Speaking %s : %s", type, sToSpeak));
            speak(sToSpeak, type);
            emptySpeechQueue_Delayed(type.ordinal() + 1, I_DELAY_BETWEEN_TWO_RELATED);
            return;
        }
    }
    private static CountDownTimer m_cdtEmptySpeachQueue = null;
    public void emptySpeechQueue_Delayed(final int iStartAt, int iDelayMs) {
        if ( m_cdtEmptySpeachQueue!= null) {
            m_cdtEmptySpeachQueue.cancel();
        }
        m_cdtEmptySpeachQueue = new CountDownTimer(iDelayMs, 100) {
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                emptySpeechQueue(iStartAt);
            }
        };
        m_cdtEmptySpeachQueue.start();
    }
}
