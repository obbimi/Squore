package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.*;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.RallyEndStatsPrefs;
import com.doubleyellow.util.StringUtil;
import com.doubleyellow.android.view.SelectEnumView;

import java.util.EnumSet;

/**
 * Dialog that allows the user to, just after a score has been entered, to indicate how the score came about.
 */
public class RallyEndStats extends BaseAlertDialog {

    public RallyEndStats(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Player.class.getSimpleName(), m_scoringPlayer);
        return true;
    }

    private Player m_scoringPlayer = null;
    private Call   m_call = null;

    @Override public boolean init(Bundle outState) {
        init((Player) outState.getSerializable(Player.class.getSimpleName()), (Call) outState.getSerializable(Call.class.getSimpleName()));
        return true;
    }

    private ViewGroup ll = null;
    @Override public void show() {

        LayoutInflater myLayout = LayoutInflater.from(context);
        ll = (ViewGroup) myLayout.inflate(R.layout.rally_end_stats, null);
        evPosition   = (SelectEnumView<Position>      ) ll.findViewById(R.id.evPosition);
        evSide       = (SelectEnumView<RacketSide>    ) ll.findViewById(R.id.evSide);
        evDirection  = (SelectEnumView<BallDirection> ) ll.findViewById(R.id.evDirection);
        evTrajectory = (SelectEnumView<BallTrajectory>) ll.findViewById(R.id.evTrajectory);

        final EnumSet<RallyEndStatsPrefs> rallyEndStatsPrefs = PreferenceValues.recordRallyEndStatsDetails(context);
        evPosition  .setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.StrikePosition) ? View.VISIBLE : View.GONE);
        evSide      .setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.RacketSide)     ? View.VISIBLE : View.GONE);
        if ( evDirection != null && evTrajectory != null ) {
            evDirection .setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.BallDirection ) ? View.VISIBLE : View.GONE);
            evTrajectory.setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.BallTrajectory) ? View.VISIBLE : View.GONE);

            // also hide the in-between splitters
            try {
                ll.findViewById(R.id.splitter_evDirection ).setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.BallDirection ) ? View.VISIBLE : View.GONE);
                ll.findViewById(R.id.splitter_evTrajectory).setVisibility(rallyEndStatsPrefs.contains(RallyEndStatsPrefs.BallTrajectory) ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
            }
        }

        dialog = adb
            .setTitle(StringUtil.capitalize(RallyEnd.class.getSimpleName(), false)) // do not set the title, save a bit of room
            .setView(ll)
            .setPositiveButton(RallyEnd.Winner + " " + matchModel.getName(m_scoringPlayer)           , dialogClickListener)
            .setNegativeButton(RallyEnd.Error  + " " + matchModel.getName(m_scoringPlayer.getOther()), dialogClickListener)
            .setNeutralButton (R.string.cmd_cancel, dialogClickListener)
        // install onkeylistener to ensure rallyEndStatsClosed event is triggered if user clicks back button on dialog
            .setOnKeyListener(getOnBackKeyListener(BUTTON_CANCEL))
            .show();
    }

    public void init(Player scoringPlayer, Call call) {
        m_scoringPlayer = scoringPlayer;
        m_call = call; // TODO: in case of a NO LET disable the ERROR button of the non scoring player
    }

    private SelectEnumView<Position>       evPosition   = null;
    private SelectEnumView<RacketSide>     evSide       = null;
    private SelectEnumView<BallDirection>  evDirection  = null;
    private SelectEnumView<BallTrajectory> evTrajectory = null;

    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    public static final int BUTTON_WINNER = DialogInterface.BUTTON_POSITIVE;
    public static final int BUTTON_ERROR  = DialogInterface.BUTTON_NEGATIVE;
    public static final int BUTTON_CANCEL = DialogInterface.BUTTON_NEUTRAL;
    @Override public void handleButtonClick(int which) {
        RallyEnd winnerError = null;
        Player player = null;
        switch (which) {
            case BUTTON_WINNER:
                winnerError = RallyEnd.Winner;
                player = m_scoringPlayer;
                break;
            case BUTTON_CANCEL :
                break;
            case BUTTON_ERROR:
                winnerError = RallyEnd.Error;
                player = m_scoringPlayer.getOther();
                break;
        }
        if ( winnerError != null ) {
            Position       position    = evPosition  .getChecked();
            RacketSide     side        = evSide      .getChecked();
            BallDirection  direction   = evDirection .getChecked();
            BallTrajectory trajectory  = evTrajectory.getChecked();
            matchModel.recordWinnerError(winnerError, player, side, position, direction, trajectory);
            //Toast.makeText(context, ListUtil.join(" ", player, winnerError, side, position, direction, trajectory), Toast.LENGTH_LONG).show();
        }
        //before triggering an event that might open another dialog, dismiss this one
        dialog.dismiss();

        scoreBoard.triggerEvent(ScoreBoard.SBEvent.rallyEndStatsClosed, this);
    }
}
