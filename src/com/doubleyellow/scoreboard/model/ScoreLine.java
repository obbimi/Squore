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
import com.doubleyellow.util.ListUtil;
import com.doubleyellow.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreLine
{
    public static final String TAG = "SB." + ScoreLine.class.getSimpleName();

    private Object[]  m_line        = new Object[4];
    private Player    scoringPlayer = null;
    private Player    servingPlayer = null;
    private Integer   score         = null;
    private ServeSide serveSide     = null;

    private Player    callTargetPlayer = null;
            Call      call             = null;
            BrokenEquipment equipment  = null;

    public ScoreLine(Player player, Call call) {
        this.callTargetPlayer = player;
        this.call             = call;
        m_line                = null;
    }
    public ScoreLine(Player player, BrokenEquipment equipment) {
        this.callTargetPlayer = player;
        this.equipment        = equipment;
        m_line                = null;
    }
    public boolean isBrokenEquipment() {
        return ( this.equipment != null );
    }
    public boolean isCall() {
        return ( this.callTargetPlayer != null ) && ( this.call != null );
    }
    public boolean isAppealWithPoint() {
        return ( this.callTargetPlayer != null ) && ( this.call != null ) && (this.call.hasScoreAffect());
    }
    public Call getCall() {
        return this.call;
    }
    public BrokenEquipment getBrokenEquipment() {
        return this.equipment;
    }

    public ScoreLine(ServeSide serveSideA, Integer iScoreA, ServeSide serveSideB, Integer iScoreB) {
        m_line[0] = serveSideA;
        m_line[1] = iScoreA;
        m_line[2] = serveSideB;
        m_line[3] = iScoreB;

        init(serveSideA, iScoreA, serveSideB, iScoreB);
    }

    private void init(ServeSide serveSideA, Integer iScoreA, ServeSide serveSideB, Integer iScoreB) {
        if ( iScoreA != null || iScoreB != null ) {
            scoringPlayer = (iScoreA    != null) ? Player.A   : Player.B;
            score         = (iScoreA    != null) ? iScoreA    : iScoreB;
        }
        if ( serveSideA!= null || serveSideB != null ) {
            servingPlayer = (serveSideA != null) ? Player.A   : Player.B;
            serveSide     = (serveSideA != null) ? serveSideA : serveSideB;
        }
    }

    public ServeSide getServeSide() {
        return serveSide;
    }
    public Player getScoringPlayer() {
        return scoringPlayer;
    }
    /** returns the player that made the appeal (isCall()==true) or the player that reported broken equipment (isBrokenEquipment()==true) or the player that misbehaved (conduct) */
    public Player getCallTargetPlayer() {
        return callTargetPlayer;
    }
    public Player getServingPlayer() {
        return servingPlayer;
    }
    public boolean isHandout(SportType sportType) {
        switch (sportType) {
            case Squash:
                Player scoringPlayer = this.getScoringPlayer();
                if ( scoringPlayer == null ) {
                    // broken equipment or call
                    return false;
                }
                return scoringPlayer.equals(this.getServingPlayer()) == false;
            case Racketlon:
            case Tabletennis:
                // in racketlon and table tennis, serve goes the other team player after 'second=L) serve
                ServeSide ss = this.getServeSide();
                return ServeSide.L.equals(ss);
        }
        return false;
    }
    public Integer getScore() {
        return score;
    }
    public List<String> toStringList() {
        return toStringList(null);
    }
    public List<String> toStringList(Context ctx) {
        List<String> lReturn = new ArrayList<String>();
        if ( this.callTargetPlayer != null ) {
            lReturn.add(" ");
            lReturn.add(" ");
            lReturn.add(" ");
            if ( this.call != null ) {
                String sCall = null;
                if (ctx != null ) {
                    sCall = ctx.getString(this.call.getAbbreviationResourceId());
                } else {
                    sCall = this.call.toString();
                }

                if (this.callTargetPlayer.equals(Player.A)) {
                    lReturn.add(1, sCall);
                } else {
                    lReturn.add(3, sCall);
                }
            } else if ( this.equipment != null ) {
                String sEquipment = null;
                if (ctx != null ) {
                    sEquipment = ctx.getString(this.equipment.getResourceIdAbbreviation());
                } else {
                    sEquipment = equipment.toString();
                }
                if (this.callTargetPlayer.equals(Player.A)) {
                    lReturn.add(1, sEquipment);
                } else {
                    lReturn.add(3, sEquipment);
                }
            }
            return lReturn;
        }
        //boolean bHasNegativeNumber = false;
        for(Object o: m_line) {
            if ( o == null ) {
                lReturn.add("-");
            } else {
                if ( o instanceof ServeSide) {
                    ServeSide ss = (ServeSide) o;
                    String sServeSide = null;
                    if ( ctx != null ) {
                        sServeSide = ctx.getString(ss.getSingleCharResourceId());
                    } else {
                        sServeSide = ss.toString().substring(0, 1);
                    }
                    lReturn.add(sServeSide);
                } else if ( o instanceof Integer ) {
                    Integer I = (Integer) o;
                    lReturn.add(I.toString());
                    //bHasNegativeNumber = bHasNegativeNumber || (I < 0);
                } else {
                    // ??
                    lReturn.add(o.toString());
                }
            }
        }
/*
        if ( bHasNegativeNumber ) {
            lReturn = ListUtil.replace(lReturn, "^-$", ".");
        }
*/
        return lReturn;
    }
    public String toString() {
        return ListUtil.join(this.toStringList(), "");
    }
    public String toString6() {
        StringBuilder sb = new StringBuilder();
        if ( this.callTargetPlayer != null ) {
            if ( call != null ) {
                if (callTargetPlayer.equals(Player.A)) {
                    sb.append("(").append(call).append(")").append("--");
                } else {
                    sb.append("--").append("(").append(call).append(")");
                }
            } else if ( equipment != null ) {
                if (callTargetPlayer.equals(Player.A)) {
                    sb.append("(").append(equipment).append(")").append("--");
                } else {
                    sb.append("--").append("(").append(equipment).append(")");
                }
            }
            return sb.toString();
        }
        for(int i=0; i < m_line.length; i++) {
            Object o = m_line[i];
            if ( i % 2 == 0 ) {
                // null or serve side
                if ( o instanceof ServeSide ) {
                    sb.append(o.toString().substring(0, 1));
                } else {
                    sb.append("-");
                }
            } else {
                // null or a number
                Integer s = (Integer) o;
                if ( s == null) {
                    sb.append("--");
                } else {
                    sb.append(StringUtil.lpad(s, ' ', 2));
                }
            }
        }
        return sb.toString();
    }
    private static final Pattern p = Pattern.compile(""
                                             +"([" + ServeSide.L + ServeSide.R + "-])"
                                             +"(-?[0-9]+|-" + ")"
                                             +"([" + ServeSide.L + ServeSide.R + "-])"
                                             +"(-?[0-9]+|-" + ")"
    );
    private static final Pattern pCall = Pattern.compile(""
                                                 +"(--|" + ListUtil.join(Call.class, "|") + ")"
                                                 +"(--|" + ListUtil.join(Call.class, "|") + ")"
    );
    private static final Pattern pEquipment = Pattern.compile(""
                                                 +"(--|" + ListUtil.join(BrokenEquipment.class, "|") + ")"
                                                 +"(--|" + ListUtil.join(BrokenEquipment.class, "|") + ")"
    );
    public ScoreLine(String sScoreLine) {
        Matcher m = p.matcher(sScoreLine);
        if ( m.find() ) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String g3 = m.group(3);
            String g4 = m.group(4);

            if ( g1.equals("-") == false) {
                m_line[0] = ServeSide.valueOf(g1);
            }
            if ( g2.equals("-") == false ) {
                if (StringUtil.isInteger(g2)) {
                    m_line[1] = Integer.parseInt(g2);
                } else {
                    this.call = Call.valueOf(g2);
                    this.callTargetPlayer = Player.A;
                }
            }
            if ( g3.equals("-") == false) {
                m_line[2] = ServeSide.valueOf(g3);
            }
            if ( g4.equals("-") == false ) {
                if (StringUtil.isInteger(g4)) {
                    m_line[3] = Integer.parseInt(g4);
                } else {
                    this.call = Call.valueOf(g4);
                    this.callTargetPlayer = Player.B;
                }
            }
        } else {
            // YL,NL or ST
            Matcher mCall = pCall.matcher(sScoreLine);
            if ( mCall.find() ) {
                String g1 = mCall.group(1);
                String g2 = mCall.group(2);

                if (g1.equals("--") == false) {
                    this.call = Call.valueOf(g1);
                    this.callTargetPlayer = Player.A;
                }
                if (g2.equals("--") == false) {
                    this.call = Call.valueOf(g2);
                    this.callTargetPlayer = Player.B;
                }
            } else {
                // broken equipment
                // BB,BS or BR
                Matcher mEquipment = pEquipment.matcher(sScoreLine);
                if ( mEquipment.find() ) {
                    String g1 = mEquipment.group(1);
                    String g2 = mEquipment.group(2);

                    if (g1.equals("--") == false) {
                        this.equipment = BrokenEquipment.valueOf(g1);
                        this.callTargetPlayer = Player.A;
                    }
                    if (g2.equals("--") == false) {
                        this.equipment = BrokenEquipment.valueOf(g2);
                        this.callTargetPlayer = Player.B;
                    }
                }
            }
        }
        init((ServeSide) m_line[0], (Integer) m_line[1], (ServeSide) m_line[2], (Integer) m_line[3]);
    }
}
