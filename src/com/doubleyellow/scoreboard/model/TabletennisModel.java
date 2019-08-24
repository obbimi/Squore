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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * http://www.allabouttabletennis.com/table-tennis-terminology.html
 *
 * A player or pair may claim one time-out period of up to 1 minute during an individual match.
 * If a valid request for a time-out is made simultaneously by or on behalf of both players or pairs,
 * play will resume when both players or pairs are ready or at the end of 1 minute,
 * whichever is the sooner, and neither player or pair shall be entitled to another time-out during that individual match.
 *
 * Players are entitled to practise on the match table for up to 2 minutes immediately before the start of a match but not during normal intervals; the specified practice period may be extended only with the permission of the referee.
 * 3.04.04.01.01  	an interval of up to 1 minute between successive games of an individual match;
 * 3.04.04.02  	A player or pair may claim one time-out period of up to 1 minute during an individual match.
 * 3.04.04.04  	The referee may allow a suspension of play, of the shortest practical duration, and in no circumstances more than 10 minutes, if a player is temporarily incapacitated by an accident, provided that in the opinion of the referee the suspension is not likely to be unduly disadvantageous to the opposing player or pair.
 *
 * The player or pair starting at one end in a game shall start at the other end in the next game of the match and in the last possible game of a match the players or pairs shall change ends when first one player or pair scores 5 points.
 *
 */
public class TabletennisModel extends Model
{
    TabletennisModel() {
        super();
        setTiebreakFormat(TieBreakFormat.TwoClearPoints);
        setNrOfGamesToWinMatch(4);
        setNrOfServesPerPlayer(2); // TODO: set it from user specification
        setEnglishScoring(false);
    }

    @Override public SportType getSport() {
        return SportType.Tabletennis;
    }

    @Override public Sport getSportForGame(int iGame1B) {
        return Sport.Tabletennis;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

    @Override public DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return DoublesServeSequence.A1B1A2B2;
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        super.determineServerAndSide_Racketlon_Tabletennis(true, getSport());
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_Racketlon_Tabletennis(iGameZB, true);
    }

    /** LR and Handout parameters are totally ignored. Returns character to indicate number of serves left */
    @Override public Object convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        if ( isInExpedite() ) {
            // TODO: special char for when in expedite
            return "X";
        }
        boolean inTieBreak = isInTieBreak_Racketlon_Tabletennis();
        if ( m_iNrOfServesPerPlayer > 2 ) {
            // not the default for tabletennis, but possible
            if ( inTieBreak ) {
                return 1;
            }
            int iA = getScore(Player.A);
            int iB = getScore(Player.B);
            int iTotalInGame = iA + iB;
            //int i=0;
            //for(; i < iTotalInGame - m_iNrOfServesPerPlayer; i += m_iNrOfServesPerPlayer ) { }
            return m_iNrOfServesPerPlayer - (iTotalInGame % m_iNrOfServesPerPlayer);
        }
        if ( (m_iNrOfServesPerPlayer == 1) || inTieBreak ) {
            return 1; // single serve only
        }
        return 2 - serveSide.ordinal(); // R means 2 serves left, L means 1 left
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        // only in the last game when 5 points are reached (in a game to 11)
        if ( iGameZB + 1 == getNrOfGamesToWinMatch() * 2 - 1 ) {
            return true;
        }
        return false;
    }

    //-------------------------------
    // Expedite system
    //-------------------------------

    public enum Mode {
        Expedite,
    }
    private boolean isInExpedite() {
        return isInMode(Mode.Expedite);
    }

    @Override void determineServerAndSide_Racketlon_Tabletennis(boolean bForUndo, SportType sportType) {
        if ( isInExpedite() ) {
            Player server = getServer();
            setServerAndSide(server.getOther(), ServeSide.L, null);
        } else {
            super.determineServerAndSide_Racketlon_Tabletennis(bForUndo, sportType);
        }
    }

    //-------------------------------
    // Toweling
    //-------------------------------

    public boolean isTowelingDownScore(int iEveryXpoints, int iIfHighestScoreEquals) {
        if (iEveryXpoints <= 0) { return false; }
        return getTotalGamePoints() % iEveryXpoints == 0;
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        return super.calculateIsPossibleGameVictoryFor_SQ_TT_BM_RL(when, gameScore);
    }

    @Override Player[] calculatePossibleMatchVictoryFor(When when, Player[] pGameVictoryFor) {
        return super.isPossibleMatchBallFor_SQ_TT_BM(when, pGameVictoryFor);
    }

    //-------------------------------
    // score
    //-------------------------------

    @Override public String getResultShort() {
        return super.getResultShort_SQ_TT_BM();
    }

    @Override public void changeScore(Player player) {
        super.changeScore_Racketlon_Tabletennis(player, getSport());
    }

    //-------------------------------
    // JSON
    //-------------------------------

    @Override public JSONObject getJsonObject(Context context, JSONObject oSettings) throws JSONException {
        JSONObject jsonObject = super.getJsonObject(context, oSettings);

        jsonObject.put(JSONKey.nrOfServersPerPlayer.toString(), getNrOfServesPerPlayer());

        return jsonObject;
    }

    @Override public JSONObject fromJsonString(String sJson, boolean bStopAfterEventNamesDateTimeResult) {
        JSONObject joMatch = super.fromJsonString(sJson, bStopAfterEventNamesDateTimeResult);
        if ( joMatch != null ) {
            int i = joMatch.optInt(JSONKey.nrOfServersPerPlayer.toString(), 2);
            setNrOfServesPerPlayer(i);
        }
        return joMatch;
    }

    //-------------------------------
    // conduct/appeal
    //-------------------------------

    @Override public void recordAppealAndCall(Player appealing, Call call) {
        // not applicable
    }

    @Override public void recordConduct(Player pMisbehaving, Call call, ConductType conductType) {
        // not applicable
    }
}
