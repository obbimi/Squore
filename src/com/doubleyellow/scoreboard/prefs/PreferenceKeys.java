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

package com.doubleyellow.scoreboard.prefs;

import com.doubleyellow.scoreboard.archive.GroupMatchesBy;
import com.doubleyellow.scoreboard.match.MatchTabbed;
import com.doubleyellow.scoreboard.model.DoublesServeSequence;
import com.doubleyellow.scoreboard.model.FinalSetFinish;
import com.doubleyellow.scoreboard.model.GoldenPointFormat;
import com.doubleyellow.scoreboard.model.HandicapFormat;
import com.doubleyellow.scoreboard.model.NewBalls;
import com.doubleyellow.scoreboard.model.TieBreakFormat;
import com.doubleyellow.scoreboard.timer.ViewType;
import com.doubleyellow.util.Feature;
import com.doubleyellow.util.SortOrder;

import java.util.EnumSet;
import java.util.List;

/**
 * Collection of keys used to store preferences
 */
public enum PreferenceKeys {
    viewedChangelogVersion(Integer.class, false),
    SBPreferences(null, false), // the root of preferences.xml

    Behaviour(null, false),
        continueRecentMatch(Feature.class, false),
        endGameSuggestion(Feature.class, false),
        /* @Deprecated */
        OrientationPreference(EnumSet.class, true),
            LandscapeLayoutPreference(com.doubleyellow.scoreboard.prefs.LandscapeLayoutPreference.class, false),
        showActionBar(Boolean.class, false),
        showFullScreen(Boolean.class, false),
        showTextInActionBar(Boolean.class, false),
        blinkFeedbackPerPoint(Boolean.class, false),
            numberOfBlinksForFeedbackPerPoint(Integer.class, false),
        hapticFeedbackPerPoint(Boolean.class, false),
        hapticFeedbackOnGameEnd(Boolean.class, false),
        numberOfCharactersBeforeAutocomplete(Integer.class, false),
        numberOfCharactersBeforeAutocompleteCountry(Integer.class, false),
        NewMatchesType(NewMatchesType.class, false),
            maxNumberOfPlayersInGroup(Integer.class, false),
        showTips(Boolean.class, false),
        lockMatchMV(EnumSet.class, false),
            numberOfMinutesAfterWhichToLockMatch(Integer.class, false),
        BackKeyBehaviour(BackKeyBehaviour.class, false),
        VolumeKeysBehaviour(VolumeKeysBehaviour.class, false),
        showFieldDivisionOn(EnumSet.class, false),
        hideFieldDivisionWhenGameInProgress(Boolean.class, false),

        changeSides(null, false),
            /** @Deprecated boolean */
            swapPlayersBetweenGames(Boolean.class, false),
            swapPlayersHalfwayGame(Feature.class, false),
            changeSidesWhen_GSM(EnumSet.class, false),
            useChangeSidesFeature(Feature.class, false),
            swapPlayersOn180DegreesRotationOfDeviceInLandscape(Boolean.class, false),
        /* Tabletennis: Expedite system */
        modeUsageCategory(null, false),
            autoShowModeActivationDialog(Boolean.class, false),
            showModeDialogAfterXMins(Integer.class, false),
        /* Tabletennis: toweling down */
        pauseGame(null, false),
            showGamePausedDialog(Feature.class, false),
            autoShowGamePausedDialogAfterXPoints(Integer.class, false),
        /* Tabletennis, Racketlon */
        numberOfServiceCountUpOrDown(DownUp.class, false),
    statistics(null, false),
        recordRallyEndStatsAfterEachScore(Feature.class, false),
        recordRallyEndStatsDetails(EnumSet.class, false),

    Sharing(null, false),
        smsResultToNr(String.class, false),
        mailResultTo(String.class, false),
        mailFullScoringSheet(Boolean.class, false),

    webIntegration(null, false),
        useFeedAndPostFunctionality(Boolean.class, false),
        Feeds(null, false),
            /** Technical */
            FeedFeedsURL(String.class, false),
            feedPostUrls(String.class, false),
            useFeedNameAsEventName(Boolean.class, false),
            /** Stored an integer, pointing to the 'Active' feed url */
            feedPostUrl(Integer.class, false),

