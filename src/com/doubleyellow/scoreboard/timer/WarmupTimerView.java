package com.doubleyellow.scoreboard.timer;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

public class WarmupTimerView extends TwoTimerView
{
    public WarmupTimerView(ScoreBoard scoreBoard, Model matchModel) {
        super(scoreBoard, matchModel);
    }

    @Override public void init(boolean bAutoTriggered) {
        super.init(Type.Warmup, bAutoTriggered);
    }
}
