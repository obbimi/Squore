package com.doubleyellow.scoreboard.speech;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Speak
{
    private static final String TAG = "SB." + Speak.class.getSimpleName();

    private TextToSpeech m_textToSpeech;
    private Context      m_context;
    private Locale       m_locale;

    private Speak() { }
    private static final Speak instance = new Speak();
    public static Speak getInstance() {
        return instance;
    }

    /** needs to be invoked to initialize text-to-speech object */
    public boolean start(Context context) {
        if ( isStarted() ) { return true; }

        m_context = context.getApplicationContext();

        m_textToSpeech = new TextToSpeech(m_context, onInitListener);

        float speechPitch = PreferenceValues.getSpeechPitch(context);
        float speechRate  = PreferenceValues.getSpeechRate(context);
        m_textToSpeech.setPitch     (speechPitch); // lower values for lower tone
        m_textToSpeech.setSpeechRate(speechRate);  // to clearly hear to score, a little lower than 1.0 is better

        Set<Voice> voices = m_textToSpeech.getVoices();
        Log.d(TAG, "Available voices: " + voices); // null on my Samsung

        if ( false ) {
            Set<Locale> langs = m_textToSpeech.getAvailableLanguages();
            Log.d(TAG, "Available languages: " + langs); // null on my old Samsung

            // TODO: is this required?
            Locale[] locales = Locale.getAvailableLocales();
            List<Locale> localeList = new ArrayList<Locale>();
            for (Locale locale : locales) {
                int res = m_textToSpeech.isLanguageAvailable(locale);
                if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    localeList.add(locale);
                }
                int iResult = m_textToSpeech.setLanguage(locale);
            }

            Set<Locale> langs2 = m_textToSpeech.getAvailableLanguages();
            Log.d(TAG, "Available languages2: " + langs2); // null on my old Samsung
        }
        return true;
    }

    /** invoked to free up resources */
    public void stop() {
        if ( m_textToSpeech != null ) {
            m_textToSpeech.stop();
            m_textToSpeech.shutdown();
            m_textToSpeech = null;
        }
        m_context = null;
        m_iStatus = TextToSpeech.STOPPED;
    }

    public boolean isStarted() {
        if ( m_textToSpeech == null            ) { return false; }
        if ( m_iStatus != TextToSpeech.SUCCESS ) { return false; }

        return true;
    }
    public void setPitch(float fSpeechPitch) {
        m_textToSpeech.setPitch(fSpeechPitch);
    }
    public void setSpeechRate(float fSpeechRate) {
        m_textToSpeech.setSpeechRate(fSpeechRate);
    }
    public void test(String s) {
        speak(s, SpeechType.Other);
    }

    public void score(Model model) {
        if ( isStarted() == false ) { return; }

        Player server = model.getServer();
        int iServer   = model.getScore(server);
        int iReceiver = model.getScore(server.getOther());

        addToQueue(SpeechType.ScoreServer  , iServer);
        if ( iReceiver == iServer ) {
            String s_X_All = getResourceString(R.string.oa_n_all, iServer);
            String sAll  = s_X_All.replaceAll("[0-9]+", "").trim();
            if ( model instanceof GSMModel && (iServer >= 3) ) {
                addToQueue(SpeechType.ScoreServer  , getResourceString(R.string.sb_deuce));
            } else {
                addToQueue(SpeechType.ScoreServer  , iServer);
                addToQueue(SpeechType.ScoreReceiver, sAll);
            }
        } else {
            if ( model instanceof GSMModel  ) {
                GSMModel gsmModel = (GSMModel) model;
                if ( Math.max(iReceiver, iServer) > 3 ) {
                    if ( Math.abs(iServer - iReceiver) == 1 ) {
                        addToQueue(SpeechType.ScoreServer, getResourceString(R.string.sb_advantage));
                        // advantage Server
                        // advantage Receiver
                    } else {
                        // game has been won
                        addToQueue(SpeechType.ScoreServer, getResourceString(R.string.oa_game));
                    }
                } else {
                    // not yet reached 40, different score
                    addToQueue(SpeechType.ScoreServer  , iServer);
                    addToQueue(SpeechType.ScoreReceiver, iReceiver);
                }
            } else {
                addToQueue(SpeechType.ScoreReceiver, iReceiver);
            }
        }
    }

    public String getResourceString(int p, Object ... args) {
        Resources resources = PreferenceValues.newResources(m_context.getResources(), m_locale);
        return resources.getString(p, args);
    }

    public void handout(boolean bIsHandout) {
        if ( isStarted() == false ) { return; }

        if ( bIsHandout ) {
            addToQueue(SpeechType.Handout, getResourceString(R.string.handout));
        } else {
            addToQueue(SpeechType.Handout, "");
        }
    }
    public void gameBall(Model model) {
        if ( isStarted() == false ) { return; }

        int iResId = Brand.getGameSetBallPoint_ResourceId();
        String sText = getResourceString(iResId);
        addToQueue(SpeechType.Gameball, sText);
    }

    public void setTimerMessage(String s) {
        if ( isStarted() == false ) { return; }

        if ( s == null ) {
            s = getResourceString(R.string.oa_time);
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
        Other,
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
        try {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP /* 21 */ ) {
                Bundle bundle = new Bundle();
                bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1); // between 0 and 1
                bundle.putInt(TextToSpeech.Engine.KEY_PARAM_PAN   , 0); // between -1 and 1
                m_textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, bundle, null);
            } else {
                m_textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stop(); // assume something is wrong al together, stop speaking
        }
    }
    private int m_iStatus = TextToSpeech.STOPPED;
    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override public void onInit(int status) {
            m_iStatus = status;

            switch (status) {
                case TextToSpeech.SUCCESS:
                    Set<String> lUnavailable = new HashSet<>();
                    Locale[] locales = new Locale[] { PreferenceValues.announcementsLocale(m_context)
                                                    , PreferenceValues.getDeviceLocale(m_context)
                                                    , Locale.ENGLISH
                                                    };
                    for(Locale locale: locales) {
                        int iResult = m_textToSpeech.setLanguage(locale);
                        switch (iResult) {
                            case TextToSpeech.LANG_MISSING_DATA:
                            case TextToSpeech.LANG_NOT_SUPPORTED:
                                lUnavailable.add(locale.getDisplayLanguage());
                                break;
                            default:
                                m_locale = locale;
                                break;
                        }
                    }
                    if ( ListUtil.isNotEmpty(lUnavailable) ) {
                        Toast.makeText(m_context, String.format("Language %s not available to perform 'text to speech'. Using %s...", lUnavailable, m_locale.getDisplayLanguage()), Toast.LENGTH_LONG).show();
                        m_textToSpeech.setLanguage(Locale.ENGLISH);
                    }
                    break;
                default:
                    Log.w(TAG, "Text to speech could not be initialized properly : " + status);
                    break;
            }
        }
    };

    private void emptySpeechQueue(int iStartAt) {
        if ( isStarted() == false ) { return; }

        for(SpeechType type: SpeechType.values() ) {
            if ( type.ordinal() < iStartAt ) { continue; }
            String sToSpeak = m_sText[type.ordinal()];
            if ( StringUtil.isEmpty(sToSpeak) ) {
                continue;
            }
            if ( m_textToSpeech.isSpeaking() ) {
                // retry in a half a second, starting at the same index
                emptySpeechQueue_Delayed(type.ordinal(), I_DELAY_BETWEEN_TWO_RELATED);
                return;
            }

            m_sText[type.ordinal()] = null;
            Log.d(TAG, String.format("Speaking %s : %s", type, sToSpeak));
            speak(sToSpeak, type);
            emptySpeechQueue_Delayed(type.ordinal() + 1, I_DELAY_BETWEEN_TWO_RELATED);
            return;
        }
    }
    private static CountDownTimer m_cdtEmptySpeechQueue = null;
    public void emptySpeechQueue_Delayed(final int iStartAt, int iDelayMs) {
        if ( m_cdtEmptySpeechQueue != null) {
            m_cdtEmptySpeechQueue.cancel();
        }
        m_cdtEmptySpeechQueue = new CountDownTimer(iDelayMs, 100) {
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                emptySpeechQueue(iStartAt);
            }
        };
        m_cdtEmptySpeechQueue.start();
    }
}