            tournamentWasBusy_DaysBack(Integer.class, false),
            tournamentWillStartIn_DaysAhead(Integer.class, false),
            tournamentMaxDuration_InDays(Integer.class, false),
            removeSeedingAfterMatchSelection(Boolean.class, false),
            switchToPlayerListIfMatchListOfFeedIsEmpty(Feature.class, false),
            myUsedFeedTypes(List.class, false),
        additionalPostKeyValuePairs(String.class, false),
        postDataPreference(PostDataPreference.class, false),
        maximumCacheAgeFlags(Integer.class, false),
        /** in minutes */
        maximumCacheAgeFeeds(Integer.class, false),
        /** in seconds */
        feedReadTimeout(Integer.class, false),
        allowTrustAllCertificatesAndHosts(Boolean.class, false),
        FlagsURLs(null, false),
            prefetchFlags(Boolean.class, false),
/*
            FeedName(null, false),
            FeedURL_players(null, false),
            FeedURL_matches(null, false),
            PostURL_matchResult(null, false),
*/
        /** if true, ensure it is NOT triggered if ShareAction = PostResult */
        autoSuggestToPostResult(Boolean.class, false),
        savePasswords(Boolean.class, false),

        groupMatchesInFeedByCourt(Boolean.class, false),
        hideCompletedMatchesFromFeed(Boolean.class, false),

        useReferees(Boolean.class, false),
            refereeList(List.class, false),
                refereeName(String.class, false),
                markerName(String.class, false),
                assessorName(String.class, false),
        playerList(List.class, false),
        myMatches(null, false),
            matchList(null, false),
            removeMatchFromMyListWhenSelected(Feature.class, false),
            useGroupNameAsEventData(Boolean.class, false),

      //matchListValidUntil(null, false),

        OfficialRulesURLs(String.class, false),

    Contacts(null, false),
        readContactsForAutoCompletion(Boolean.class, false),
        readContactsForAutoCompletion_max(Integer.class, false),
        onlyForContactGroups(Integer.class, false),

    useEventPreviousValuesAsDefault(Boolean.class, false),
    eventList(null, false),
        eventLast(String.class, false),
    divisionList(null, false),
        divisionLast(String.class, false),
    roundList(null, false),
        roundLast(String.class, false),
    courtList(null, false),
        courtLast(String.class, false),
    locationList(null, false),
        locationLast(String.class, false),
    clubList(null, false),
        clubListLastA(null, false),
        clubListLastX(null, false),

