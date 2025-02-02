package com.doubleyellow.scoreboard.bluetooth_le;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.bluetooth.BLEBridge;
import com.doubleyellow.scoreboard.bluetooth_le.selectdevice.BLEActivity;
import com.doubleyellow.scoreboard.bluetooth_le.selectdevice.VerifyConnectedDevices;
import com.doubleyellow.scoreboard.main.PlayerFocusEffectCountDownTimer;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.LandscapeLayoutPreference;
import com.doubleyellow.scoreboard.prefs.PreferenceKeys;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;
import com.doubleyellow.scoreboard.prefs.ShowScoreChangeOn;
import com.doubleyellow.scoreboard.prefs.StartupAction;
import com.doubleyellow.scoreboard.vico.FocusEffect;
import com.doubleyellow.scoreboard.vico.IBoard;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.StringUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to serve as glue for BLE functionality.
 */
public class BLEConfigHandler implements BLEBridge {
    private static final String TAG = "SB." + BLEConfigHandler.class.getSimpleName();

    static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private        JSONObject         m_bleConfig                             = null;
    private        boolean            m_bSingleDevice_ConfirmWithSameButton   = false;
    private        BLEDeviceButton    m_eInitiateSelfScoreChangeButton        = BLEDeviceButton.PRIMARY_BUTTON;
    private        BLEDeviceButton    m_eInitiateOpponentScoredChangeButton   = null;
    private        BLEDeviceButton    m_eConfirmScoreBySelfButton             = null;
    private        BLEDeviceButton    m_eConfirmScoreByOpponentButton         = m_eInitiateSelfScoreChangeButton.getOther();
    private        BLEDeviceButton    m_eCancelScoreByOpponentButton          = null;
    private        BLEDeviceButton    m_eCancelScoreByInitiatorButton         = m_eInitiateSelfScoreChangeButton.getOther();
    private static int                m_nrOfBLEDevicesConnected               = 0;
    private static Player             m_blePlayerWaitingForScoreToBeConfirmed = null;
    private static Player             m_blePlayerToConfirmOwnScore            = null;
    /** attempt to 'catch' accidental double press on connected BLE device */
    private static final Map<Player, Long> m_lastBLEScoreChangeReceivedFrom   = new HashMap<>();
    private        long               m_lIgnoreAccidentalDoublePress_ThresholdMS = 1500;

    private static boolean            m_bBLEDevicesSelected                   = false;

    private ScoreBoard m_context = null;
    private IBoard     m_iBoard  = null;
    public BLEConfigHandler() {}

    private        BLEReceiverManager m_bleReceiverManager                    = null;
    @Override public boolean init(ScoreBoard context, IBoard iBoard) {
        final String sMethod = "init";

        m_context = context;
        m_iBoard  = iBoard;

        m_lIgnoreAccidentalDoublePress_ThresholdMS = PreferenceValues.IgnoreAccidentalDoublePress_ThresholdInMilliSeconds(m_context);

        if ( m_bBLEDevicesSelected == false ) {
            Log.i(sMethod, "Don't use BLE. First select devices");
            return false;
        }

        String sBluetoothLEDevice1 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral1, null, m_context);
        String sBluetoothLEDevice2 = PreferenceValues.getString(PreferenceKeys.BluetoothLE_Peripheral2, null, m_context);
        if ( StringUtil.hasEmpty(sBluetoothLEDevice1, sBluetoothLEDevice2) ) {
            Log.w(sMethod, "Don't use BLE. No 2 devices specified");
            return false;
        }
        Log.i(sMethod, String.format("Scanning for devices %s, %s", sBluetoothLEDevice1, sBluetoothLEDevice2));

