package com.doubleyellow.scoreboard.speech;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.LinkedHashSet;
import java.util.Locale;

public class Speak
{
    private static final String TAG = "SB." + Speak.class.getSimpleName();

    private TextToSpeech m_textToSpeech;
    private Context      m_context;
    private Locale       m_locale;
    private int          m_iStatus = TextToSpeech.STOPPED;

    private static final int I_DELAY_START            = 500;
    private              int m_iDelayBetweenTwoPieces = 500;

    private static final int RECALC_BEFORE_START      = -1;

    //------------------------------
    // lifecycle (singleton)
    //------------------------------

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

        setFeature(PreferenceValues.useSpeechFeature(m_context));

        float speechPitch = PreferenceValues.getSpeechPitch(context);
        float speechRate  = PreferenceValues.getSpeechRate(context);
        m_iDelayBetweenTwoPieces = PreferenceValues.getSpeechPauseBetweenWords(context);

        m_textToSpeech.setPitch     (speechPitch); // lower values for lower tone
        m_textToSpeech.setSpeechRate(speechRate);  // to clearly hear to score, a little lower than 1.0 is better

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

    private Feature m_Feature = Feature.DoNotUse;
    public boolean isEnabled() {
        return PreferenceValues.useFeatureYesNo(m_Feature);
    }
    public void setFeature(Feature feature) {
        m_Feature = feature;
    }

    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override public void onInit(int status) {
            m_iStatus = status;

/*
            if ( m_textToSpeech != null ) {
                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M */
/* 22 *//*
 ) {
                    Set<Voice> voices = m_textToSpeech.getVoices();
                    Log.d(TAG, "Available voices (listener): " + voices);
                }
            }
*/

            switch (status) {
                case TextToSpeech.SUCCESS:
                    Feature feature = PreferenceValues.useOfficialAnnouncementsFeature(m_context);
                    if ( PreferenceValues.useFeatureYesNo(feature) ) {
                        setLocale(PreferenceValues.announcementsLocale(m_context));
                    } else {
                        setLocale(null);
                    }
                    break;
                case TextToSpeech.ERROR:
                default:
                    Log.w(TAG, "Text to speech could not be initialized properly : " + status);
                    break;
            }
        }
    };

    //------------------------------
    // settings
    //------------------------------

    public void setLocale(Locale locNew) {
        Locale[] aLocales = new Locale[3];
        aLocales[0] = locNew;
        aLocales[1] = PreferenceValues.getDeviceLocale(m_context);
        aLocales[2] = Locale.ENGLISH;
        setOneOfLocales(aLocales);
    }

    public void setPitch(float fSpeechPitch) {
        m_textToSpeech.setPitch(fSpeechPitch);
    }
    public void setSpeechRate(float fSpeechRate) {
        m_textToSpeech.setSpeechRate(fSpeechRate);
    }
    public void setPauseBetweenParts(int iPauseInMS) {
        m_iDelayBetweenTwoPieces = iPauseInMS;
    }

    /** Returns false if first in sequence is not used */
    public boolean setOneOfLocales(Locale[] aLocales) {
        if ( m_textToSpeech == null ) { return false; }
        LinkedHashSet<String> lUnavailable = new LinkedHashSet<>();
        for ( Locale locale: aLocales ) {
            if ( locale == null ) { continue; }
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
            if ( m_locale != null ) {
                break;
            }
        }
        if ( ListUtil.isNotEmpty(lUnavailable) ) {
            if ( isEnabled() ) {
                String sMsg;
                if ( m_locale != null ) {
                    sMsg = String.format("Language %s not available to perform 'text to speech'. Using %s...", lUnavailable, m_locale.getDisplayLanguage());
                } else {
                    sMsg = String.format("Language %s not available to perform 'text to speech'.", lUnavailable);
                }
                Toast.makeText(m_context, sMsg, Toast.LENGTH_LONG).show();
            }
            //m_textToSpeech.setLanguage(Locale.ENGLISH);
            return false;
        }
        return true;
    }

    //------------------------------
    // determine what to say about score
    //------------------------------

    private void score(Model model)
    {
        Player server = model.getServer();
        int iServer   = model.getScore(server);
        int iReceiver = model.getScore(server.getOther());

        String sServer   = String.valueOf(iServer);
        String sReceiver = String.valueOf(iReceiver);

        GSMModel gsmModel = null;
        if ( model instanceof GSMModel ) {
            gsmModel = (GSMModel) model;
            sServer   = gsmModel.translateScore(server           , iServer);
            sReceiver = gsmModel.translateScore(server.getOther(), iReceiver);
        }
        if ( Brand.isSquash() || Brand.isGameSetMatch() ) {
            if ( iServer == 0 ) {
                sServer   = getResourceString(R.string.oa_love);
            }
            if ( iReceiver == 0 ) {
                sReceiver = getResourceString(R.string.oa_love);
            }
        }

        if ( iReceiver == iServer ) {
            // score is equal
            String s_X_All__or__X_Equal = getResourceString(R.string.oa_n_all__or__n_equal, iServer);
            String sXAll__or_sXEqual    = s_X_All__or__X_Equal.replaceAll("[0-9]+", "").trim();
            if ( (gsmModel != null) && (iServer >= 3) && (gsmModel.isTieBreakGame() == false) ) {
                int iResGoldenPointOrDeuce = gsmModel.getGoldenPointToWinGame() ? R.string.oa_golden_point : R.string.oa_deuce;
                String sDeuceOrGoldenPoint = getResourceString(iResGoldenPointOrDeuce);
                setTextToSpeak(SpeechType.ScoreServer  , sDeuceOrGoldenPoint);
                setTextToSpeak(SpeechType.ScoreReceiver, "");
            } else {
                setTextToSpeak(SpeechType.ScoreServer  , sServer);
                setTextToSpeak(SpeechType.ScoreReceiver, sXAll__or_sXEqual);
            }
        } else {
            if ( gsmModel != null  ) {
                if ( (Math.max(iReceiver, iServer) > 3) && (gsmModel.isTieBreakGame() == false) ) {
                    if ( Math.abs(iServer - iReceiver) == 1 ) {
                        setTextToSpeak(SpeechType.ScoreServer, getResourceString(R.string.sb_advantage));
                        String sAdvWho = null;
                        if ( iServer > iReceiver ) {
                            sAdvWho = gsmModel.getName(gsmModel.getServer());
                            //sAdvWho = getResourceString(R.string.sb_server);
                        } else {
                            sAdvWho = gsmModel.getName(gsmModel.getReceiver());
                            //sAdvWho = getResourceString(R.string.sb_receiver);
                        }
                        setTextToSpeak(SpeechType.ScoreReceiver, sAdvWho);
                        // advantage Server
                        // advantage Receiver
                    } else {
                        // game has been won
                        setTextToSpeak(SpeechType.ScoreServer, getResourceString(R.string.oa_game));
                        setTextToSpeak(SpeechType.ScoreReceiver, "");
                    }
                } else {
                    // not yet reached 40, different score
                    setTextToSpeak(SpeechType.ScoreServer  , sServer);
                    setTextToSpeak(SpeechType.ScoreReceiver, sReceiver);
                }
            } else {
                setTextToSpeak(SpeechType.ScoreServer  , sServer);
                setTextToSpeak(SpeechType.ScoreReceiver, sReceiver);

                if ( Brand.isNotSquash() && model.isPossibleGameVictory() ) {
                    // say score of winner of game first (not by default the server)
                    setTextToSpeak(SpeechType.ScoreServer  , Math.max(iServer, iReceiver));
                    setTextToSpeak(SpeechType.ScoreReceiver, Math.min(iServer, iReceiver));
                }
            }
        }
    }

    private void handout(Model model) {
        if ( Brand.isNotSquash() && (Brand.isBadminton() == false) ) {
            return;
        }

        boolean bIsHandout = model.isLastPointHandout();

        // only say handout 'between' points in a game, so not at zero-zero and not if game is won by someone
        boolean bGameNotStarted = model.gameHasStarted() == false;
        boolean bGameEnded      = model.isPossibleGameVictory();
      //Log.d(TAG, String.format("GameNotStarted: %s, GameEnded : %s", bGameNotStarted, bGameEnded));
        if ( bGameNotStarted ) { bIsHandout = false; }
        if ( bGameEnded      ) { bIsHandout = false; }

        String sText = "";
        if ( bIsHandout ) {
            sText = getResourceString(R.string.oa_handout);

            if ( Brand.isBadminton() ) {
                sText = getResourceString(R.string.sb_service_over);
            }
        }
        setTextToSpeak(SpeechType.Handout, sText);
    }

    private void gameBall(Model model)
    {
        if ( Brand.isNotSquash() ) { return; }

        Player[] possibleGameBallFor = model.isPossibleGameBallFor();
        Boolean bIsGameball = (possibleGameBallFor != null) && (possibleGameBallFor.length != 0);

        String sText = "";
        if ( bIsGameball ) {
            int iResId = Brand.getGameSetBallPoint_ResourceId();
            sText = getResourceString(iResId);
        }
        setTextToSpeak(SpeechType.Gameball, sText);
    }

    //------------------------------
    // timer announcements
    //------------------------------

    public void setTimerMessage(String s) {
        if ( isStarted() == false ) { return; }
        if ( isEnabled() == false ) { return; }

        if ( s == null ) {
            s = getResourceString(R.string.oa_time);
        }
        setTextToSpeak(SpeechType.TimerRelated, s);
    }

    /** for automatic playing of score */
    public void playAllDelayed(int iDelay) {
        if ( isStarted() == false ) { return; }
        if ( Feature.Automatic.equals(m_Feature) == false ) { return; }

        // 'delayed' to allow model to
        //    1) complete all eventhandlers, and
        //    2) allow user to change score quickly without all scores actually being spoken
        if ( iDelay <= 0 ) {
            iDelay = I_DELAY_START;
        }
        emptySpeechQueue_Delayed(RECALC_BEFORE_START, iDelay, 0);
    }

    /** for playing of score on request */
    public void playAll(Model matchModel) {
        if ( isStarted() == false ) { return; }
        if ( isEnabled() == false ) { return; }

        if ( Brand.isSquash() ) {
            this.handout(matchModel);
        }
        if ( matchModel != null ) {
            this.score(matchModel);
            this.gameBall(matchModel);
        }

        emptySpeechQueue(0, 0);
    }

    /** get text from correct locale */
    private String getResourceString(int p, Object ... args) {
        Resources resources = PreferenceValues.newResources(m_context.getResources(), m_locale);
        return resources.getString(p, args);
    }

    //------------------------------
    // the actual storage of what to say and actually speak it
    //------------------------------

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
    private String[] m_sText = new String[SpeechType.values().length];
    private void setTextToSpeak(SpeechType type, Object oText) {
        String sOld = m_sText[type.ordinal()];
        String sNew = String.valueOf(oText);
        if ( (sOld == null) || (sOld.equals(sNew) == false) ) {
            m_sText[type.ordinal()] = sNew;
        }
    }

    private int m_iErrorCount = 0;
    private void speak(String sText, SpeechType type) {
        try {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP /* 21 */ ) {
                Bundle bundle = new Bundle();
                bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f); // between 0 and 1
                bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN   , 0.0f); // between -1 and 1
                m_textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, bundle, null);
            } else {
                m_textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, null);
            }
            m_iErrorCount = 0;
        } catch (Exception e) {
            e.printStackTrace();
            m_iErrorCount++;
            if ( m_iErrorCount > 3 ) {
                setFeature(Feature.DoNotUse);
            }
        }
    }

    private void emptySpeechQueue(int iStartAtStep, int iFromDelayed) {
        if ( isStarted() == false ) { return; }

        for(SpeechType type: SpeechType.values() ) {
            if ( type.ordinal() < iStartAtStep ) { continue; }
            String sToSpeak = m_sText[type.ordinal()];
            if ( StringUtil.isEmpty(sToSpeak) ) {
                continue;
            }
            if ( m_textToSpeech.isSpeaking() ) {
                // retry in a half a second, starting at the same index
                if ( iFromDelayed < 10 ) {
                    emptySpeechQueue_Delayed(type.ordinal(), m_iDelayBetweenTwoPieces, iFromDelayed+1);
                }
                return;
            }

            m_sText[type.ordinal()] = null;
            Log.d(TAG, String.format("Speaking %s : %s", type, sToSpeak));
            speak(sToSpeak, type);
            emptySpeechQueue_Delayed(type.ordinal() + 1, m_iDelayBetweenTwoPieces, 0);
            return;
        }
    }
    private static CountDownTimer m_cdtEmptySpeechQueue = null;
    private void emptySpeechQueue_Delayed(final int iStartAtStep, int iDelayMs, final int iDelayedBecauseStillSpeaking) {
        if ( m_cdtEmptySpeechQueue != null) {
            m_cdtEmptySpeechQueue.cancel();
        }
        m_cdtEmptySpeechQueue = new CountDownTimer(iDelayMs, 100) {
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                if ( iStartAtStep == RECALC_BEFORE_START ) {
                    playAll(ScoreBoard.getMatchModel());
                }
                emptySpeechQueue(iStartAtStep, iDelayedBecauseStillSpeaking);
            }
        };
        m_cdtEmptySpeechQueue.start();
    }
}
