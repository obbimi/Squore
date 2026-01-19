package com.doubleyellow.scoreboard.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import android.widget.RelativeLayout;
import com.doubleyellow.android.view.ViewUtil;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.util.SBToast;
import com.doubleyellow.util.Direction;

/**
 * Long running thread to explain most functionality of Squore.
 * - text messages
 * - arrows
 */
public abstract class DemoThread extends Thread
{
    protected static final String TAG = "SB." + DemoThread.class.getSimpleName();


    public enum DemoMessage
            /**
             * Settings should be
             * - showTimers = Suggest
             * - showOfficialAnnouncements = Suggest
             * - showToss = Suggest
             * - share = Suggest
             * - tieBreakFormat = 2 clear points
             * - ensure Tips will not be shown
             * - clean all historical matches except the one we want
             *
             * A completed/far progressed match should be available in the 'Stored' games list
             * Select this stored match
             *
             * Ensure last tab for 'new match' defaults to 'Manual'
             *
             * Start with last game in final score so that score sheet is filled.
             * ... (Intro_x)
             **/
    {
        /**  */
        Intro_General {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "Squore is a scoreboard targeted at the squash sport"
                                                                                 , "Hence the name 'Squore'"
                                                                                 , "First a little intro about all the buttons"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        Intro_BigButtons {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "The big buttons show the score of the game in progress"
                                                                                 , "Tap on one of them to record a scored point"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_score1, R.id.btn_score2 }; } // TODO: default arrows showup 'under' FB buttons now...
        },
/*
        Intro_Undo {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "If you accidently assign the point to the wrong player, use the undo option" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.sb_undo_last }; }
        },
*/
        Intro_ServeSideButtons {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "'Inside' the Score buttons there are small 'Serve Side' buttons"
                                                                                 , "They indicate which player should serve and what side he/she serves (L or R)..."
                                                                                 , "... and whether or not the last point was a handout (question mark)"
                                                                                 , "After a scored point is recorded (by clicking on the score buttons), the 'Serve Side' buttons will be updated automatically"
                                                                                 , "So although touching these buttons will change server and/or side, normally there is no need to actually tap these buttons that often"
                                                                                 }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_side1, R.id.btn_side2 }; }
        },
        Intro_PlayerNames {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "The buttons with the player names are firstly there so you know what score belongs to whoom"
                                                                                 , "But by clicking on them they can be used to keep record of appeals ('Let please') and your decision"
                                                                                 , "Hopefully it is not necassary, but they can also be used to keep record of possible 'Conducts' (Warnings and Strokes)"
                                                                                 , "For this you would need to long-click them"
                                                                                 }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.txt_player1, R.id.txt_player2 }; }
        },
        Intro_GameHistory {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "The 'old school' scoring sheet of the game in progress is shown in the middle" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.scorehistorytable}; }
        },
        Intro_PreviousGamesResult {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "The end score of previous games is shown at the top, between the player names"
                                                                                 , "The winner of each game is highlighted"
                                                                                 , "To get more details about the entire scoring history of the match, you can tap on it"
                                                                                 }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.gamescores}; }
        },
        Intro_End_See_action {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "Now lets see it in action"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        Intro_End_Leave_new {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "We are gonna leave this match for now, and start a new one"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        First4PointsForA {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "The first 4 points are scored by " + m.getName(Player.A)}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        FirstPointsForB {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "Then " + m.getName(Player.B) + " scores his first point"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        NewMatch_1 {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "This can be done via the menu in the top-right"
                                                                                 }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_TOP, R.id.txt_player2, RelativeLayout.ALIGN_PARENT_RIGHT, R.id.btn_score2, Direction.N.ordinal() }; }
        },
        NewMatch_2 {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "But it is also possible by clicking on both player name buttons at the same time" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.txt_player1, R.id.txt_player2 }; }
        },
        NewMatch_3 {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "But more easy is to simply click on the 'Plus' button at the bottom right, (not visible when a match is in progress)" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; /*new int[] {RelativeLayout.ALIGN_TOP, R.id.txt_player1, RelativeLayout.ALIGN_PARENT_LEFT, R.id.btn_score1, Direction.N.ordinal() };*/ }
        },
        NewMatch_FB_All {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "When a new match is started, the floating buttons in the middle offer you a few options"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        NewMatch_FB_Toss {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "You can perform a toss"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.hasStarted() == false; }
            //@Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_TOP, R.id.btn_side2, RelativeLayout.ALIGN_LEFT, R.id.txt_player2, Direction.SW.ordinal() }; }
            //@Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_TOP, R.id.sb_toss, RelativeLayout.RIGHT_OF, R.id.sb_toss, Direction.W.ordinal() }; }
            @Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_TOP, R.id.scorehistorytable, RelativeLayout.ALIGN_LEFT, R.id.btn_side2, Direction.W.ordinal() }; }
        },
        NewMatch_FB_Timer {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "Start a timer"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.gameHasStarted() == false; }
            //@Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.CENTER_VERTICAL, 0, RelativeLayout.ALIGN_LEFT, R.id.txt_player2, Direction.W.ordinal() }; }
            @Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_TOP, R.id.scorehistorytable, RelativeLayout.ALIGN_LEFT, R.id.btn_side2, Direction.SW.ordinal() }; }
        },
        NewMatch_FB_Announcement {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "Show a dialog with the official announcement of a match"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.gameHasStarted() == false; }
            @Override public int[] focusOn(Model m)                 { return new int[] {RelativeLayout.ALIGN_BOTTOM, R.id.btn_score2, RelativeLayout.ALIGN_LEFT, R.id.btn_side2, Direction.NW.ordinal() }; }
        },
        NewMatch_EnterNames {
            @Override public String   guidance()                    { return "NOT USED FOR NOW: Type 'Amr Shabana' vs 'Ramy Ashour'.\nDismiss demo message afterwards"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Previously used player names will be used for easy autocompletion while you type" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.match_playerA, R.id.match_playerB }; }
        },
        NewMatch_TimersAndAnnouncements_Off {
            @Override public String   guidance()                    { return "NOT USED FOR NOW: Uncheck timers and official announcements after dismissing both messages"; }
            @Override public String[] getMessages(Model m) { return new String[] { "For more official matches you have the option to start with usage of dialogs presenting you with timers and official announcements."
                                                                          , "For the purpose of this demo I'll turn them off for now"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { /*R.id.useTimers, R.id.showOfficialAnnouncements*/ }; }
        },
        Game_ServeSideToggles {
            @Override public String   guidance()                    { return "Prepare by: A. \n On toast: A, A (3-0), Dismiss "; }
            @Override public String[] getMessages(Model m) { return new String[] { "Notice how the 'Serve Side' button automatically changes from 'L' to 'R' and vice versa if the server also scored" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 0
                                                                          && m.isLastPointHandout() == false
                                                                          && m.getServer().equals(Player.A); }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.btn_side1}; }
        },
        Game_HandoutQuestionMarkB {
            @Override public String   guidance()                    { return "On toast: B (3-1), Dismiss"; }
            @Override public String[] getMessages(Model m) { return new String[] { "On the 'Serve Side' button a question mark appears if the last point was scored by the receiver"
                                                                                 , "This indicates he must serve now and is free to choose Left (L) or Right (R) for his first serve" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 2
                                                                          && m.isLastPointHandout() == false
                                                                          && m.getServer().equals(Player.A); }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_side1 }; }
        },
        Game_HandoutQuestionMarkA {
            @Override public String   guidance()                    { return "On toast: B (3-1), Dismiss"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Here is that question mark again after a handout" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 2
                                                                          && m.isLastPointHandout() == false
                                                                          && m.getServer().equals(Player.B); }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_side2 }; }
        },
        Game_RecordAppealAndDecision {
            @Override public String   guidance()                    { return "Prepare: A,B\nOn toast: Dismiss, Appeal B, Dismiss by swipe, decide Stroke (4-3)"; }
            @Override public String[] getMessages(Model m) { return new String[] { "If you wish to do so, you can record an appeal (and your decision)"
                                                                                 , "Simply click on the name of the player calling for a 'Let'" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 3 && m.getDiffScore() < 3; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.txt_player2}; }
        },
        Game_RecordAppealAndDecision_2 {
            @Override public String   guidance()                    { return ""; }
            @Override public String[] getMessages(Model m) { return new String[] { "If your decision means an additional point for either player, this is automatically added"
                                                                                 , "No need to hit the big score buttons any more" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 3 && m.getDiffScore() < 3; }
            @Override public int[] focusOn(Model m)                 { return new int[] {}; }
        },
        Game_RecordAppealAndDecision_3 {
            @Override public String   guidance()                    { return ""; }
            @Override public String[] getMessages(Model m) { return new String[] { "As you can see, also the appeals and your decision show up in the original paper scoring"
                                                                                 , "" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 3 && m.getDiffScore() < 3; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.scorehistorytable}; }
        },
        Game_RecordConductAndDecision {
            @Override public String   guidance()                    { return "Prepare: A (5-3)\n On Toast: Conduct B, Dismiss by swipe, Scroll throug types, click Conduct Warning (5-3)"; }
            @Override public String[] getMessages(Model m) { return new String[] { "You can record a conduct when a player misbehaves by long-clicking on that player." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 4; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.txt_player2}; }
        },
        Game_UndoScoring {
            @Override public String   guidance()                    { return "Prepare: B, A, A (7-4)\nOn Toast: Undo and appoint to B (6-5), Dismiss"; }
            @Override public String[] getMessages(Model m) { return new String[] { "If you accidentally assign a point to the wrong player, you can easily undo it by pressing 'Undo' in the actionbar" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() > 6; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.sb_undo_last } ; }
        },
        Game_HistoryOfGameInProgress {
            @Override public String   guidance()                    { return "Prepare: B, A, A (8-6)\n On Toast: Dismiss and Scoll up"; }
            @Override public String[] getMessages(Model m) { return new String[] { "An old fashioned scoring sheet is available during the game. You can scroll back if desired." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() >= 8; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.scorehistorytable}; }
        },
        Game_GameBallButtonColor {
            @Override public String   guidance()                    { return "Prepare: A (9-6).\nOn Toast: A (10-6) Dismiss"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Notice that the score button of the player that reaches gameball changes color" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() == 9; }
            @Override public int[] focusOn(Model m)                 { int i = m.getServer().equals(Player.A) ? R.id.btn_score1 : R.id.btn_score2; return new int[] {i}; }
        },
        Game_EndGameDialog {
            @Override public String   guidance()                    { return "On Toast: B, Dismiss, A (11-5) Dismiss by swipe, [OK] in dialog"; }
            @Override public String[] getMessages(Model m) { return new String[] { "When the game ends, by default a dialog pops up allowing you to end this game. (Check the 'Settings/Behaviour' to change this if you don't like it.)" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() == 10; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        SwitchMatch_Intro {
            @Override public String   guidance()                    { return "Prepare: End the game if not yet done\n On Toast: dismiss both"; }
            @Override public String[] getMessages(Model m) { return new String[] { "For the purpose of this demo we will now continue the game we had open at the very start of the demo"
                                                                          , "We can find it under 'Stored Matches'"
                                                                          }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() == 0; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },
        SwitchMatch_HistoricalGames {
            @Override public String   guidance()                    { return "Go to 'Stored matches' only than dismiss first message"; }
            @Override public String[] getMessages(Model m) { return new String[] { "By default all games reffed with this app are stored here."
                                                                          , "This allows you to (re-)consult the details later if desired."
                                                                          , "We now re-open the initial game."
                                                                          }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.newmatch_tab_strip}; }
        },
        Game_ClickOnGamesScoresToShowDetails {
            @Override public String   guidance()                    { return "Dismiss message than bring up history"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Click on the game scores in the middle of the screen to have details about the score history appear" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.gamescores}; }
        },
        Game_ScoreDetailsButton {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "In the actionbar, press on the icon with the 'Graph' to get details about the last game(s)" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMinScore() == 0; }
            @Override public int[] focusOn(Model m)                 { return null; }
        },

        History_SwipeToNavigate {
            @Override public String   guidance()                    { return "Dismiss. Swipe to first game. Dismiss, Swipe Dismiss, Swipe Dismiss"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Initially a graph for the last set is shown."
                                                                          , "Swipe to show graph of previous games"
                                                                          , "Swipe even further to get a score sheet of all played games"
                                                                          , "Swipe untill the end to see an overview of calls you made"
                                                                          }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.matchhistory_tab_strip}; }
        },

        NiceToKnow_ShareResult {
            @Override public String   guidance()                    { return "Close score history activity. Ensure last set is actually ended."; }
            @Override public String[] getMessages(Model m) { return new String[] { "It is possible to share the result of a match."}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.sb_sub_share_match}; }
        },
        NiceToKnow_Fixedmatches {
            @Override public String   guidance()                    { return "Close score history activity. " + "Prepare: open 'My matches' dialog/tab." + "On toast: select one of the matches"; }
            @Override public String[] getMessages(Model m) { return new String[] { "If you know up front what matches you will be reffing you can define them here for easy selection."
                                                                          , "I will know create a new group with 2 new matches"
                                                                          , "Know I select one of them to start that match"
                                                                          }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.expandable_match_selector_group_with_menu}; }
        },
        NewMatch_TimersAndAnnouncements_On {
            @Override public String   guidance()                    { return null; }
            @Override public String[] getMessages(Model m) { return new String[] { "This time I leave 'timers' and 'official announcements' on"}; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { /*R.id.useTimers, R.id.showOfficialAnnouncements*/ }; }
        },
        NiceToKnow_Toss {
            @Override public String   guidance()                    { return "Prepare: Actually select a match from my matches. Select Show/Toss menu option"; }
            @Override public String[] getMessages(Model m) { return new String[] { "Usually the players use a racket or a coin to toss for who will start serving."
                                                                          , "But if you like it, you can let the app do the tossing." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return m.getMaxScore() == 0; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.float_toss}; }
        },
        NiceToKnow_ClickBothScoreButtons {
            @Override public String   guidance()                    { return "Preapre: Actually select a match from my matches if not already done so."; }
            @Override public String[] getMessages(Model m) { return new String[] { "Click on both 'Score' buttons at the same times will trigger the 'Adjust score' dialog."
                                                                          , "For example if you only start reffing a match after the first few game have already been played." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] { R.id.btn_score1, R.id.btn_score2 }; }
        },
        NiceToKnow_OfficialSquashRules {
            @Override public String   guidance()                    { return "Dismiss message 1. Choose Help/Official squash rules. And click e.g. on 8. Interferance."; }
            @Override public String[] getMessages(Model m) { return new String[] { "If your unsure about a certain squash rule..."
                                                                          , "Know you have the option to view the official rules" }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.sb_official_rules}; }
        },
        NiceToKnow_ConductInfo {
            @Override public String   guidance()                    { return "Choose Help/Show possible conducts."; }
            @Override public String[] getMessages(Model m) { return new String[] { "If your not sure about conducts you can impose in what situation, check out the appropriate Help entry for a little guidance." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.sb_possible_conductsA}; }
        },
        NiceToKnow_Settings {
            @Override public String   guidance()                    { return "Choose Settings."; }
            @Override public String[] getMessages(Model m) { return new String[] { "There are several settings you can tweak that will influence the appearance and behaviour of the app."
                                                                          , "If you do not like the default PSA colors, you can switch color scheme to e.g. Squash Skills."
                                                                          , "You can even define your own, e.g. so the app colors match with the colors of your club. Check out the help page for more info." }; }
            @Override public boolean appliesTo(Model m, Activity a) { return true; }
            @Override public int[] focusOn(Model m)                 { return new int[] {R.id.sb_settings}; }
        },
        ;
        /** A string the is printed to the log file just to have a reminder of what to do to get to the message and what to do when the message is being displayed */
        public abstract String   guidance();
        /** The message that will be displayed in sequence */
        public abstract String[] getMessages(Model m);
        public abstract boolean  appliesTo(Model model, Activity activity);
        public abstract int[]    focusOn(Model m);
    }

    protected ScoreBoard scoreBoard = null;
    protected Model      matchModel = null;
    protected Activity   activity   = null;
    protected Menu       menu       = null;
    private int bgColor  = Color.WHITE;
    private int txtColor = Color.BLACK;

    DemoThread(ScoreBoard scoreBoard, Model model) {
        this.scoreBoard = scoreBoard;
        this.matchModel = model;
        setActivity(scoreBoard);
        bgColor = PreferenceValues.getDemoBackgroundColor(scoreBoard);
        txtColor = scoreBoard.getResources().getColor(android.R.color.white);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    /** Invoked from MatchTabbed and ScoreBoard */
    public void setMenu(Menu menu) {
        this.menu = menu;
    }
    public void setModel(Model model) {
        this.matchModel = model;
    }

    boolean bStopLoop = false;
    public void stopLoop() {
        bStopLoop = true;
    }
// ---------------------------------------
    // HELPER METHODS
    // ---------------------------------------

    //InstrumentationTestCase instrumentationTestCase = new InstrumentationTestCase();
    //TouchUtils touchUtils = new TouchUtils();
    protected void simulateClickOnView(int iResId, DemoMessage demoMessage) {

        final int iDiff = iResId < 0?2000:100;
        int iResIdAbs = Math.abs(iResId);
        final View view = activity.findViewById(iResIdAbs);
        if (view == null) {
            Log.w(TAG, "Could not locate view with id " + iResId + " " + demoMessage);
            return;
        }

/*
        if ( true ) {
            touchUtils.clickView(instrumentationTestCase, view);
            return;
        }
*/

        Handler handler = new Handler(activity.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                if (iDiff > 100) {
                    view.performLongClick();
                } else {
                    view.performClick();
                }
            }
        });

