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

import android.content.Context;
import com.doubleyellow.util.JsonUtil;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * http://www.racketlon.net/sites/default/files/FIR%20-%20Rules%20of%20Racketlon%20(2017-01-01),%20FINAL.pdf
 */
public class RacketlonModel extends Model {

    RacketlonModel() {
        super();
        setTiebreakFormat(TieBreakFormat.TwoClearPoints);
        setEnglishScoring(false);
        setNrOfPointsToWinGame(21);
        //setNrOfGamesToWinMatch(3);
    }

    // If, after all 4 sets, both players have exactly the same number of points a "Gummiarm point" shall be decisive
    // Start serving in Tabletennis (1) means start serving in Squash (3) and start receiving in Badminton (2) and Tennis (4)
    // At the first of these two serves the server always serves from the right (except in table tennis, of course) to left. The second serve is from the left to the right side
    // Ends are switched at the time when 11 points are first reached by any of the players. (IH: except squash)
    // After 20-20 the serve switches hand after every point until the set is decided. The two first serves are from the right side , the two next serves are from the left and so on.
    // (Thus player A serves from the right, then player B serves from the right followed by player A from the left and player B from the left.)

    // A maximum break of one minute shall be allowed at 11 (i.e. when 11 points is first reached by any of the players) in each set.

    // The break between sets shall be maximised at "3+3" minutes meaning:
    // (a) Warming up at the next sport has to commence within 3 minutes after the end of the previous set.
    // (b) The next set has to commence within 6 minutes after the end of the previous set


    private Sport getSportForSetInProgress() {
        return getSportForGame(getGameNrInProgress());
    }

    @Override public SportType getSport() {
        return SportType.Racketlon;
    }

    @Override public Sport getSportForGame(int iSet1B) {
        List<Sport> disciplines = getDisciplines();
        int iSetZB = ( iSet1B - 1 ) % ListUtil.size(disciplines);
        if ( iSetZB < 0 ) {
            iSetZB += ListUtil.size(disciplines);
        }
        return disciplines.get(iSetZB);
    }

    private List<Sport> m_lDisciplines = null;

    /** called from e.g. next set dialog */
    public void setDiscipline(int iSetZB, Sport sport) {
        List<Sport> disciplines = getDisciplines();
        int iCurrentPos = disciplines.indexOf(sport);
        if ( iCurrentPos == iSetZB ) {
            return;
        }
        disciplines.remove(sport);
        disciplines.add(iSetZB, sport);
        setDirty();
    }

    public void setDisciplines(Collection<Sport> lDisciplines) {
        m_lDisciplines = new ArrayList<>(lDisciplines);
    }

    public List<Sport> getDisciplines() {
        // may be (but usually is not) in deviating sequence
        if ( ListUtil.size(m_lDisciplines) == 0 ) {
            // construct a mutatable list
            m_lDisciplines = new ArrayList<>(Arrays.asList(Sport.Tabletennis, Sport.Badminton, Sport.Squash, Sport.Tennis));
        }
        return m_lDisciplines;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    @Override public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return false; // can not be changed, determines on discipline
    }

    @Override public DoublesServeSequence getDoubleServeSequence(int iSetZB) {
        Sport disicpline = getSportForGame(iSetZB+1);
        DoublesServeSequence ds = null;
        switch (disicpline) {
            case Tabletennis:
                ds = DoublesServeSequence.A1B1A2B2;
                break;
            case Badminton:
                ds = DoublesServeSequence.A1B1A2B2;
                break;
            case Squash:
                // only single player on court
                ds = DoublesServeSequence.A1B1A1B1;
                break;
            case Tennis:
                ds = DoublesServeSequence.A1B1A2B2;
                break;
        }
        return ds;
    }

