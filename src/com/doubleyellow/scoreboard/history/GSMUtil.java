/*
 * Copyright (C) 2024  Iddo Hoeve
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
package com.doubleyellow.scoreboard.history;

import android.util.Log;

import com.doubleyellow.scoreboard.model.GSMModel;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.model.ScoreLine;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.MapUtil;

import java.util.List;
import java.util.Map;

public class GSMUtil
{
    private static final String TAG = "SB." + GSMUtil.class.getSimpleName();

    private static final int finalSetTieBreakOnlyNoGames_NegativeOffset = -100000;
    private static final int normalTiebreak_NegativeOffset = -1000;
    public static List<Map<Player, Integer>> gsmGamesWonPerSet(GSMModel gsmModel, boolean bEncodeTieBreakScore) {
        List<Map<Player, Integer>> endScores = gsmModel.getGamesWonPerSet(bEncodeTieBreakScore);
        // for all tiebreaks turn into large negatieve number encoding both nr of games won as wel as number of points won in tiebreak
        int iNrOfSets = ListUtil.size(endScores);
        if ( iNrOfSets > 0 ) {
            int iSetNrZB = -1;
            for(Map<Player, Integer> setScore: endScores) {
                iSetNrZB++;
                int iMaxNrOfGamesInSet = MapUtil.getMaxValue(setScore);
                int iMinNrOfGamesInSet = MapUtil.getMinValue(setScore);
                int nrOfGamesToWinSet  = gsmModel.getNrOfGamesToWinSet(iSetNrZB + 1);
                boolean bNormalTieBreak = iMaxNrOfGamesInSet >= nrOfGamesToWinSet + 1 && (iMaxNrOfGamesInSet - iMinNrOfGamesInSet==1);
                boolean bFinalSetTiebreakNoGames = gsmModel.matchHasEnded() && iMaxNrOfGamesInSet == 1;
                if ( bNormalTieBreak || bFinalSetTiebreakNoGames ) {
                    Player pSetWinner = MapUtil.getMaxKey(setScore, Player.A);
                    Player pSetLoser =  pSetWinner.getOther();
                    if ( setScore.get(pSetLoser) == gsmModel.getNrOfGamesToWinSet() || bFinalSetTiebreakNoGames) {
                        // still not 100% it was a tiebreak if it is the set in progress
                        List<List<ScoreLine>> gameScoreLinesOfSet = gsmModel.getGameScoreLinesOfSet(iSetNrZB);
                        List<ScoreLine> tieBreakScorelines = ListUtil.getLast(gameScoreLinesOfSet);
                        int iTBScoreOfLoser  = 0;
                        int iTBScoreOfWinner = 0;
                        for(ScoreLine line: tieBreakScorelines) {
                            if ( pSetLoser.equals(line.getScoringPlayer()) ) {
                                iTBScoreOfLoser = line.getScore();
                            }
                            if ( pSetWinner.equals(line.getScoringPlayer()) ) {
                                iTBScoreOfWinner = line.getScore();
                            }
                        }
                        //setScore.put(pSetLoser, -1 * iScoreOfLoser);
                        if ( bEncodeTieBreakScore ) {
                            if ( bFinalSetTiebreakNoGames ) {
                                setScore.put(pSetLoser , (setScore.get(pSetLoser )+1) * finalSetTieBreakOnlyNoGames_NegativeOffset - iTBScoreOfLoser);
                                setScore.put(pSetWinner, (setScore.get(pSetWinner)+1) * finalSetTieBreakOnlyNoGames_NegativeOffset - iTBScoreOfWinner);
                            } else {
                                // normal tiebreak
                                setScore.put(pSetLoser , (setScore.get(pSetLoser )+1) * normalTiebreak_NegativeOffset - iTBScoreOfLoser);
                                setScore.put(pSetWinner, (setScore.get(pSetWinner)+1) * normalTiebreak_NegativeOffset - iTBScoreOfWinner);
                            }
                        }
                    }
                }
            }
        }
        return endScores;
    }

    public static GSMTieBreakType getTiebreakType(int iGamesFor1, int iGamesFor2) {
        boolean bTiebreak                      = iGamesFor1 <= GSMUtil.normalTiebreak_NegativeOffset              && iGamesFor2 <= GSMUtil.normalTiebreak_NegativeOffset;
        boolean bTiebreakInFinalSetWithNoGames = iGamesFor1 <= GSMUtil.finalSetTieBreakOnlyNoGames_NegativeOffset && iGamesFor2 <= GSMUtil.finalSetTieBreakOnlyNoGames_NegativeOffset;
        if ( bTiebreakInFinalSetWithNoGames ) {
            return GSMTieBreakType.FinalSetNoGames;
        } else if ( bTiebreak ) {
            return GSMTieBreakType.Normal;
        } else {
            return null;
        }
    }

    /**
     * decode the hugh negative number back into number of games won and number of points won in tiebreak
     */
    public static int[] gamesWonAndTiebreakPoints(int iGameScoreEncoded, GSMTieBreakType gsmTieBreakType) {
        int iHughNegative = GSMTieBreakType.FinalSetNoGames.equals(gsmTieBreakType) ? GSMUtil.finalSetTieBreakOnlyNoGames_NegativeOffset : GSMUtil.normalTiebreak_NegativeOffset;
        int iHughPositive = Math.abs(iHughNegative);

        int iGamesFor = Math.abs(iGameScoreEncoded);
        int iTBPoints = iGamesFor % iHughPositive;
        iGamesFor = ((iGamesFor - iTBPoints) / iHughPositive) - 1;

        return new int[] { iGamesFor, iTBPoints };
    }
}
