package com.doubleyellow.scoreboard.timer;

import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;

public class InjuryTimerView extends TwoTimerView
{
    public InjuryTimerView(ScoreBoard scoreBoard, Model matchModel, Type timerType) {
        super(scoreBoard, matchModel);
        m_timerType = timerType;
    }

    @Override public void init(boolean bAutoTriggered) {
        super.init(m_timerType, bAutoTriggered);
    }
}