        m_bleConfig = BLEUtil.getActiveConfig(m_context);
        if ( m_bleConfig == null ) {
            Toast.makeText(m_context, "Could not obtain config for BLE", Toast.LENGTH_LONG).show();
            return false;
        }
        if ( (sBluetoothLEDevice1 != null) && (sBluetoothLEDevice1.equalsIgnoreCase(sBluetoothLEDevice2) == false) ) {
            // settings only relevant when using 2 wristbands with confirmation mechanism
            m_eInitiateSelfScoreChangeButton      = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.InitiateScoreChangeButton        , m_eInitiateSelfScoreChangeButton);
            m_eInitiateOpponentScoredChangeButton = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.InitiateOpponentScoreChangeButton, m_eInitiateOpponentScoredChangeButton);
            m_eConfirmScoreByOpponentButton       = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.ConfirmScoreByOpponentButton     , m_eConfirmScoreByOpponentButton);
            m_eCancelScoreByOpponentButton        = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.CancelScoreByOpponentButton      , m_eCancelScoreByOpponentButton);
            m_eConfirmScoreBySelfButton           = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.ConfirmScoreBySelfButton         , m_eConfirmScoreBySelfButton);
            m_eCancelScoreByInitiatorButton       = BLEUtil.getButtonFor(m_bleConfig, BLEUtil.Keys.CancelScoreByInitiatorButton     , m_eCancelScoreByInitiatorButton);
        }
        if ( (sBluetoothLEDevice1 != null) && (sBluetoothLEDevice1.equalsIgnoreCase(sBluetoothLEDevice2)) ) {
            // settings only relevant when using 1 wristbands with 2 buttons and confirmation mechanism
            m_bSingleDevice_ConfirmWithSameButton = m_bleConfig.optBoolean(BLEUtil.Keys.SingleDevice_ConfirmWithSameButton.toString(), m_bSingleDevice_ConfirmWithSameButton);
        }

        m_bleReceiverManager = new BLEReceiverManager(m_context, mBluetoothAdapter, sBluetoothLEDevice1, sBluetoothLEDevice2, m_bleConfig);

        BLEHandler bleHandler = new BLEHandler(m_context, this);
        m_bleReceiverManager.setHandler(bleHandler);
        m_bleReceiverManager.startReceiving();

        return true;
    }

    @Override public void stop() {
        if ( m_bleReceiverManager != null ) {
            m_bleReceiverManager.closeConnection();
            m_bleReceiverManager = null;
        }
    }
    public boolean notifyBLE(Player p, BLEUtil.Keys configKey) {
        if ( m_bleReceiverManager == null ) { return false; }
        return m_bleReceiverManager.writeToBLE(p, configKey, null);
    }

    public String getBLEMessage(int iResId, Object... formatArgs) {
        Object[] newFormatArgs = new Object[formatArgs.length];
        for(int i=0; i<formatArgs.length;i++) {
            Object arg = formatArgs[i];
            if ( arg instanceof Player) {
                newFormatArgs[i] = ScoreBoard.getMatchModel().getName((Player) arg);
            } else if ( arg instanceof BLEDeviceButton ) {
                newFormatArgs[i] = getBLEButtonDescription((BLEDeviceButton) arg);
            } else {
                newFormatArgs[i] = arg;
            }
        }
        return m_context.getString(iResId, newFormatArgs);
    }
    public String getBLEButtonDescription(BLEDeviceButton b) {
        if ( b == null ) { return "" ; }
        String s = m_bleConfig.optString(b.toString(), b.toString());
        if ( s.startsWith("R.string.") ) {
            // TODO: if in form of R.string.xxx in bluetooth_le_config.json
            String sName = s.replace("R.string.", "");
            int stringId = m_context.getResources().getIdentifier(sName, "string", m_context.getPackageName());
            if ( stringId != 0 ) {
                s = m_context.getString(stringId);
            } else {
                s = StringUtil.capitalize(sName);
            }
        }

        return s;
    }

    @Override public void undoLastBLE(String[] saMethodNArgs) {
        Player pInitiatedBy = Player.A; // undetermined: typically only used with 'one device'
        if ( m_lIgnoreAccidentalDoublePress_ThresholdMS > 0L ) {
            long lNow = System.currentTimeMillis();
            if ( m_lastBLEScoreChangeReceivedFrom.containsKey(pInitiatedBy) ) {
                long lLastPress = m_lastBLEScoreChangeReceivedFrom.get(pInitiatedBy);
                if ( lNow -  lLastPress < m_lIgnoreAccidentalDoublePress_ThresholdMS ) {
                    String sInfoMsg = getBLEMessage(R.string.ble_undo_last_ignored__assume_accidental_double_click, m_lIgnoreAccidentalDoublePress_ThresholdMS);
                    m_iBoard.showBLEInfoMessage(sInfoMsg, 10);
                    return;
                }
            }
            m_lastBLEScoreChangeReceivedFrom.put(pInitiatedBy, lNow);
            m_lastBLEScoreChangeReceivedFrom.remove(pInitiatedBy.getOther()); // e.g. allow more quickly changing the score by pressing buttons alternating
        }
        ScoreBoard.getMatchModel().undoLast();
    }
    @Override public void changeScoreBLE(String[] saMethodNArgs) {
        if ( (saMethodNArgs.length > 1) && (ScoreBoard.getMatchModel() != null) ) {
            // derive score to change from first parameter
            String sAorB = saMethodNArgs[1].toUpperCase().trim();
            Player pScored;
            if ( sAorB.matches("[0-1]") ) {
                int i0isA1IsB = Integer.parseInt(sAorB);
                pScored = Player.values()[i0isA1IsB];
            } else {
                pScored = Player.valueOf(sAorB);
            }

            // derive initiated-by-player and by-button-pressed from optional 2nd and 3th parameter(s)
            Player pInitiatedBy = pScored;
            BLEDeviceButton buttonPressed = null;
            int[] iaPosition = {2,3};
            for(int iParamPos: iaPosition) {
                if ( saMethodNArgs.length > iParamPos ) {
                    sAorB = saMethodNArgs[iParamPos].toUpperCase().trim();
                    if ( sAorB.length() == 1 ) {
                        if ( sAorB.matches("[0-1]") ) {
                            int i0isA1IsB = Integer.parseInt(sAorB);
                            pInitiatedBy = Player.values()[i0isA1IsB];
                        } else {
                            pInitiatedBy = Player.valueOf(sAorB);
                        }
                    } else {
                        String sPrimaryOrSecondary = sAorB;
                        buttonPressed = BLEDeviceButton.valueOf(sPrimaryOrSecondary);
                    }
                }
            }

            if ( m_lIgnoreAccidentalDoublePress_ThresholdMS > 0L ) {
                long lNow = System.currentTimeMillis();
                if ( m_lastBLEScoreChangeReceivedFrom.containsKey(pInitiatedBy) ) {
                    long lLastPress = m_lastBLEScoreChangeReceivedFrom.get(pInitiatedBy);
                    if ( lNow -  lLastPress < m_lIgnoreAccidentalDoublePress_ThresholdMS ) {
                        String sInfoMsg = getBLEMessage(R.string.ble_score_for_X_changed_by_Y_ble_button_of_Z_ignored__assume_accidental_double_click, pScored, buttonPressed, pInitiatedBy, m_lIgnoreAccidentalDoublePress_ThresholdMS);
                        if ( m_nrOfBLEDevicesConnected == 1 ) {
                            // TODO
                        }
                        m_iBoard.showBLEInfoMessage(sInfoMsg, 10);
                        return;
                    }
                }
                m_lastBLEScoreChangeReceivedFrom.put(pInitiatedBy, lNow);
                m_lastBLEScoreChangeReceivedFrom.remove(pInitiatedBy.getOther()); // e.g. allow more quickly changing the score by pressing buttons alternating
            }

            int iTmpTxtOnElementDuringFeedback = m_context.getTxtOnElementDuringFeedback(pScored);
            String sInfoMsg = getBLEMessage(R.string.ble_score_for_X_changed_by_Y_ble_button_of_Z, pScored, buttonPressed, pInitiatedBy);
            if ( m_nrOfBLEDevicesConnected == 1 ) {
                sInfoMsg = getBLEMessage(R.string.ble_score_for_X_changed_by_Y_ble_button, pScored, buttonPressed);
            }
            m_iBoard.showBLEInfoMessage(sInfoMsg, 10);
            m_context.startVisualFeedbackForScoreChange(pScored, iTmpTxtOnElementDuringFeedback);
        }

    }
    @Override public void undoScoreForInitiatorBLE(String[] saMethodNArgs) {
        if ( (saMethodNArgs.length > 1) && (ScoreBoard.getMatchModel() != null) ) {
            String sAorB = saMethodNArgs[1].toUpperCase().trim();
            Player pUndoTriggeredBy;
            if ( sAorB.matches("[0-1]") ) {
                int i0isA1IsB = Integer.parseInt(sAorB);
                pUndoTriggeredBy = Player.values()[i0isA1IsB];
            } else {
                pUndoTriggeredBy = Player.valueOf(sAorB);
            }
            Player lastScorer = ScoreBoard.getMatchModel().getLastScorer();

            String sInfoMsg = getBLEMessage(R.string.ble_last_score_for_X_undone_by_ble, pUndoTriggeredBy);
            if ( pUndoTriggeredBy.equals(lastScorer) ) {
                //matchModel.undoLast(); // triggered by timer after blinking 'undo'
                m_context.startVisualFeedbackForScoreChange(lastScorer, R.string.uc_undo);
                m_iBoard.showBLEInfoMessage(sInfoMsg, 10);
            } else {
                if ( lastScorer != null ) {
                    sInfoMsg = getBLEMessage(R.string.ble_last_score_for_X_can_not_be_undone_by_ble_of_Y, lastScorer, pUndoTriggeredBy);
                    m_iBoard.showBLEInfoMessage(sInfoMsg, 10);
                }
            }
        }

    }
    @Override public void changeScoreBLEConfirm(String[]  saMethodNArgs) {
        Player          playerWristBand      = Player         .valueOf(saMethodNArgs[1].toUpperCase().trim());
        BLEDeviceButton eButtonPressed       = BLEDeviceButton.valueOf(saMethodNArgs[2].toUpperCase().trim());
        int             iNrOfDevicesRequired = m_bleConfig.optInt   (BLEUtil.Keys.NrOfDevices.toString(), 2);
        Log.i(TAG, String.format("[interpretReceivedMessage] changeScoreBLEConfirm: %s, player:%s, button:%s", m_blePlayerWaitingForScoreToBeConfirmed, playerWristBand, eButtonPressed));
        int iTmpTxtOnElementDuringFeedback = m_context.getTxtOnElementDuringFeedback(m_blePlayerWaitingForScoreToBeConfirmed);
        if ( iNrOfDevicesRequired == 1 ) {
            if ( m_blePlayerWaitingForScoreToBeConfirmed != null ) {
                String sDoChangeScore = null;
                String sDoCancelScore = null;
                if ( playerWristBand.equals(m_blePlayerWaitingForScoreToBeConfirmed) ) {
                    // button for same player pressed again
                    if ( m_bSingleDevice_ConfirmWithSameButton ) {
                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_pressing_y, eButtonPressed);
                    } else {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_pressing_y, eButtonPressed);
                    }
                } else {
                    // button for other player pressed
                    if ( m_bSingleDevice_ConfirmWithSameButton ) {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_pressing_y, eButtonPressed);
                    } else {
                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_pressing_y, eButtonPressed);
                    }
                }
                if ( sDoChangeScore != null ) {
                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                    m_context.startVisualFeedbackForScoreChange(m_blePlayerWaitingForScoreToBeConfirmed, iTmpTxtOnElementDuringFeedback);
                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                } else if ( sDoCancelScore != null ) {
                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                } else {
                    // should not happen... with single device one button should confirm, the other cancel (third button?)
                    Log.w(TAG, "[Single device] Score still waiting confirmation? " + m_blePlayerWaitingForScoreToBeConfirmed + " " + m_bSingleDevice_ConfirmWithSameButton);
                    m_iBoard.appendToInfoMessage(".");
                }
            } else {
                BLEDeviceButton eButtonOther     = BLEDeviceButton.values()[1 - eButtonPressed.ordinal()];
                BLEDeviceButton eButtonToConfirm = m_bSingleDevice_ConfirmWithSameButton ? eButtonPressed : eButtonOther;
                Log.w(TAG, String.format("Score for %s entered with button %s now waiting for confirmation by pressing %s", playerWristBand, eButtonPressed, eButtonToConfirm));
                m_blePlayerWaitingForScoreToBeConfirmed = playerWristBand;

                String sToConfirmMsg = getBLEMessage(R.string.ble_pressed_X__confirm_score_for_Y_by_pressing_Z, eButtonPressed, m_blePlayerWaitingForScoreToBeConfirmed, eButtonToConfirm);
                m_iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                startWaitingForBLEConfirmation(m_blePlayerWaitingForScoreToBeConfirmed, null);
            }

        } else if ( iNrOfDevicesRequired == 2 ) {
            if ( m_blePlayerWaitingForScoreToBeConfirmed != null ) {
                String sDoChangeScore = null;
                String sDoCancelScore = null;
                if (playerWristBand.getOther().equals(m_blePlayerWaitingForScoreToBeConfirmed)) {
                    // check the confirmation by other player/team
                    Log.i(TAG, "Check BLE confirmation by " + playerWristBand + " with button " + m_eConfirmScoreByOpponentButton);
                    if (eButtonPressed.equals(m_eConfirmScoreByOpponentButton)) {
                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_opponent_x_by_pressing_y, playerWristBand, eButtonPressed);
                    } else if ( eButtonPressed.equals(m_eCancelScoreByOpponentButton) ) {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_opponent_x_by_pressing_y, playerWristBand, eButtonPressed);
                    }
                } else {
                    // same player/team pressed a button...
                    Log.w(TAG, "Player " + m_blePlayerWaitingForScoreToBeConfirmed + " waiting for confirmation by opponent pressing " + m_eConfirmScoreByOpponentButton);
                    if (eButtonPressed.equals(m_eCancelScoreByInitiatorButton)) {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_initiator_x_by_pressing_y, playerWristBand, eButtonPressed);
                    }
                }
                if (sDoChangeScore != null) {
                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                    m_context.startVisualFeedbackForScoreChange(m_blePlayerWaitingForScoreToBeConfirmed, iTmpTxtOnElementDuringFeedback);
                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                } else if (sDoCancelScore != null) {
                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                    m_blePlayerWaitingForScoreToBeConfirmed = null;
                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                } else {
                    Log.w(TAG, "Score still waiting confirmation? " + m_blePlayerWaitingForScoreToBeConfirmed);
                    m_iBoard.appendToInfoMessage(".");
                }
            } else if ( m_blePlayerToConfirmOwnScore != null ) {
                String sDoChangeScore = null;
                String sDoCancelScore = null;
                if ( playerWristBand.equals(m_blePlayerToConfirmOwnScore) ) {
                    // check the confirmation by scoring player/team
                    Log.i(TAG, "Check BLE confirmation by " + playerWristBand + " with button " + m_eConfirmScoreBySelfButton);
                    if (eButtonPressed.equals(m_eConfirmScoreBySelfButton)) {
                        sDoChangeScore = getBLEMessage(R.string.ble_score_confirmed_by_scoring_team_x_by_pressing_y, playerWristBand, eButtonPressed);
                    } else {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_scoring_team_x_by_pressing_y, playerWristBand, eButtonPressed);
                    }
                } else {
                    // same player/team pressed a button...
                    if ( eButtonPressed.equals(m_eInitiateOpponentScoredChangeButton.getOther()) ) {
                        sDoCancelScore = getBLEMessage(R.string.ble_score_cancelled_by_initiator_x_by_pressing_y, playerWristBand, eButtonPressed);
                    } else {
                        Log.w(TAG, "Player " + m_blePlayerToConfirmOwnScore + " to confirm own score by pressing " + m_eConfirmScoreBySelfButton);
                        m_iBoard.appendToInfoMessage(".");
                    }
                }
                if (sDoChangeScore != null) {
                    Log.i(TAG, "sDoChangeScore : " + sDoChangeScore);

                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoChangeScore, 10);
                    m_context.startVisualFeedbackForScoreChange(m_blePlayerToConfirmOwnScore, iTmpTxtOnElementDuringFeedback);
                    m_blePlayerToConfirmOwnScore = null;
                } else if (sDoCancelScore != null) {
                    Log.i(TAG, "sDoCancelScore : " + sDoCancelScore);
                    m_blePlayerToConfirmOwnScore = null;
                    stopWaitingForBLEConfirmation();
                    m_iBoard.showBLEInfoMessage(sDoCancelScore, 10);
                } else {
                    Log.w(TAG, "Score still waiting confirmation? " + m_blePlayerToConfirmOwnScore);
                    m_iBoard.appendToInfoMessage(".");
                }
            } else {
                if ( eButtonPressed.equals(m_eInitiateSelfScoreChangeButton) ) {
                    Log.w(TAG, String.format("Score for %s entered with button %s now waiting for confirmation by opponent %s pressing %s", playerWristBand, eButtonPressed, playerWristBand.getOther(), m_eConfirmScoreByOpponentButton));
                    m_blePlayerWaitingForScoreToBeConfirmed = playerWristBand;

                    String sToConfirmMsg = getBLEMessage(R.string.ble_player_x_confirm_score_for_y_by_pressing_z, playerWristBand.getOther(), playerWristBand, m_eConfirmScoreByOpponentButton);
                    m_iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                    startWaitingForBLEConfirmation(m_blePlayerWaitingForScoreToBeConfirmed, playerWristBand.getOther());
                } else if ( eButtonPressed.equals(m_eInitiateOpponentScoredChangeButton) ) {
                    Log.w(TAG, String.format("Score for opponent entered by %s with button %s now waiting for confirmation by scoring player %s pressing %s", playerWristBand, eButtonPressed, playerWristBand.getOther(), m_eConfirmScoreBySelfButton));
                    m_blePlayerToConfirmOwnScore = playerWristBand.getOther();

                    String sToConfirmMsg = getBLEMessage(R.string.ble_player_x_confirm_you_scored_by_pressing_y, playerWristBand.getOther(), m_eConfirmScoreBySelfButton);
                    m_iBoard.showBLEInfoMessage(sToConfirmMsg, -1);
                    startWaitingForBLEConfirmation(m_blePlayerToConfirmOwnScore, playerWristBand.getOther());
                } else {
                    Log.w(TAG, String.format("In state waiting for initiate-score-change, button %s does nothing ", eButtonPressed));
                    if ( PreferenceValues.currentDateIsTestDate() ) {
                        String sInfoMsg = getBLEMessage(R.string.ble_waiting_initiate_score_change_message, m_eInitiateSelfScoreChangeButton);
                        m_iBoard.showBLEInfoMessage(sInfoMsg, 4);
                    }
                }
            }
        }
    }

    @Override public boolean clearBLEConfirmationStatus() {
        if ( m_blePlayerWaitingForScoreToBeConfirmed != null || m_blePlayerToConfirmOwnScore != null ) {
            stopWaitingForBLEConfirmation();
            m_blePlayerWaitingForScoreToBeConfirmed = null;
            m_blePlayerToConfirmOwnScore = null;
            m_context.showInfoMessage(R.string.ble_score_cancelled_via_gui, 5);
            return true;
        }
        return false;
    }

    public void stopWaitingForBLEConfirmation() {
        for(Player p: Player.values() ) {
            waitForBLEConfirmation(p, null, false);
        }
    }
    public void startWaitingForBLEConfirmation(Player pScorer, Player pToConfirm) {
        waitForBLEConfirmation(pScorer, pToConfirm, true);
    }
    private final PlayerFocusEffectCountDownTimerConfirm m_timerBLEConfirm = new PlayerFocusEffectCountDownTimerConfirm();
    private void waitForBLEConfirmation(Player pScorer, Player pToConfirm, boolean bWaiting) {
        ShowScoreChangeOn guiElementToUseForFocus = ShowScoreChangeOn.ScoreButton; // TODO: from options
        if ( bWaiting ) {
            int iNrOfSecs = PreferenceValues.nrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired(m_context);
            m_timerBLEConfirm.start(guiElementToUseForFocus, pScorer, pToConfirm, iNrOfSecs);
        } else {
            m_iBoard.guiElementColorSwitch(guiElementToUseForFocus, pScorer, FocusEffect.BlinkByInverting, 0, 0);
            m_iBoard.guiElementColorSwitch(guiElementToUseForFocus, pScorer, FocusEffect.SetTransparency, 0, 0);
            m_timerBLEConfirm.myCancel();
        }
    }

    private static final int I_CONFIRM_COUNTDOWN_INTERVAL = 50;
    private class PlayerFocusEffectCountDownTimerConfirm extends PlayerFocusEffectCountDownTimer {
        private Player m_pNotifyAfterXSecs = null;
        private int    m_iNotifyAfterXSecs = 3;

        PlayerFocusEffectCountDownTimerConfirm() {
            super(FocusEffect.SetTransparency, 60 * 1000, I_CONFIRM_COUNTDOWN_INTERVAL, m_iBoard);
        }
        public void start(ShowScoreChangeOn guiElementToUseForFocus, Player p, Player pNotifyAfterXSecs, int iNotifyAfterXSecs) {
            m_iInvocationCnt                 = 0;
            m_guiElementToUseForFocus        = guiElementToUseForFocus;
            m_player                         = p;
            m_pNotifyAfterXSecs              = pNotifyAfterXSecs;
            m_iNotifyAfterXSecs              = iNotifyAfterXSecs;
            super.start();
        }
        @Override public void doOnTick(int iInvocationCnt, long millisUntilFinished) {
            if ( iInvocationCnt == m_iNotifyAfterXSecs * (1000 / I_CONFIRM_COUNTDOWN_INTERVAL) && m_pNotifyAfterXSecs != null ) {
                // after 3 seconds let wristband of player to confirm vibrate
                if ( notifyBLE(m_pNotifyAfterXSecs, BLEUtil.Keys.PokeConfig) ) {
                    String sAppend = getBLEMessage(R.string.ble_signalled_x_to_confirm, m_pNotifyAfterXSecs);
                    m_iBoard.appendToInfoMessage(sAppend, true);
                }
            }
        }

        @Override public void doOnFinish() {
            clearBLEWaitForConfirmation();
        }
    }

    @Override public boolean clearBLEWaitForConfirmation() {
        if ( m_blePlayerWaitingForScoreToBeConfirmed != null || m_blePlayerToConfirmOwnScore != null ) {
            m_blePlayerWaitingForScoreToBeConfirmed = null;
            m_blePlayerToConfirmOwnScore            = null;
            stopWaitingForBLEConfirmation();
            String sUIMsg = m_context.getResources().getQuantityString(R.plurals.ble_ready_for_scoring_with_devices, m_nrOfBLEDevicesConnected);
            updateBLEConnectionStatus(View.VISIBLE, m_nrOfBLEDevicesConnected, sUIMsg, 10);
            return true;
        }
        return false;
    }

    /** invoked by the BLEHandler */
    @Override public void updateBLEConnectionStatus(int visibility, int nrOfDevicesConnected, String sMsg, int iDurationSecs) {
        if ( m_iBoard == null ) {
            return;
        }

        if ( 0 <= nrOfDevicesConnected && nrOfDevicesConnected <=2 ) {
            m_iBoard.updateBLEConnectionStatusIcon(visibility, nrOfDevicesConnected);
            m_nrOfBLEDevicesConnected = nrOfDevicesConnected;
        } else {
            // nrOfDevicesConnected = actually the battery level
        }
        if ( visibility == View.INVISIBLE ) {
            m_iBoard.showInfoMessage(null, -1);
        } else {
            if ( StringUtil.isNotEmpty(sMsg) ) {
                m_iBoard.showInfoMessage(sMsg, iDurationSecs);
            }
        }
        TextView vTxt = m_context.findViewById(R.id.sb_bluetoothble_nrofconnected);
        if ( vTxt != null && (vTxt.hasOnClickListeners() == false) ) {
            vTxt.setOnClickListener(v -> {
                showBLEDevicesBatteryLevels();
            } );
            vTxt.setOnLongClickListener(v -> {
                showBLEVerifyConnectedDevicesDialog(m_nrOfBLEDevicesConnected);
                return true;
            });
        }
    }

    private Player m_bleRequestBatteryLevelOf = Player.A;
    private void showBLEDevicesBatteryLevels() {
        // actual level will be returned async via an INFO message
        if ( m_nrOfBLEDevicesConnected == 2 ) {
            m_bleReceiverManager.readBatteryLevel(m_bleRequestBatteryLevelOf);
            m_bleRequestBatteryLevelOf = m_bleRequestBatteryLevelOf.getOther(); // for next call
        } else {
            m_bleReceiverManager.readBatteryLevel(null);
        }
    }

    private void showBLEVerifyConnectedDevicesDialog(int iNrOfDevices) {
        VerifyConnectedDevices verify = new VerifyConnectedDevices(m_context, ScoreBoard.getMatchModel(), m_context);
        verify.init(iNrOfDevices, m_bleReceiverManager);
        verify.show();
        //dialogManager.show(verify);
    }

    @Override public void selectBleDevices() {
        if ( mBluetoothAdapter == null ) {
            Toast.makeText(m_context, R.string.bt_no_bluetooth_on_device, Toast.LENGTH_SHORT).show();
            return;
        }
        if ( PreferenceValues.useBluetoothLE(m_context) == false ) { return; }

        m_bBLEDevicesSelected = false;

        //String[] permissions = BLEUtil.getPermissions();
        //ActivityCompat.requestPermissions(this, permissions, PreferenceKeys.UseBluetoothLE.ordinal());

        // ensure currently connected devices are disconnected and there for will start broadcasting again to show up in BLEActivity
        stop();

        Intent bleActivity = new Intent(m_context, BLEActivity.class);
        m_context.startActivityForResult(bleActivity, R.id.sb_ble_devices);
    }
    @Override public void selectBleDevices_handleResult(boolean bResultOk, Intent data) {
        if ( bResultOk ) {
            Bundle extras = data.getExtras();
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral1, m_context, "");
            PreferenceValues.setString(PreferenceKeys.BluetoothLE_Peripheral2, m_context, "");
            for(Player p : Player.values() ) {
                String sAddress = extras.getString(p.toString());
                PreferenceKeys key = p.equals(Player.A) ? PreferenceKeys.BluetoothLE_Peripheral1 : PreferenceKeys.BluetoothLE_Peripheral2;
                PreferenceValues.setString(key, m_context, sAddress);
            }
            m_bBLEDevicesSelected = true;
            if ( PreferenceValues.getLandscapeLayout(m_context).equals(LandscapeLayoutPreference.Default) ) {
                if ( PreferenceValues.setEnum(PreferenceKeys.useChangeSidesFeature, m_context, Feature.Automatic) ) {
                    Toast.makeText(m_context, R.string.ble_change_sides_automated_in_ble_mode, Toast.LENGTH_LONG).show();
                }
            }

            m_context.onResumeInitBluetoothBLE();
        } else {
            m_bBLEDevicesSelected = false;
        }
    }

    @Override public void promoteAppToUseBLE() {
        PreferenceValues.setBoolean(PreferenceKeys.UseBluetoothLE                 , m_context, true);
        PreferenceValues.setBoolean(PreferenceKeys.blinkFeedbackPerPoint          , m_context, true);
        PreferenceValues.setBoolean(PreferenceKeys.showActionBar                  , m_context, false);
        PreferenceValues.setEnum   (PreferenceKeys.useTimersFeature               , m_context, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useSpeechFeature               , m_context, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useOfficialAnnouncementsFeature, m_context, Feature.DoNotUse);
        PreferenceValues.setEnum   (PreferenceKeys.endGameSuggestion              , m_context, Feature.Automatic);
        PreferenceValues.setEnum   (PreferenceKeys.useChangeSidesFeature          , m_context, Feature.DoNotUse);
        PreferenceValues.setEnum   (PreferenceKeys.LandscapeLayoutPreference      , m_context, LandscapeLayoutPreference.Presentation1);
        if ( PreferenceValues.currentDateIsTestDate() ) {
            PreferenceValues.setEnum   (PreferenceKeys.StartupAction        , m_context, StartupAction.BLEDevices);
        }
        Toast.makeText(m_context, String.format("D-SCORE BLE option enabled. Use menu option: %s", m_context.getString(R.string.pref_BluetoothLE_Devices)), Toast.LENGTH_LONG).show();
        //RWValues.Permission permission = PreferenceValues.doesUserHavePermissionToAccessFineLocation(this, true);
    }
}
