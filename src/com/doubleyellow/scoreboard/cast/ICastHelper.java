package com.doubleyellow.scoreboard.cast;

import android.app.Activity;
import android.view.Menu;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.timer.TimerView;

import java.util.Map;

public interface ICastHelper {
    void initCasting(ScoreBoard activity);

    void initCastMenu(Activity activity, Menu menu, int iResIdMenuItem);

    void startCast();
    void stopCast();
    void pauseCast();
    void resumeCast();

    boolean isCasting();


    void setModelForCast(Model matchModel);

    TimerView getTimerView();

    void castColors(Map<ColorPrefs.ColorTarget, Integer> mColors);

    void castDurationChronos();

    void castGamesWonAppearance();
}
