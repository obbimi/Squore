package com.doubleyellow.scoreboard.timer;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

public class PauseTimerView extends TwoTimerView
{
    public PauseTimerView(ScoreBoard scoreBoard, Model matchModel) {
        super(scoreBoard, matchModel);
    }

    @Override public void init(boolean bAutoTriggered) {
        super.init(Type.UntillStartOfNextGame, bAutoTriggered);
    }
}
