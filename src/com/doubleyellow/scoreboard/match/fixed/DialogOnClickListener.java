/*
 * Copyright (C) 2018  Iddo Hoeve
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

package com.doubleyellow.scoreboard.match.fixed;

import android.content.DialogInterface;
import android.widget.TextView;

import com.doubleyellow.scoreboard.match.StaticMatchSelector;
import com.doubleyellow.scoreboard.prefs.NewMatchesType;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import static com.doubleyellow.scoreboard.match.ExpandableMatchSelector.HEADER_PREFIX;
import static com.doubleyellow.scoreboard.match.ExpandableMatchSelector.NAMES_SPLITTER;

/**
 * Controls the flow when defining new static matches.
 */
public class DialogOnClickListener implements DialogInterface.OnClickListener
{
    NewMatchesType       m_newMatchesType = null; /* if null, add single match to existing group, or edit match of existing group */
    StaticMatchSelector  m_smSelector     = null;
    List<List<TextView>> m_llTextViews    = null;
    String               m_sToHeader      = null;
    String               m_sEditMatch     = null;
    boolean              m_bIsReplace     = false;

    public DialogOnClickListener (NewMatchesType newMatchesType, String sToHeader, String sEditMatch, boolean bIsReplace, List<List<TextView>> llTextViews, StaticMatchSelector smSelector) {
        m_newMatchesType = newMatchesType;
        m_llTextViews    = llTextViews;
        m_sToHeader      = sToHeader;
        m_sEditMatch     = sEditMatch;
        m_bIsReplace     = bIsReplace;
        m_smSelector     = smSelector;
    }

