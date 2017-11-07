package com.doubleyellow.scoreboard.model;

import java.util.EnumSet;

public enum Halfway
{
    Before(false, null),

    /* 5-2 in a game to 11: tabletennis -> switch sides */
    JustBefore(true, EnumSet.of(SportType.Tabletennis)),
    /** 5-3 in game to 10 (even points)... less common */
    Exactly   (true, EnumSet.of(SportType.Tabletennis
                              , SportType.Racketlon)),
    /* 11-7 in a game to 21: racketlon -> switch sides */
    JustAfter (true, EnumSet.of(SportType.Racketlon)),

    After(false, null),
    ;

    private boolean isHalfway = false;
    private EnumSet<SportType> forSports;

    Halfway(boolean isHalfway, EnumSet<SportType> forSports) {
        this.isHalfway = isHalfway;
        this.forSports = forSports;
    }

    public boolean isHalfway() {
        return isHalfway;
    }

    public boolean changeSidesFor(SportType sport) {
        return this.forSports != null && this.forSports.contains(sport);
    }
}