    /** What is returned is based on the discipline in progress. Can be a String or an Integer */
    @Override public Object convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        Sport sport = getSportForSetInProgress();
        String s1or2_forRorL = String.valueOf(serveSide.ordinal() + 1);
        if ( sport.equals(Sport.Tabletennis) ) {
            if ( isInTieBreak_TT_RL() ) {
                return 1; // single serve only
            }
            return 2 - serveSide.ordinal(); // R means 2 serves left, L means 1 left
        } else {
            if ( isInTieBreak_TT_RL() ) {
                sRLInternational = s1or2_forRorL + sRLInternational;
            }
        }
        return sRLInternational;
    }

    @Override public boolean showChangeSidesMessageInGame(int iSetZB) {
        List<Sport> disciplines = getDisciplines();
        if ( disciplines.get(iSetZB % 4).equals(Sport.Squash) ) {
            // during squash: only for doubles
            return isDoubles();
        }
        return true;
    }

    /* concept of handout not used in racketlon. Not even when playing squash */
    //@Override public boolean isLastPointHandout() { return false; }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        super.determineServerAndSide_TT_RL(true, getSport());
    }

    @Override Player determineServerForNextGame(int iSetZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_TT_RL(iSetZB, true);
    }

    @Override public void changeSide(Player player) {
        super.changeSide(player);
        // in racketlon at zero-zero first server will always serve from the right side first
        if ( getMaxScore() == 0 && ServeSide.R.equals(m_nextServeSide) == false ) {
            setServerAndSide(player, ServeSide.R, isDoubles()?DoublesServe.I :DoublesServe.NA);
        }
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    /** in racketlon one person can have set ball while the other has match ball. Match ball is more important to show */
    @Override public Player[] isPossibleGameBallFor() {
        Player[] pSetBall    = _isPossibleGameVictoryFor(When.ScoreOneMorePoint, false);
        Player[] paMatchBall = _isPossibleMatchVictoryFor(When.ScoreOneMorePoint, pSetBall);
        if ( ListUtil.isNotEmpty(paMatchBall) ) {
            pSetBall = paMatchBall;
        }
        return pSetBall;
    }

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        Player[] pVictoryFor = super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore);
        if ( ListUtil.isEmpty(pVictoryFor) && getGameNrInProgress() >= 3 && when.equals(When.ScoreOneMorePoint) && (bFromIsMatchBallFrom == false) ) {
            // in both the 3th and 4th (last) game in racketlon, it can be a match ball while it is not game ball just looking at the points of the current game
            // if it is, interpret it as game ball as well
            pVictoryFor = _isPossibleMatchVictoryFor(when, pVictoryFor);
        }
        return pVictoryFor;
    }

    @Override public Player isPossibleMatchVictoryFor() {
        Player[] players = _isPossibleMatchVictoryFor(When.Now, null);
        if ( ListUtil.length(players) == 1 ) {
            return players[0];
        }
        return null;
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] pSetBallFor) {
        Map<Player, Integer> diffNow             = getPointsDiff(true);
        int                  nrOfPointsToWinSet  = getNrOfPointsToWinGame();
        int                  iSetInProgress1B    = getGameNrInProgress();
        int                  iDiffTotal          = MapUtil.getMaxValue(diffNow);
        Player               leaderInCurrentSet  = getLeaderInCurrentGame();

        if ( iSetInProgress1B < 3 ) {
            // if set 3 has not yet started, it can not be match(ball)
            return getNoneOfPlayers();
        }
        if ( iDiffTotal == 0 ) {
            switch (when) {
                case Now:
                    // there can be no winner if there is no difference
                    return getNoneOfPlayers();
                case ScoreOneMorePoint:
                    // it can only be match ball in the LAST set (if diff=0)
                    if ( iSetInProgress1B == 4 ) {
                        Player[] pSetIsFor = _isPossibleGameVictoryFor(When.Now, true);
                        if ( pSetBallFor == null ) {
                            pSetBallFor = _isPossibleGameVictoryFor(When.ScoreOneMorePoint, true);
                        }
                        if ( ListUtil.length(pSetIsFor) != 0 ) {
                            // diff=0 and last set has ended: gummi arm: both players have match ball
                            return getPlayers();
                        } else if ( ListUtil.length(pSetBallFor) != 0 ) {
                            // diff=0 and leader in current set has set ball
                            return new Player[] { leaderInCurrentSet };
                        }
                    } else {
                        return getNoneOfPlayers();
                    }
            }
        } else {
            Player pLeader     = MapUtil.getMaxKey(diffNow, null);
            Player pTrailer    = pLeader.getOther();

            int iNrOfPointsTrailerCanScoreInComingSets = (4 - iSetInProgress1B) * nrOfPointsToWinSet;
            int iNrOfPointsTrailerCanStillScore        = iNrOfPointsTrailerCanScoreInComingSets
                    + Math.max(getScore(pLeader) + 2, nrOfPointsToWinSet)
                    - getScore(pTrailer);
            Player[] pSetIsFor = _isPossibleGameVictoryFor(When.Now, true);
            switch (when) {
                case Now:
                    if ( (iSetInProgress1B == 4) && ( ListUtil.singleElementEquals(pSetIsFor, pLeader) ) ) {
                        // there is a difference in points and last set has finished: winner of last set is winner
                        return new Player[] { pLeader };
                    } else if ( (iSetInProgress1B == 3) && ListUtil.singleElementEquals(pSetIsFor, pLeader) && (iDiffTotal > iNrOfPointsTrailerCanScoreInComingSets) ) {
                        // if leader has won current set, and his lead will be more than trailer can catch up in only last set
                        return new Player[] { pLeader };
                    } else if ( iDiffTotal > iNrOfPointsTrailerCanStillScore ) {
                        // leader has more points than trailer can still score in current and coming sets
                        return new Player[] { pLeader };
                    }
                    break;
                case ScoreOneMorePoint:
                    if ( ListUtil.length(pSetIsFor) != 0 ) {
                        // it can not be match ball if the set in progress just ended
                        return getNoneOfPlayers();
                    }
                    if ( pSetBallFor == null ) {
                        pSetBallFor = _isPossibleGameVictoryFor(When.ScoreOneMorePoint, true);
                    }
                    int iNrOfPointsTrailerCanScoreAfterPointForLeader = iNrOfPointsTrailerCanStillScore + ((getScore(pLeader) >= nrOfPointsToWinSet-2)?1:0);
                    if ( iDiffTotal == iNrOfPointsTrailerCanScoreAfterPointForLeader ) {
                        // one more point for leader and trailer can not catch up anymore
                        return new Player[] { pLeader };
                    } else if ( ListUtil.singleElementEquals(pSetBallFor, pLeader) && (iDiffTotal >= iNrOfPointsTrailerCanScoreInComingSets) ) {
                        // leader has set ball and if he wins the set he will have more points than his opponent can still score
                        return new Player[] { pLeader };
                    }
            }
        }
        return getNoneOfPlayers();
    }


    //-------------------------------
    // score
    //-------------------------------

    @Override public void changeScore(Player player) {
        super.changeScore_TT_BM_RL(player, getSport());
    }

    @Override public String getResultShort() {
        Map<Player, Integer> pointsDiff = getPointsDiff(true);
        Player pLeader = MapUtil.getMaxKey(pointsDiff, null);
        if ( pLeader == null ) {
            return "+0";
        }
        Integer iLead = pointsDiff.get(pLeader);
        if ( iLead == 0 ) {
            return "+0";
        }
        return pLeader + "+" + iLead;
    }

    //-------------------------------
    // JSON
    //-------------------------------

    @Override public JSONObject getJsonObject(Context context, JSONObject oSettings) throws JSONException {
        JSONObject jsonObject = super.getJsonObject(context, oSettings);

        JSONArray joDisiplines = new JSONArray(ListUtil.elementsToString(getDisciplines()));
        jsonObject.put(JSONKey.disciplineSequence.toString(), joDisiplines); // TODO: only if deviating from default, and re-read

        return jsonObject;
    }

    @Override public JSONObject fromJsonString(String sJson, boolean bStopAfterEventNamesDateTimeResult) {
        JSONObject joMatch = super.fromJsonString(sJson, bStopAfterEventNamesDateTimeResult);
        if ( (joMatch != null) ) {
            JSONArray joDisciplines = joMatch.optJSONArray(JSONKey.disciplineSequence.toString());
            if (JsonUtil.isNotEmpty(joDisciplines) ) {
                List<String> disciplines = JsonUtil.asListOfStrings(joDisciplines);
                Set<Sport> sports = ListUtil.toEnumValues(disciplines, Sport.class);
                this.setDisciplines(sports);
            }
            if ( isDoubles() ) {
                // initialize
                DoublesServe dsIO = calculateDoublesInOut_TT_RL();
                setServerAndSide(null, null, dsIO);
            }
        }
        return joMatch;
    }

    //-------------------------------
    // conduct/appeal
    //-------------------------------

    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        super.recordConduct_SQ_RL_RB(pMisbehaving, call, conductType);
    }

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        if ( getSportForSetInProgress().equals(Sport.Squash) == false ) { return; }
        super.recordAppealAndCall_SQ_RL_RB(appealing, call);
    }


/*
    @Override public String getEventDivision() {
        String eventDivision = super.getEventDivision();
        Sport sportForSetInProgress = getSportForSetInProgress();
        if ( StringUtil.isNotEmpty(eventDivision) && eventDivision.equals(sportForSetInProgress.toString()) == false ) {
            try {
                Sport sport = Sport.valueOf(eventDivision);
                // assume previous event was actually taken from Racketlon model and was stored in the actual model
                setEvent(m_sEventName, null, m_sEventRound, m_sEventLocation);
            } catch (Exception e) {
            }
        }
        if ( StringUtil.isEmpty(eventDivision) ) {
            eventDivision = sportForSetInProgress.toString();
            Log.d(TAG, "Event division " + eventDivision);
        }
        return eventDivision;
    }
*/

}