    @Override public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (m_newMatchesType == null ) {
                    TextView txtA = m_llTextViews.get(0).get(0);
                    TextView txtB = m_llTextViews.get(0).get(1);
                    String sPlayerA = txtA.getText().toString().trim();
                    String sPlayerB = txtB.getText().toString().trim();
                    if (StringUtil.areAllNonEmpty(sPlayerA, sPlayerB)) {
                        String sNewMatch = sPlayerA + NAMES_SPLITTER + sPlayerB;
                        m_smSelector._replaceMatch(m_sToHeader, m_bIsReplace ? m_sEditMatch : null, sNewMatch);
                    }
                } else {
                    switch (m_newMatchesType) {
                        case TeamVsTeam_OneMatchPerPlayer: {
                            List<String> lMatches = new ArrayList<>();

                            List<TextView> lTextViewsA = m_llTextViews.get(0);
                            List<TextView> lTextViewsB = m_llTextViews.get(1);
                            int nrOfTxts = Math.min(lTextViewsA.size(), lTextViewsB.size());
                            for (int i = 0; i < nrOfTxts; i++) {
                                TextView txtA = lTextViewsA.get(i);
                                TextView txtB = lTextViewsB.get(i);
                                if (txtA == null || txtB == null) {
                                    continue;
                                }
                                String sPlayerA = txtA.getText().toString().trim();
                                String sPlayerB = txtB.getText().toString().trim();
                                if (StringUtil.hasEmpty(sPlayerA, sPlayerB)) {
                                    continue;
                                }
                                String sNewMatch = sPlayerA + NAMES_SPLITTER + sPlayerB;
                                lMatches.add(sNewMatch);
                            }
                            addGroupWithMatches(m_sToHeader, lMatches);
                            break;
                        }
                        case TeamVsTeam_XMatchesPlayer: {
                            List<TextView> lTextViewsA = m_llTextViews.get(0);
                            List<TextView> lTextViewsB = m_llTextViews.get(1);
                            int nrOfTxtsA = lTextViewsA.size();
                            int nrOfTxtsB = lTextViewsA.size();

                            // add matches so that same player is not (less likely) playing in 2 subsequent matches
                            List<String> lMatches = new ArrayList<>();
                            for (int iOffset = 0; iOffset < nrOfTxtsB; iOffset++) {
                                for (int iA = 0; iA < nrOfTxtsA; iA++) {
                                    TextView txtA = lTextViewsA.get(iA);
                                    if (txtA == null) {
                                        continue;
                                    }
                                    String sPlayerA = txtA.getText().toString().trim();
                                    TextView txtB = lTextViewsB.get((iA + iOffset) % lTextViewsB.size());
                                    if (txtB == null) {
                                        continue;
                                    }
                                    String sPlayerB = txtB.getText().toString().trim();
                                    if (StringUtil.hasEmpty(sPlayerA, sPlayerB)) {
                                        continue;
                                    }
                                    String sNewMatch = sPlayerA + NAMES_SPLITTER + sPlayerB;
                                    lMatches.add(sNewMatch);
                                }
                            }
    /*
                                    // add matches 'group by' players from team A
                                    for (int iA = 0; iA < nrOfTxtsA; iA++) {
                                        TextView txtA = lTextViewsA.get(iA);
                                        if (txtA == null) {
                                            continue;
                                        }
                                        String sPlayerA = txtA.getText().toString().trim();
                                        for (int iB = 0; iB < nrOfTxtsB; iB++) {
                                            TextView txtB = lTextViewsB.get(iB);
                                            if (txtB == null) {
                                                continue;
                                            }
                                            String sPlayerB = txtB.getText().toString().trim();
                                            if (StringUtil.hasEmpty(sPlayerA, sPlayerB)) {
                                                continue;
                                            }
                                            String sNewMatch = sPlayerA + NAMES_SPLITTER + sPlayerB;
                                            lMatches.add(Math.min(iA + 2 * iB,lMatches.size()), sNewMatch);
                                        }
                                    }
    */
                            addGroupWithMatches(m_sToHeader, lMatches);
                            break;
                        }
                        case Poule: {
                            List<String> lMatches = new ArrayList<>();

                            List<TextView> lTextViews = m_llTextViews.get(0);
                            int nrOfTxts = lTextViews.size();
                            for (int a = 0; a < nrOfTxts; a++) {
                                TextView txt1 = lTextViews.get(a);
                                if (txt1 == null) {
                                    continue;
                                }

                                String sPlayer = txt1.getText().toString().trim();
                                if (StringUtil.isEmpty(sPlayer)) {
                                    continue;
                                }
                                PreferenceValues.addPlayerToList(m_smSelector.getContext(), sPlayer);
                                for (int b = a + 1; b < nrOfTxts; b++) {
                                    TextView txt2 = lTextViews.get(b);
                                    if (txt2 == null) {
                                        continue;
                                    }

                                    String sPlayer2 = txt2.getText().toString().trim();
                                    if (StringUtil.isEmpty(sPlayer2)) {
                                        continue;
                                    }
                                    if (sPlayer.equals(sPlayer2)) {
                                        continue;
                                    }
                                    String sNewMatch = sPlayer + NAMES_SPLITTER + sPlayer2;
                                    lMatches.add(sNewMatch);
                                }
                            }
                            if ( ListUtil.isNotEmpty(lMatches) ) {
                                addGroupWithMatches(m_sToHeader, lMatches);
                            }

                            break;
                        }
                        case RotatingDoublePartners: {
                            List<String> lMatches = new ArrayList<>();

                            List<TextView> lTextViews = m_llTextViews.get(0);
                            List<int[]> lPairs    = new ArrayList<>();
                            lPairs.add(new int[] { 1,2 ,3,4 }); // -5 ==
                            lPairs.add(new int[] { 1,2 ,3,5 }); // -4
                            lPairs.add(new int[] { 1,2 ,4,5 }); // -3

                            lPairs.add(new int[] { 1,3 ,2,4 }); // -5 ==
                            lPairs.add(new int[] { 1,3 ,2,5 }); // -4
                            lPairs.add(new int[] { 1,3 ,4,5 }); // -3

                            lPairs.add(new int[] { 1,4, 2,3 }); // -5 ==
                            lPairs.add(new int[] { 1,4 ,2,5 }); // -3
                            lPairs.add(new int[] { 1,4 ,3,5 }); // -2

                            lPairs.add(new int[] { 1,5 ,2,3 }); // -4
                            lPairs.add(new int[] { 1,5 ,2,4 }); // -3
                            lPairs.add(new int[] { 1,5 ,3,4 }); // -2

                            lPairs.add(new int[] { 2,3 ,4,5 }); // -1
                            lPairs.add(new int[] { 2,4 ,3,5 }); // -1
                            lPairs.add(new int[] { 2,5 ,3,4 }); // -1
                            for(int m=0; m< lPairs.size(); m++) {
                                int[] iDblPlayers = lPairs.get(m);
                                List<String> lPlayers = new ArrayList<>();
                                for(int i=0; i < iDblPlayers.length; i++) {
                                    int iPlayer = iDblPlayers[i] - 1;
                                    if ( iPlayer < lTextViews.size() ) {
                                        lPlayers.add(lTextViews.get(iPlayer).getText().toString().trim());
                                    }
                                }
                                ListUtil.removeEmpty(lPlayers);
                                if ( ListUtil.size(lPlayers) != 4 ) { continue; }
                                String sTeam1 = lPlayers.get(0) + "/" + lPlayers.get(1);
                                String sTeam2 = lPlayers.get(2) + "/" + lPlayers.get(3);
                                lMatches.add(sTeam1 + " - " + sTeam2);
                            }
                            if ( ListUtil.isNotEmpty(lMatches) ) {
                                addGroupWithMatches(m_sToHeader, lMatches);
                            }

                            break;
                        }
                    }
                }

                // ensure the new match is displayed
                //emsAdapter.load(); // already invoked from _replaceMatch()
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }

    private void addGroupWithMatches(final String sHeader, List<String> lMatches) {
        List<String> lFixedMatches = PreferenceValues.getMatchList(m_smSelector.getContext());

        String sHeaderToAdd = sHeader;
        String sCorrespondingConfiguredHeader = m_smSelector.determineConfiguredHeader(sHeaderToAdd, lFixedMatches, false);
        int i = 0;
        while ( sCorrespondingConfiguredHeader != null ) {
            sHeaderToAdd = sHeader + " " + (++i);
            sCorrespondingConfiguredHeader = m_smSelector.determineConfiguredHeader(sHeaderToAdd, lFixedMatches, false);
        }

        List<String> lFixedNew = new ArrayList<String>();
        lFixedNew.addAll(lFixedMatches);

        lFixedNew.add(HEADER_PREFIX +  sHeaderToAdd);
        lFixedNew.addAll(lMatches);

        m_smSelector.storeAndRefresh(lFixedNew);
    }

}
