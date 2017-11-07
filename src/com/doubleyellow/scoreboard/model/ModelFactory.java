package com.doubleyellow.scoreboard.model;

import com.doubleyellow.scoreboard.Brand;

public class ModelFactory
{
    public static Model getModel(SportType type) {
        Model m = null;
        switch (type) {
            case Squash:
                m = new SquashModel();
                break;
            case Racketlon:
                m = new RacketlonModel();
                break;
            case Tabletennis:
                m = new TabletennisModel();
                break;
            case Racquetball:
                m = new RacquetballModel();
                break;
        }
        return m;
    }

    private static SquashModel      mTmpSquash      = new SquashModel(false);
    private static RacketlonModel   mTmpRacketlon   = new RacketlonModel();
    private static TabletennisModel mTmpTabletennis = new TabletennisModel();
    private static RacquetballModel mTmpRacketball  = new RacquetballModel();
    public static Model getTemp() {
        return getTemp(Brand.brand);
    }
    private static Model getTemp(Brand type) {
        Model mTmp = null;
        switch (type.getSportType()) {
            case Squash:
                mTmp = mTmpSquash;
                break;
            case Racketlon:
                mTmp = mTmpRacketlon;
                break;
            case Tabletennis:
                mTmp = mTmpTabletennis;
                break;
            case Racquetball:
                mTmp = mTmpRacketball;
                break;
        }
        mTmp.clear();
        return mTmp;
    }
}
