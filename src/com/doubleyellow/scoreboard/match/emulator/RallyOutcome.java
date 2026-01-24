/*
 * Copyright (C) 2026  Iddo Hoeve
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
package com.doubleyellow.scoreboard.match.emulator;

import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.Player;

public enum RallyOutcome {
    WinPlayerA          (Player.A, null),      
    WinPlayerB          (Player.B, null),
    AppealPlayerA(Player.A, Call.ST),
    AppealPlayerB(Player.A, Call.ST),
  //AppealPlayerA_Stroke(Player.A, Call.ST),
  //AppealPlayerA_YesLet(Player.A, Call.YL),
  //AppealPlayerA_NoLet (Player.A, Call.NL),
  //AppealPlayerB_Stroke(Player.B, Call.ST),
  //AppealPlayerB_YesLet(Player.B, Call.YL),
  //AppealPlayerB_NoLet (Player.B, Call.NL),
    ;

    private Player p;
    private Call c;
    RallyOutcome(Player p, Call c) {
        this.p = p;
        this.c = c;
    }
    public Player getPlayer() {
        return p;
    }
    public Call getCall() {
        return c;
    }
}