    Brand(null, false),
        squoreBrand(Brand.clazz, true),
        showBrandLogoOn(EnumSet.class, false),
        hideBrandLogoWhenGameInProgress(Boolean.class, false),
    showNewMatchFloatButton(Boolean.class, false),
    Timers(null, false),
        useTimersFeature(Feature.class, false),
        showLastGameInfoInTimer(Boolean.class, false),
        showPauseButtonOnTimer(Boolean.class, false),
        showHideButtonOnTimer(Boolean.class, false), // no actual preference (yet) for showcase
        showCircularCountdownInTimer(Boolean.class, false),
        cancelTimerWhenTimeIsUp(Boolean.class, false),
        timerWarmup(Integer.class, false),
        timerWarmup_values(null, false),
        timerPauseBeforeFirstGame(Integer.class, false),
        timerPauseBeforeFirstGame_values(null, false),
        timerPauseBetweenGames(Integer.class, false),
        timerPauseBetweenGames_values(null, false),
      //timerInjury(null, false),
            timerSelfInflictedInjury(null, false),
            timerSelfInflictedBloodInjury(null, false),
            timerContributedInjury(null, false),
            timerOpponentInflictedInjury(null, false),
            // mid-game timers (tabletennis)
            timerTowelingDown(null, false),
            timerTimeout(null, false),
        useSoundNotificationInTimer(Boolean.class, false),
        useVibrationNotificationInTimer(Boolean.class, false),
        showAdjustTimeButtonsInTimer(Boolean.class, false),
        showUseAudioCheckboxInTimer(Boolean.class, false),
        timerViewType(ViewType.class, false),
        showTimeIsAlreadyUpFor_Chrono(Boolean.class, false),
        showMatchDurationChronoOn(EnumSet.class, false),
        showLastGameDurationChronoOn(EnumSet.class, false),
    //showWhoServesDialog(null, false),
    useTossFeature(Feature.class, false),
    useSpeechFeature(Feature.class, false),
        speechVoice(String.class, false),
        speechPitch(Integer.class, false),
        speechRate(Integer.class, false),
        speechPauseBetweenParts(Integer.class, false),
        speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Boolean.class, false),
        speechOverBT_PauseBetweenPlaysToKeepAlive(Integer.class, false),
        speechOverBT_PlayingVolumeToKeepAlive(Integer.class, false),

    useShareFeature(Feature.class, false),
        shareAction(ShareMatchPrefs.class, false),
    //showOfficalAnnouncements(null, false),
    useOfficialAnnouncementsFeature(Feature.class, false),
        officialAnnouncementsLanguage(AnnouncementLanguage.class, false),
    //showTieBreakDialog(null, false),
    showDetailsAtEndOfGameAutomatically(Boolean.class, false),

    //showWhoServesFAB(null, false),
    //showOfficialAnnouncementsFAB(null, false),
    //showTimersFAB(null, false),

    MatchFormat(null, false),
        /** = numberOfSetsToWin for GSMModel */
        numberOfGamesToWinMatch(Integer.class, false), // also used as JSONKey
            /** special case: not best-of-x, but total-of-x games */
            playAllGames(Boolean.class, false),
        /* = numberOfGamesToWinSet for GSMModel model */
        numberOfPointsToWinGame(Integer.class, false), // also used as JSONKey
        /** tabletennis specific */
        numberOfServesPerPlayer(Integer.class, false),
        useHandInHandOutScoring(Boolean.class, false),
        tieBreakFormat(TieBreakFormat.class, false),
        /** Racketlon specific */
        disciplineSequence(EnumSet.class, false),

        doublesServeSequence(DoublesServeSequence.class, false),
        /** to start with a score other than 0-0 */
        handicapFormat(HandicapFormat.class, false),
            allowNegativeHandicap(Boolean.class, false),

        /** GSMModel*/
        finalSetFinish(FinalSetFinish.class, false), // also used as JSONKey
        newBalls(NewBalls.class, false),       // also used as JSONKey
            newBallsXGamesUpFront(Integer.class, false),
        goldenPointFormat(GoldenPointFormat.class, false), // also used as JSONKey
        StartTiebreakOneGameEarly(Boolean.class, false), // also used as JSONKey
    PowerPlay(null, false),
        usePowerPlay(Boolean.class, false),
        numberOfPowerPlaysPerPlayerPerMatch(Integer.class, false),

    StartupAction(StartupAction.class, false),

    Visuals(null, false),
        indicateGameBall(Boolean.class, false),
        indicateGoldenPoint(Boolean.class, false),
        //floatingMessageForGameBall(null, false),
        floatingMessageForGameBallOn(EnumSet.class, false),
        //keepScreenOn(null, false),
        keepScreenOnWhen(KeepScreenOnWhen.class, false),
        //showScoringHistoryInMainScreen(null, false),
        showScoringHistoryInMainScreenOn(EnumSet.class, false),

        AppealHandGestureIconSize(Integer.class, false),
        showChoosenDecisionShortly(Boolean.class, false),
            showChoosenDecisionDuration_Appeal(Integer.class, false),
            showChoosenDecisionDuration_Conduct(Integer.class, false),
        showAvatarOn(EnumSet.class, false),
            hideAvatarForSameImage(Boolean.class, false),
        showCountryAs(EnumSet.class, false),
            hideFlagForSameCountry(Boolean.class, false),

    Appearance(null, false),
        //TextSizeScoreAsBigAsPossible(null, false),
        scorelineLayout(ScorelineLayout.class, false),
        newMatchLayout(NewMatchLayout.class, false),
        serveButtonTransparencyNonServer(Integer.class, false),
        gameScoresAppearance(GameScoresAppearance.class, false),
    Colors(null, false),
        colorSchema(Integer.class, false),
        /** grouping **/
        backgroundColors(null, false),
        /* @Deprecated */
        //textColorDynamically(null, false),
        textColorDetermination(DetermineTextColor.class, false),
        textColors(null, false),
        showPlayerColorOn(EnumSet.class, false),
            PlayerColorsNewMatch(PlayerColorsNewMatch.class, false),
                PlayerColorsNewMatchA(String.class, false),
                PlayerColorsNewMatchB(String.class, false),
            showPlayerColorOn_Text(Boolean.class, false),

    Misc(null, false),
        resetPrefs(null, false),
    saveMatchesForLaterUsage(Boolean.class, false),
        //sortMatchesByEvent(null, false),

    LiveScorePrefs(null, false),
        LiveScoreOpenURL(null, false),
        liveScoreDeviceId(String.class, false),
        liveScoreDeviceId_customSuffix(String.class, false),
        postEveryChangeToSupportLiveScore(Boolean.class, false),
        turnOnLiveScoringForMatchesFromFeed(Boolean.class, false),