/*
        float x = 10.0f;
        float y = 10.0f;
        int metaState = 0;
        long downTime = SystemClock.uptimeMillis();
        final MotionEvent downEvent = MotionEvent.obtain(downTime, downTime + iDiff, MotionEvent.ACTION_DOWN, x, y, metaState);
        downTime += iDiff;
        final MotionEvent upEvent   = MotionEvent.obtain(downTime, downTime + 100  , MotionEvent.ACTION_UP  , x, y, metaState);
        Log.w(TAG, "Simulate click on " + view.toString());

        Handler handler = new Handler(scoreBoard.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                view.dispatchTouchEvent(downEvent);
                view.dispatchTouchEvent(upEvent);
            }
        });
*/
    }

    protected void pause(long lMs) {
        try {
            synchronized (this) {
                //Log.d(TAG, "Waiting...");
                wait(lMs);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "?? " + e); // normally only when thread is deliberately stopped/interrupted
        }
        //Log.d(TAG, "Resumed");
    }

    void showInfo(String sMessage, int[] iRelatedResId, int iSeconds) {
        showInfo(new String[]{sMessage}, 0, iRelatedResId, iSeconds);
    }
    void showInfo(final String[] sMessages, final int iMsgCnt, final int[] iRelatedResId, final int iSeconds) {

        int iNrOfRelatedViews = iRelatedResId != null ? iRelatedResId.length : 0;
/*
        final View[] vRelatedGuiElements = new View[iNrOfRelatedViews];
        if ( vRelatedGuiElements.length > 0 ) {
            for (int i = 0; i < iRelatedResId.length; i++) {
                vRelatedGuiElements[i] = activity.findViewById(Math.abs(iRelatedResId[i]));
            }
        }
*/

        int gravity = Gravity.NO_GRAVITY; // 80
        if ( ViewUtil.isLandscapeOrientation(activity) ) {
            gravity = Gravity.BOTTOM;
            if ( iNrOfRelatedViews == 1 ) {
                gravity = Gravity.BOTTOM + Gravity.END;
            }
        } else {
            gravity = Gravity.CENTER;
            if ( iNrOfRelatedViews == 1 ) {
                gravity = Gravity.CENTER_HORIZONTAL + Gravity.BOTTOM;
            }
        }

/*
        // use related resource id to determine where the toast message should appear
        int gravity = Gravity.BOTTOM; // 80
        switch (iMsgCnt % 3) {
            case 0:
                gravity = Gravity.BOTTOM;
                break;
            case 1:
                gravity = Gravity.CENTER;
                break;
            case 2:
                gravity = Gravity.TOP;
                break;
        }
        boolean bImageFirst = true;
        if ( iMsgCnt == 0 ) {
            if ( (vRelatedGuiElements!=null) && (vRelatedGuiElements.length > 0) && (vRelatedGuiElements[0] != null)) {
                View vParent = vRelatedGuiElements[0].getRootView();
                int iHalfScreenWidth  = vParent.getWidth()  / 2; // e.g. 240
                int iHalfScreenHeight = vParent.getHeight() / 2; // e.g. 400
                float x = vRelatedGuiElements[0].getX();
                float y = vRelatedGuiElements[0].getY(); // x,y = (6,78) for score button 1 in portrait | (3,3) for player1 in portrait | (6,400) for score2 in portrait
                float w = vRelatedGuiElements[0].getWidth();
                float h = vRelatedGuiElements[0].getHeight();
                if (x + w > iHalfScreenWidth) {
                    bImageFirst = false;
                }
                if (y + h > iHalfScreenHeight) {
                    gravity = Gravity.TOP; // 48
                }
            }
        }
*/

        final int iGravityF    = gravity;

        Handler handler = new Handler(activity.getMainLooper());
        handler.post( new Runnable(){
            public void run(){
                String sMessage = sMessages[iMsgCnt];
                int iTextSize = ViewUtil.getScreenHeightWidthMinimum() / 20;
                demoToast = new SBToast(activity, sMessage, iGravityF, bgColor, txtColor, activity.findViewById(android.R.id.content), iRelatedResId, iTextSize);
                //Log.d(TAG, "Showing '" + sMessage + "' for " + iSecs + " secs [" + sMessage.length() + "]");
                boolean bDrawArrows = (iMsgCnt == 0); // draw arrows for the first of possible multiple text messages
                demoToast.show(iSeconds, bDrawArrows, iMsgCnt==sMessages.length-1);
            }
        });
    }

    private SBToast demoToast = null;
    boolean isDemoMessageShowing() {
        return demoToast != null && demoToast.isShowing();
    }
    public void cancelDemoMessage() {
        if ( demoToast != null ) {
            Handler handler = new Handler(activity.getMainLooper());
            handler.post( new Runnable(){
                public void run(){
                    demoToast.cancel();
                }
            });
        }
    }
}
