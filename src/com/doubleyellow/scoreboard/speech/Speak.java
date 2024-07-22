package com.doubleyellow.scoreboard.speech;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
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
import com.doubleyellow.util.MapUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        m_textToSpeech.setOnUtteranceProgressListener(onUtteranceProgressListener);

        setFeature(PreferenceValues.useSpeechFeature(m_context));

        float speechPitch = PreferenceValues.getSpeechPitch(context);
        float speechRate  = PreferenceValues.getSpeechRate(context);
        m_iDelayBetweenTwoPieces = PreferenceValues.getSpeechPauseBetweenWords(context);

        setPitch     (speechPitch); // lower values for lower tone
        setSpeechRate(speechRate);  // to clearly hear to score, a little lower than 1.0 is better

        boolean bPlayWhiteNoise = PreferenceValues.speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(context);
        if ( isEnabled() && bPlayWhiteNoise ) {
            startWhiteNoise();
        }

        return true;
    }
    private BTAudioConnection btAudioConnection = null;

    public void startWhiteNoise() {
        String sUrlOfAudio = PreferenceValues.getSpeech_UrlOfSoundFileToPlayToKeepAlive(m_context);
        if ( StringUtil.isNotEmpty(sUrlOfAudio) ) {
            if ( btAudioConnection == null ) {
                int iPauseBetween = PreferenceValues.speechOverBT_PauseBetweenPlaysToKeepAlive(m_context);
                int iVolume       = PreferenceValues.speechOverBT_PlayingVolumeToKeepAlive(m_context);
                iVolume = iVolume % 100;
                float fVolume0to1 = (iVolume / 100.0f);
                btAudioConnection = new BTAudioConnection(iPauseBetween, sUrlOfAudio, fVolume0to1, m_context);
                btAudioConnection.start();
            }
        }
    }
    public void stopWhiteNoise() {
        if ( btAudioConnection != null ) {
            btAudioConnection.stopLooping();
            btAudioConnection = null;
        }
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

    private final TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override public void onInit(int status) {
            m_iStatus = status;

            switch (status) {
                case TextToSpeech.SUCCESS:
                    Locale announcementsLocale = PreferenceValues.announcementsLocale(m_context);
                    setLocale(announcementsLocale);

                    String sVoice = PreferenceValues.getSpeechVoice(m_context);
                    if ( sVoice != null ) {
                        setVoice(sVoice);
                    }

/*
                    Feature feature = PreferenceValues.useOfficialAnnouncementsFeature(m_context);
                    if ( PreferenceValues.useFeatureYesNo(feature) ) {
                        setLocale(announcementsLocale);
                    } else {
                        setLocale(null);
                    }
*/
                    break;
                case TextToSpeech.ERROR:
                default:
                    Log.w(TAG, "Text to speech could not be initialized properly : " + status);
                    break;
            }
        }
    };
    private final UtteranceProgressListener onUtteranceProgressListener = new UtteranceProgressListener() {
        @Override public void onStart(String utteranceId) {
            Log.d(TAG, "onStart with " + utteranceId);
        }

        @Override public void onDone(String utteranceId) {
            Log.d(TAG, "onDone with " + utteranceId);
            //Looper.prepare(); // Only one Looper may be created per thread
            speakNext(utteranceId);
        }

        @Override public void onError(String utteranceId) {
            Log.d(TAG, "onError with " + utteranceId);
            speakNext(utteranceId);
        }

        private boolean speakNext(String utteranceId) {
            int ordinal = SpeechType.valueOf(utteranceId).ordinal();
            emptySpeechQueue_Delayed(ordinal + 1, m_iDelayBetweenTwoPieces/*, 0*/);
            return true;
        }
    };

    /*
    m_textToSpeech.addSpeech("40", m_context.getPackageName(), R.raw.speak_40_german);
    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {
        m_textToSpeech.addSpeech("40", Uri.parse("https://squore.double-yellow.be/speach/"));
    }
    */

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
    private boolean setOneOfLocales(Locale[] aLocales) {
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
            getVoices(locale.getLanguage());
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

    private Map<String, Map<String, Voice>> mLang2Voices = new HashMap<>();
    private Map<String, Voice> mName2VoiceForLanguage = new HashMap<>();

    /** invoked e.g. from Preferences. DisplayName2VoiceName */
    public Map<String, String> getVoices()
    {
        Map<String, String> sDisplayName2Name = new LinkedHashMap<>();
        final String reGender = ".*#(female|male).*";
        for(String sName: mName2VoiceForLanguage.keySet() ) {
            Voice voice = mName2VoiceForLanguage.get(sName);

            List<String> lOptions = new ArrayList<>();
            String country = voice.getLocale().getCountry();
            if ( country != null ) {
                lOptions.add(country);
            }
            String sDisplayName = "";
/*
            if ( voice.isNetworkConnectionRequired() ) {
                Log.w(TAG, "voice " + voice.getName() + " needs network");
                lOptions.add("network");
            }
*/
            if ( voice.getQuality() <= Voice.QUALITY_LOW ) {
                Log.w(TAG, "voice " + voice.getName() + " has low quality");
                lOptions.add("q:" + voice.getQuality());
            }
/*
            Set<String> features = voice.getFeatures();
            if ( features != null && features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) ) {
                lOptions.add("n.a.");
            }
*/
            if ( sName.matches(reGender) ) {
                String sGender = sName.replaceAll(reGender, "$1");
                sDisplayName += StringUtil.capitalize(sGender);
            }
            // [networkTimeoutMs, notInstalled, legacySetLanguageVoice, networkRetriesCount]
            Log.w(TAG, "Voice features " + voice.getName() + " : " + voice.getFeatures());
            sDisplayName = "Voice " + (MapUtil.size(sDisplayName2Name) + 1) + " " + sDisplayName;
            if ( lOptions.size() > 0 ) {
                sDisplayName += " [" + ListUtil.join(lOptions, ",") + "]"; // TODO: deliberately not using round braces: causes issues with RWValues.updatePreferenceTitle(preference)
            }
            sDisplayName = StringUtil.normalize(sDisplayName);
            sDisplayName2Name.put(sDisplayName, sName);
        }

        return sDisplayName2Name;
    }
    private boolean getVoices(String sLanguage) {
        mName2VoiceForLanguage = mLang2Voices.get(sLanguage);
        if ( mName2VoiceForLanguage == null ) {
            mName2VoiceForLanguage = new HashMap<>();
            mLang2Voices.put(sLanguage, mName2VoiceForLanguage);
        }
        Set<Voice> voices = m_textToSpeech.getVoices(); // 465 on my Mi: https://cloud.google.com/text-to-speech/docs/voices
        if ( voices == null ) {
            return false;
        }
        for(Voice voice: voices) {
            if ( voice.getLocale().getLanguage().equalsIgnoreCase(sLanguage)) {

                // name may contain #male or #female
                mName2VoiceForLanguage.put(voice.getName(), voice);
            }
        }
        return true;
    }

    public void setVoice(String sName) {
        if ( MapUtil.isEmpty(mName2VoiceForLanguage) && m_locale != null ) {
            getVoices(m_locale.getLanguage());
        }
        Voice voice = mName2VoiceForLanguage.get(sName);
        if ( voice != null ) {
            int SuccessOfError = m_textToSpeech.setVoice(voice);
            if ( SuccessOfError == TextToSpeech.ERROR ) {
                Toast.makeText(m_context, "Could not set voice " + sName, Toast.LENGTH_SHORT).show();
            }
        }
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

            if ( (iReceiver == 0) && (iServer == 0) ) {
                // also speak the number of games won at the start of a new game
                Map<Player, Integer> gamesWonLastSet = null;
                if ( gsmModel != null ) {
                    List<Map<Player, Integer>> gamesWonPerSet = gsmModel.getGamesWonPerSet();
                    gamesWonLastSet = ListUtil.getLast(gamesWonPerSet);
                } else {
                    gamesWonLastSet = model.getGamesWon();
                }
                if ( MapUtil.getMaxValue(gamesWonLastSet) > 0 && model.matchHasEnded() == false ) {
                    Player pLeaderInGames = MapUtil.getMaxKey(gamesWonLastSet, Player.A);
                    int iGamesLeader  = gamesWonLastSet.get(pLeaderInGames);
                    int iGamesTrailer = gamesWonLastSet.get(pLeaderInGames.getOther());
                    String sGamesScoreText = null;
                    if ( iGamesLeader == iGamesTrailer ) {
                        sGamesScoreText = (iGamesLeader==1) ? getResourceString(R.string.oa_1_game_all)
                                                            : getResourceString(R.string.oa_x_games_all, iGamesLeader);
                    } else {
                        String sLeader    = model.getName(pLeaderInGames);
                        String sGameScore = x_GamesTo_y(iGamesLeader, iGamesTrailer, R.string.oa_game, R.string.oa_games, m_context);
                        if ( model.matchHasEnded() ) {
                            if ( gsmModel != null ) {
                                // todo
                            } else {
                                sGamesScoreText = getResourceString(R.string.oa_a_wins_xGamesToy , sLeader, sGameScore);
                            }
                        } else {
                            sGamesScoreText = getResourceString(R.string.oa_a_leads_xGamesToy, sLeader, sGameScore);
                        }
                    }
                    setTextToSpeak(SpeechType.GamesScore, sGamesScoreText);
                } else {
                    if (gsmModel != null) {
                        List<Map<Player, Integer>> gamesWonPerSet = gsmModel.getGamesWonPerSet();
                        Map<Player, Integer> gamesWonPreviousSet = gamesWonPerSet.get(gamesWonPerSet.size()-2);
                        Player pWinnerInGames = MapUtil.getMaxKey(gamesWonPreviousSet, Player.A);
                        int    iGamesWinner   = gamesWonPreviousSet.get(pWinnerInGames);
                        int    iGamesLoser    = gamesWonPreviousSet.get(pWinnerInGames.getOther());
                        String sWinner        = model.getName(pWinnerInGames);
                        int    iSetNr         = gsmModel.getSetNrInProgress() - 1;
                        String sGameScore     = x_GamesTo_y(iGamesWinner, iGamesLoser, R.string.oa_game, R.string.oa_games, m_context);
                        String sSetScoreText  = getResourceString(R.string.oa_a_wins_set_b_xGamesToy__TennisPadel, sWinner, iSetNr, sGameScore);
                        setTextToSpeak(SpeechType.GamesScore, sSetScoreText);

                        Map<Player, Integer> setsWon = gsmModel.getSetsWon();
                        int iMax = MapUtil.getMaxValue(setsWon);
                        int iMin = MapUtil.getMinValue(setsWon);
                        String sSetScore = null;
                        if ( iMax == iMin ) {
                            if ( iMax == 1 ) {
                                sSetScore = getResourceString(R.string.oa_1_set_all__TennisPadel);
                            } else {
                                sSetScore = getResourceString(R.string.oa_x_sets_all__TennisPadel, iMax);
                            }
                        } else {
                            Player pLeaderInSets = MapUtil.getMaxKey(setsWon, Player.A);
                            String sLeaderSets   = model.getName(pLeaderInSets);
                            int    iSetsLeader   = setsWon.get(pLeaderInSets);
                            int    iSetsTrailer  = setsWon.get(pLeaderInSets.getOther());
                            String sxSetsToy     = x_GamesTo_y(iSetsLeader, iSetsTrailer, R.string.oa_set, R.string.oa_sets, m_context);
                            sSetScore = getResourceString(R.string.oa_a_leads_xGamesToy, sLeaderSets, sxSetsToy);
                        }
                        setTextToSpeak(SpeechType.GSMSetScore, sSetScore);
                    }
                }
            }

            if ( (gsmModel != null) && (iServer >= GSMModel.NUMBER_OF_POINTS_TO_WIN_GAME-1) && (gsmModel.isTieBreakGame() == false) ) {
                int iResGoldenPointOrDeuce = (gsmModel.getGoldenPointFormat().onDeuceNumber() >= 0 && iServer >= GSMModel.NUMBER_OF_POINTS_TO_WIN_GAME - 1 + gsmModel.getGoldenPointFormat().onDeuceNumber() ) ? R.string.oa_golden_point : R.string.oa_deuce;
                String sDeuceOrGoldenPoint = getResourceString(iResGoldenPointOrDeuce);
                setTextToSpeak(SpeechType.ScoreServer  , sDeuceOrGoldenPoint);
                setTextToSpeak(SpeechType.ScoreReceiver, "");
            } else {
                if ( model.matchHasEnded() ) {
                    // 0-0 no longer needs to be announced
                } else {
                    setTextToSpeak(SpeechType.ScoreServer, sServer);
                    setTextToSpeak(SpeechType.ScoreReceiver, sXAll__or_sXEqual);
                }
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
        //if ( Brand.isNotSquash() ) { return; }

        Player[] possibleGameBallFor = model.isPossibleGameBallFor();
        Boolean bIsGameball = (possibleGameBallFor != null) && (possibleGameBallFor.length != 0);

        String sText = "";
        if ( bIsGameball ) {
            int iResId = Brand.getGameSetBallPoint_ResourceId();
            if ( model instanceof GSMModel ) {
                // in tennis/padel 'Game ball' is not spoken
            } else {
                sText = getResourceString(iResId);
            }

            if ( model instanceof GSMModel ) {
                GSMModel gsmModel = (GSMModel) model;
                // TODO: set ball
                Player[] possibleSetBallFor = gsmModel.isPossibleSetVictoryFor();
                Boolean bIsSetBall = (possibleSetBallFor != null) && (possibleSetBallFor.length != 0);
                if ( bIsSetBall ) {
                    sText = getResourceString(R.string.oa_set_ball);
                }
            }

            Player[] possibleMatchBallFor = model.isPossibleMatchBallFor();
            Boolean bIsMatchBall = (possibleMatchBallFor != null) && (possibleMatchBallFor.length != 0);
            if ( bIsMatchBall ) {
                sText = getResourceString(R.string.oa_matchball);
            }
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
        emptySpeechQueue_Restartable(RECALC_BEFORE_START, iDelay/*, 0*/);
    }

    /** for playing of score on request */
    public void playAll(Model matchModel) {
        if ( matchModel == null ) { return; }
        if ( isStarted() == false ) { return; }
        if ( isEnabled() == false ) { return; }

        if ( Brand.isSquash() ) {
            this.handout(matchModel);
        }
        if ( matchModel != null ) {
            this.score(matchModel);
            this.gameBall(matchModel);
        }

        emptySpeechQueue(0/*, 0*/);
    }

    /** get text from correct locale */
    private String getResourceString(int p, Object ... args) {
        p = PreferenceValues.getSportSpecificSuffixedResId(m_context, p); // e.g. for Squash Game=Satz, but for TennisPadel Game=Spiel. Hence we have oa_game=Game and Satz and oa_game__TennisPadel=Game and Spiel
        Resources resources = PreferenceValues.newResources(m_context.getResources(), m_locale);
        return resources.getString(p, args);
    }
    private String x_GamesTo_y(int iGamesLeader, int iGamesTrailer, int iResIdGameOrSet, int iResIdGamesOrSets, Context ctx) {
        return iGamesLeader
                + " " + (iGamesLeader==1?getResourceString(iResIdGameOrSet):getResourceString(iResIdGamesOrSets))
                + " " + getResourceString(R.string.oa_x_games_TO_y)
                + " " + (iGamesTrailer==0?getResourceString(R.string.oa_love):String.valueOf(iGamesTrailer));
    }

    //------------------------------
    // the actual storage of what to say and actually speak it
    //------------------------------

    /** text fragments will be announced in this sequence they are defined in this enumeration */
    private enum SpeechType {
      //Call,
      //StartAnnouncement,
        Handout,
        GamesScore,
        GSMSetScore,
        ScoreServer,
        ScoreReceiver,
        Gameball,
      //EndAnnouncement,
        TimerRelated,
        Other,
    }
    private String[] m_sText = new String[SpeechType.values().length];
    private void setTextToSpeak(SpeechType type, Object oText) {
        if ( oText == null ) { return; }
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
                m_textToSpeech.speak(sText, TextToSpeech.QUEUE_ADD, bundle, type.toString());

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

    private void emptySpeechQueue(int iStartAtStep/*, int iCntFromDelayed*/) {
        if ( isStarted() == false ) { return; }
        if ( btAudioConnection != null && iStartAtStep == 0  ) {
            btAudioConnection.reset("Start emptying queue");
        }

        for(SpeechType type: SpeechType.values() ) {
            if ( type.ordinal() < iStartAtStep ) { continue; }
            String sToSpeak = m_sText[type.ordinal()];
            if ( StringUtil.isEmpty(sToSpeak) ) {
                continue;
            }

            m_sText[type.ordinal()] = null;
            Log.d(TAG, String.format("Speaking %s : %s", type, sToSpeak));
            speak(sToSpeak, type);
            //emptySpeechQueue_Delayed(type.ordinal() + 1, m_iDelayBetweenTwoPieces, 0);
            return;
        }

        // just played some audio, but nothing more to play. Restart keep alive
        if ( btAudioConnection != null ) {
            btAudioConnection.reset("Queue has been emptied");
        }
    }
    private void emptySpeechQueue_Delayed(final int iStartAtStep, int iDelayMs/*, final int iCntDelayedBecauseStillSpeaking*/) {
        synchronized (this) {
            try {
                wait(iDelayMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
/*
        if ( iStartAtStep == RECALC_BEFORE_START ) {
            playAll(ScoreBoard.getMatchModel());
        }
*/
        emptySpeechQueue(iStartAtStep/*, iCntDelayedBecauseStillSpeaking*/);
    }

    private static CountDownTimer m_cdtEmptySpeechQueue = null;
    private void emptySpeechQueue_Restartable(final int iStartAtStep, int iDelayMs) {
        if ( m_cdtEmptySpeechQueue != null) {
            m_cdtEmptySpeechQueue.cancel();
        }
        m_cdtEmptySpeechQueue = new CountDownTimer(iDelayMs, 100) {
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                if ( iStartAtStep == RECALC_BEFORE_START ) {
                    playAll(ScoreBoard.getMatchModel());
                }
                emptySpeechQueue(iStartAtStep);
            }
        };
        m_cdtEmptySpeechQueue.start();
    }
}