/*
    FCMPrefs(null, false),
        FCMEnabled(null, false),
        showToastMessageForEveryReceivedFCMMessage(null, false),
*/

    /* not really preferences but makes them manageable from 'parent' app */
    FromParent(null, false),
        //hideStartNewMatch(null, false),
        //hideSelectFromMyMatches(null, false),
        //disableEditPlayerNames(null, false),
        //captionForSelectNewMatch(null, false),
        captionForMessageMatchResult(String.class, false),
        captionForPostMatchResultToSite(String.class, false),
        iconForPostMatchResultToSite(String.class, false),
        captionForEmailMatchResult(String.class, false),

    MatchTabbed_defaultTab(MatchTabbed.SelectTab.class, false),
    ArchiveTabbed_defaultTab(String.class, false),

    groupArchivedMatchesBy(GroupMatchesBy.class, false),
    sortOrderOfArchivedMatches(SortOrder.class, false),

    targetDirForImportExport(String.class, false),

    ChromeCast(null, false),
        showCastButtonInActionBar(Boolean.class, false),
        useCastScreen(Integer.class, false),
            castScreenLogoUrl(String.class, false),
                castScreenShowLogo(Boolean.class, false),
            castScreenSponsorUrl(String.class, false),
                castScreenShowSponsor(Boolean.class, false),
          //Cast_ShowGraphDuringTimer(null, false),

    // BT mirror/sync
    /** e.g. for table tennis: referee holding 'controlling' device with A on left and B on right, while on mirrored device (tablet on referee table but facing the players) A is on the right and B is on the left */
    BTSync_keepLROnConnectedDeviceMirrored(Boolean.class, false),
    BTSync_showFullScreenTimer(Boolean.class, false),

    enableScoringByBluetoothConnection(null, false),
        lastConnectedBluetoothDevice(String.class, false),
    /** e.g use 'Previous song' for scoring for player A, and 'Next song' for scoring for B, and play/pause for 'Undo' */
    allowForScoringWithBlueToothConnectedMediaControlButtons(Boolean.class, false),
        mediaControlMode(MediaControlMode.class, false),

    wearable_(null, false),
        wearable_syncColorPrefs(Boolean.class, false),
        wearable_allowScoringWithHardwareButtons(Boolean.class, false),
        wearable_allowScoringWithRotary(Boolean.class, false),
        wearable_keepScreenOnWhen(null, false),

    AutomateWhatCanBeAutomatedPrefs(null, false),

    BluetoothLE(null, false),
        UseBluetoothLE(Boolean.class, false),
        BluetoothLE_Peripheral1(String.class, false),
        BluetoothLE_Peripheral2(String.class, false),
        BluetoothLE_Config(null, false),
            ShortDescription(String.class, false),
        BluetoothLE_AutoReconnect(null, false),
        NrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired(Integer.class, false),
        ShowFeedBackOnBLEButtonsPressedInfoMessages(Boolean.class, false),
        IgnoreAccidentalDoublePress_ThresholdInMilliSeconds(Integer.class, false),
        BLEBridge_ClassName(String.class, false),

    UseMQTT(Boolean.class, false),
        MQTTBrokerURL(String.class, false),
            MQTTBrokerURL_Custom(String.class, false),
        MQTTPublishJoinerLeaverTopic(String.class, false),
        MQTTPublishTopicMatch(String.class, false),
        MQTTPublishTopicUnloadMatch(String.class, false),
        MQTTPublishTopicDeviceInfo(String.class, false),
        MQTTPublishDeviceInfo_EveryXSeconds(Integer.class, false),
        MQTTSubscribeTopic_remoteControl(String.class, false),
            MQTTSkipJsonKeys(List.class, false),
        MQTTPublishTopicChange(String.class, false),
        MQTTSubscribeTopicChange(String.class, false),
            MQTTOtherDeviceId(String.class, false),
        MQTTDisableInputWhenSlave(Boolean.class, false),

    ImportExportReset(null, false),
        RemoteSettingsURL(String.class, true),
        RemoteSettingsURL_Default(String.class, true),
        RemoteSettingsURL_AlwaysShowLoadErrors(Boolean.class, false),

    useSinglesMatchesTab(Boolean.class, false),
    useDoublesMatchesTab(Boolean.class, false),
    useMyListFunctionality(Boolean.class, false),

    kioskMode(KioskMode.class, true),
    hideMenuItems(List.class, false),
    showMenuItems(List.class, false),

    restartMode(RestartMode.class, false),

    ;
    private Class clazz = String.class;
    private boolean bRestartRequired = false;
    PreferenceKeys(Class c, boolean bRestartRequired) {
        this.clazz = c;
        this.bRestartRequired = bRestartRequired;
    }
    public Class getType() {
        return clazz;
    }
    public boolean restartRequired() {
        return bRestartRequired;
    }

}
