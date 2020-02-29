/*
 * Copyright (C) 2017  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            case Badminton:
                m = new BadmintonModel();
                break;
            case Racquetball:
                m = new RacquetballModel();
                break;
            case Padel:
                m = new PadelModel();
                break;
        }
        return m;
    }

    private static SquashModel      mTmpSquash      = null;
    private static RacketlonModel   mTmpRacketlon   = null;
    private static TabletennisModel mTmpTabletennis = null;
    private static BadmintonModel   mTmpBadminton   = null;
    private static RacquetballModel mTmpRacketball  = null;
    private static PadelModel       mTmpPadel       = null;
    public static Model getTemp() {
        return getTemp(Brand.brand);
    }
    private static Model getTemp(Brand type) {
        Model mTmp = null;
        switch (type.getSportType()) {
            case Squash:
                if ( mTmpSquash == null ) {
                    mTmpSquash = new SquashModel(false);
                }
                mTmp = mTmpSquash;
                break;
            case Racketlon:
                if ( mTmpRacketlon == null ) {
                    mTmpRacketlon = new RacketlonModel();
                }
                mTmp = mTmpRacketlon;
                break;
            case Tabletennis:
                if ( mTmpTabletennis == null ) {
                    mTmpTabletennis = new TabletennisModel();
                }
                mTmp = mTmpTabletennis;
                break;
            case Badminton:
                if ( mTmpBadminton == null ) {
                    mTmpBadminton = new BadmintonModel();
                }
                mTmp = mTmpBadminton;
                break;
            case Racquetball:
                if ( mTmpRacketball == null ) {
                    mTmpRacketball = new RacquetballModel();
                }
                mTmp = mTmpRacketball;
                break;
            case Padel:
                if ( mTmpPadel == null ) {
                    mTmpPadel = new PadelModel();
                }
                mTmp = mTmpPadel;
                break;
        }
        mTmp.clear();
        return mTmp;
    }
}
