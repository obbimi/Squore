/*
 * Copyright (C) 2019  Iddo Hoeve
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

import com.doubleyellow.util.MapUtil;

import java.util.Map;

/**
 * http://www.worldbadminton.com/rules/
 *
 * http://www.worldbadminton.com/rules/history.htm
 * http://www.worldbadminton.com/ibf_laws_200208.htm
 *
 * TODO:
 * 7.4 If the score becomes 20-all, the side which gains a two point lead first, shall win that game.
 * 7.5 If the score becomes 29-all, the side scoring the 30th point shall win that game.
 *
 * Doubles:
 * 11.1.3 The player of the receiving side who served last shall stay in the same service court from where he served last. The reverse pattern shall apply to the receiver's partner.
 *
 * 11.4 Sequence of serving
 *      In any game, the right to serve shall pass consecutively:
 * 11.4.1 from the initial server who started the game from the right service court
 * 11.4.2 to the partner of the initial receiver.
 * 11.4.3 to the partner of the initial server
 * 11.4.4 to the initial receiver,
 * 11.4.5 to the initial server and so on.

 * 11.6 Either player of the winning side may serve first in the next game, and either player of the losing side may receive first in the next game.
 *
 * 16.2 Intervals:
 * 16.2.1 not exceeding 60 seconds during each game when the leading score reaches 11 points; and
 * 16.2.2 not exceeding 120 seconds between the first and second game, and between the second and third game shall be allowed in all matches.
 */
public class BadmintonModel extends Model
{
    BadmintonModel() {
        super();
        setTiebreakFormat(TieBreakFormat.TwoClearPoints);
        setNrOfGamesToWinMatch(2);
        setNrOfPointsToWinGame(21);
        setEnglishScoring(false); // default, but may be true
    }

    @Override public SportType getSport() {
        return SportType.Badminton;
    }

    @Override public Sport getSportForGame(int iGame1B) {
        return Sport.Badminton;
    }

    //-------------------------------
    // Toweling
    //-------------------------------

    public boolean isTowelingDownScore(int iEveryXpoints, int iIfHighestScoreEquals) {
        return getMaxScore() == iIfHighestScoreEquals && getNrOfPointsToWinGame() == 21;
    }

    //-------------------------------
    // serve side/sequence
    //-------------------------------

  //private Player       m_previousServingTeam   = null;
  //private DoublesServe m_previousServingPlayer = null;
  //private ServeSide    m_previousServerSide    = null;
    @Override void setServerAndSide(Player newServer, ServeSide side, DoublesServe doublesServe) {
        // serves changes from one team to the other: remember who served from where
/*
        Player oldServer = getServer();
        if ( newServer.equals(oldServer) && isDoubles() ) {
            //m_previousServingTeam   = null;
            m_previousServerSide    = null;
            m_previousServingPlayer = null;
        } else {
            //m_previousServingTeam   = oldServer;
            m_previousServerSide    = getNextServeSide(oldServer);
            m_previousServingPlayer = getNextDoubleServe(oldServer);
        }
*/
        if ( (doublesServe != null) && doublesServe.equals(DoublesServe.NA) == false ) {
            // for both teams keep R=O and L=I and in sync
            if ( doublesServe.ordinal() == side.ordinal() ) {
                doublesServe = doublesServe.getOther();
            }
        }

        super.setServerAndSide(newServer, side, doublesServe);
    }

    @Override DoublesServe determineDoublesReceiver(DoublesServe serverOfOppositeTeam, ServeSide serveSide) {
/*
        if ( m_previousServingPlayer != null ) {
            if ( serveSide.equals(m_previousServerSide) ) {
                return
            }
        } else {
            // server remains to same to team same player, just other player will be receiving
            return getDoubleReceiver().getOther();
        }
*/
        return serverOfOppositeTeam;
    }

    @Override void setLastPointWasHandout(boolean b) {
        // handout is 'abused' for doubles to see if we need to auto-swap players to have serving right-to-right or left-left
        super.setLastPointWasHandout(b);
        if ( isDoubles() && (b == false) ) {
            swapDoublesPlayerNames(getServer());
        }
    }

    public boolean setDoublesServeSequence(DoublesServeSequence dsq) {
        return super._setDoublesServeSequence(dsq);
    }

    @Override public DoublesServeSequence getDoubleServeSequence(int iGameZB) {
        return m_doubleServeSequence; // default DoublesServeSequence.A1B1A2B2
    }

    @Override void determineServerAndSideForUndoFromPreviousScoreLine(ScoreLine lastValidWithServer, ScoreLine slRemoved) {
        if ( lastValidWithServer != null ) {
            Player    pServer              = lastValidWithServer.getScoringPlayer();
            ServeSide serversPreferredSide = MapUtil.getMaxKey(m_player2ServeSideCount.get(pServer), ServeSide.R);
            Player    lastServer           = lastValidWithServer.getServingPlayer();
            ServeSide lastServeSide        = lastValidWithServer.getServeSide();
            // savety precaution... should not occur
            if ( lastServeSide == null ) {
                lastServeSide = ServeSide.R;
            }
            ServeSide nextServeSide        = serversPreferredSide;
            if ( pServer.equals(lastServer) && (lastServeSide != null) ) {
                nextServeSide = lastServeSide.getOther();
                if ( isDoubles() ) {
                    m_in_out          = m_in_out.getOther();
                    m_in_out_receiver = m_in_out_receiver.getOther();
                    swapDoublesPlayerNames(pServer);
                }
            }

            setServerAndSide(pServer, nextServeSide, m_in_out);
        } else if (slRemoved != null ) {
            Player    removedServingPlayer = slRemoved.getServingPlayer();
            ServeSide removedServeSide     = slRemoved.getServeSide();
            if ( removedServingPlayer != null ) {
                setServerAndSide(removedServingPlayer, null, null);
            }
            if ( removedServeSide != null ) {
                setServerAndSide(null, removedServeSide, null);
            }
        }
        setLastPointWasHandout(false);
    }

    @Override Player determineServerForNextGame(int iGameZB, int iScoreA, int iScoreB) {
        return determineServerForNextGame_SQ_BM(iScoreA, iScoreB);
    }

    @Override public String convertServeSideCharacter(String sRLInternational, ServeSide serveSide, String sHandoutChar) {
        return sRLInternational /* + sHandoutChar*/;
    }

    @Override public boolean showChangeSidesMessageInGame(int iGameZB) {
        // only in the last game when 11 points are reached (in a game to 21)
        if ( iGameZB + 1 == getNrOfGamesToWinMatch() * 2 - 1 ) {
            return true;
        }
        return false;
    }

    //-------------------------------
    // game/match ball
    //-------------------------------

    @Override Player[] calculateIsPossibleGameVictoryFor(When when, Map<Player, Integer> gameScore, boolean bFromIsMatchBallFrom) {
        if ( getNrOfPointsToWinGame() == 21 ) {
            switch (when) {
                case Now:
                    if ( getMaxScore() == 30 && getDiffScore() != 0 ) {
                        Player pLeadingPlayer = MapUtil.getMaxKey(gameScore, null);
                        return new Player[] { pLeadingPlayer };
                    }
                    break;
                case ScoreOneMorePoint:
                    // At 29 all, the side scoring the 30th point, wins that game.
                    if ( getMaxScore() == 29 && getDiffScore() == 0 ) {
                        return new Player[] { Player.A, Player.B };
                    }
                    break;
            }
        }
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
        super.changeScore_TT_BM_RL(player, getSport());
    }

    //-------------------------------
    // JSON
    //-------------------------------

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
