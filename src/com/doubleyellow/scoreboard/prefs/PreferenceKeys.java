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

/**
 * Collection of keys used to store preferences
 */
public enum PreferenceKeys {
    viewedChangelogVersion(Integer.class),
    SBPreferences(null), // the root of preferences.xml

    Behaviour(null),
        continueRecentMatch(Feature.class),
        endGameSuggestion(Feature.class),
        /* @Deprecated */
        OrientationPreference(null /*EnumSet<com.doubleyellow.prefs.OrientationPreference.class>*/),
            LandscapeLayoutPreference(com.doubleyellow.scoreboard.prefs.LandscapeLayoutPreference.class),
        showActionBar(Boolean.class),
        showFullScreen(Boolean.class),
        showTextInActionBar(Boolean.class),
        blinkFeedbackPerPoint(Boolean.class),
        numberOfBlinksForFeedbackPerPoint(Integer.class),
        hapticFeedbackPerPoint(Boolean.class),
        hapticFeedbackOnGameEnd(Boolean.class),
        numberOfCharactersBeforeAutocomplete(Integer.class),
        numberOfCharactersBeforeAutocompleteCountry(Integer.class),
        NewMatchesType(NewMatchesType.class),
            maxNumberOfPlayersInGroup(Integer.class),
        showTips(Boolean.class),
        /** @Deprecated */
        lockMatch(null),
        lockMatchMV(null),
            numberOfMinutesAfterWhichToLockMatch(Integer.class),
        BackKeyBehaviour(BackKeyBehaviour.class),
        VolumeKeysBehaviour(VolumeKeysBehaviour.class),
        showFieldDivisionOn(null),
        hideFieldDivisionWhenGameInProgress(Boolean.class),

        changeSides(null),
            /** @Deprecated boolean */
            swapPlayersBetweenGames(Boolean.class),
            swapPlayersHalfwayGame(Feature.class),
            changeSidesWhen_GSM(null),
            useChangeSidesFeature(Feature.class),
            swapPlayersOn180DegreesRotationOfDeviceInLandscape(Boolean.class),
        /* Tabletennis: Expedite system */
        modeUsageCategory(null),
            autoShowModeActivationDialog(Boolean.class),
            showModeDialogAfterXMins(Integer.class),
        /* Tabletennis: toweling down */
        pauseGame(null),
            showGamePausedDialog(Feature.class),
            autoShowGamePausedDialogAfterXPoints(Integer.class),
        /* Tabletennis, Racketlon */
        numberOfServiceCountUpOrDown(DownUp.class),
    statistics(null),
        recordRallyEndStatsAfterEachScore(Feature.class),
        recordRallyEndStatsDetails(null),

    Sharing(null),
        smsResultToNr(String.class),
        mailResultTo(String.class),
        mailFullScoringSheet(Boolean.class),

    webIntegration(null),
        useFeedAndPostFunctionality(Boolean.class),
        Feeds(null),
            /** Technical */
            FeedFeedsURL(String.class),
            feedPostUrls(String.class),
            useFeedNameAsEventName(Boolean.class),
            /** Stored an integer, pointing to the 'Active' feed url */
            feedPostUrl(Integer.class),

            tournamentWasBusy_DaysBack(Integer.class),
            tournamentWillStartIn_DaysAhead(Integer.class),
            tournamentMaxDuration_InDays(Integer.class),
            removeSeedingAfterMatchSelection(Boolean.class),
            switchToPlayerListIfMatchListOfFeedIsEmpty(Feature.class),
            myUsedFeedTypes(null),
        additionalPostKeyValuePairs(String.class),
        postDataPreference(PostDataPreference.class),
        maximumCacheAgeFlags(Integer.class),
        /** in minutes */
        maximumCacheAgeFeeds(Integer.class),
        /** in seconds */
        feedReadTimeout(Integer.class),
        allowTrustAllCertificatesAndHosts(Boolean.class),
        FlagsURLs(null),
            prefetchFlags(Boolean.class),
/*
            FeedName(null),
            FeedURL_players(null),
            FeedURL_matches(null),
            PostURL_matchResult(null),
*/
        /** if true, ensure it is NOT triggered if ShareAction = PostResult */
        autoSuggestToPostResult(Boolean.class),
        savePasswords(Boolean.class),

        groupMatchesInFeedByCourt(Boolean.class),
        hideCompletedMatchesFromFeed(Boolean.class),

        useReferees(Boolean.class),
            refereeList(null),
                refereeName(String.class),
                markerName(String.class),
                assessorName(String.class),
        playerList(null),
        myMatches(null),
            matchList(null),
            removeMatchFromMyListWhenSelected(Feature.class),
            useGroupNameAsEventData(Boolean.class),

        matchListValidUntill(null),

        OfficialSquashRulesURLs(null),

    Contacts(null),
        readContactsForAutoCompletion(Boolean.class),
        readContactsForAutoCompletion_max(Integer.class),
        onlyForContactGroups(Integer.class),

    useEventPreviousValuesAsDefault(Boolean.class),
    eventList(null),
        eventLast(String.class),
    divisionList(null),
        divisionLast(String.class),
    roundList(null),
        roundLast(String.class),
    courtList(null),
        courtLast(String.class),
    locationList(null),
        locationLast(String.class),
    clubList(null),
        clubListLastA(null),
        clubListLastX(null),

    Brand(null),
        squoreBrand(Brand.clazz),
        showBrandLogoOn(null),
        hideBrandLogoWhenGameInProgress(Boolean.class),
    showNewMatchFloatButton(Boolean.class),
    Timers(null),
        useTimersFeature(Feature.class),
        showLastGameInfoInTimer(Boolean.class),
        showPauseButtonOnTimer(Boolean.class),
        showHideButtonOnTimer(Boolean.class), // no actual preference (yet) for showcase
        showCircularCountdownInTimer(Boolean.class),
        cancelTimerWhenTimeIsUp(Boolean.class),
        timerWarmup(Integer.class),
        timerWarmup_values(null),
        timerPauseBetweenGames(Integer.class),
        timerPauseBetweenGames_values(null),
      //timerInjury(null),
            timerSelfInflictedInjury(null),
            timerSelfInflictedBloodInjury(null),
            timerContributedInjury(null),
            timerOpponentInflictedInjury(null),
            // mid-game timers (tabletennis)
            timerTowelingDown(null),
            timerTimeout(null),
        useSoundNotificationInTimer(Boolean.class),
        useVibrationNotificationInTimer(Boolean.class),
        showAdjustTimeButtonsInTimer(Boolean.class),
        showUseAudioCheckboxInTimer(Boolean.class),
        timerViewType(ViewType.class),
        showTimeIsAlreadyUpFor_Chrono(Boolean.class),
        showMatchDurationChronoOn(null),
        showLastGameDurationChronoOn(null),
    //showWhoServesDialog(null),
    useTossFeature(Feature.class),
    useSpeechFeature(Feature.class),
        speechVoice(String.class),
        speechPitch(Integer.class),
        speechRate(Integer.class),
        speechPauseBetweenParts(Integer.class),
        speechOverBT_PlayWhiteNoiseSoundFileToKeepAlive(Boolean.class),
        speechOverBT_PauseBetweenPlaysToKeepAlive(Integer.class),
        speechOverBT_PlayingVolumeToKeepAlive(Integer.class),

    useShareFeature(Feature.class),
        shareAction(ShareMatchPrefs.class),
    //showOfficalAnnouncements(null),
    useOfficialAnnouncementsFeature(Feature.class),
        officialAnnouncementsLanguage(AnnouncementLanguage.class),
    //showTieBreakDialog(null),
    showDetailsAtEndOfGameAutomatically(Boolean.class),

    //showWhoServesFAB(null),
    //showOfficialAnnouncementsFAB(null),
    //showTimersFAB(null),

    MatchFormat(null),
        /** = numberOfSetsToWin for GSMModel */
        numberOfGamesToWinMatch(Integer.class), // also used as JSONKey
            /** special case: not best-of-x, but total-of-x games */
            playAllGames(Boolean.class),
        /* = numberOfGamesToWinSet for GSMModel model */
        numberOfPointsToWinGame(Integer.class), // also used as JSONKey
        /** tabletennis specific */
        numberOfServesPerPlayer(Integer.class),
        useHandInHandOutScoring(Boolean.class),
        tieBreakFormat(TieBreakFormat.class),
        /** Racketlon specific */
        disciplineSequence(null),

        doublesServeSequence(DoublesServeSequence.class),
        /** to start with a score other than 0-0 */
        handicapFormat(HandicapFormat.class),
            allowNegativeHandicap(Boolean.class),

        /** GSMModel*/
        finalSetFinish(FinalSetFinish.class), // also used as JSONKey
        newBalls(NewBalls.class),       // also used as JSONKey
            newBallsXGamesUpFront(Integer.class),
        goldenPointFormat(GoldenPointFormat.class), // also used as JSONKey
        StartTiebreakOneGameEarly(Boolean.class), // also used as JSONKey
    PowerPlay(null),
        usePowerPlay(Boolean.class),
        numberOfPowerPlaysPerPlayerPerMatch(Integer.class),

    StartupAction(null),

    Visuals(null),
        indicateGameBall(Boolean.class),
        indicateGoldenPoint(null),
        //floatingMessageForGameBall(null),
        floatingMessageForGameBallOn(null),
        //keepScreenOn(null),
        keepScreenOnWhen(KeepScreenOnWhen.class),
        //showScoringHistoryInMainScreen(null),
        showScoringHistoryInMainScreenOn(null),

        AppealHandGestureIconSize(Integer.class),
        showChoosenDecisionShortly(Boolean.class),
            showChoosenDecisionDuration_Appeal(null),
            showChoosenDecisionDuration_Conduct(null),
        showAvatarOn(null),
            hideAvatarForSameImage(Boolean.class),
        showCountryAs(null),
            hideFlagForSameCountry(Boolean.class),

    Appearance(null),
        //TextSizeScoreAsBigAsPossible(null),
        scorelineLayout(ScorelineLayout.class),
        newMatchLayout(NewMatchLayout.class),
        serveButtonTransparencyNonServer(Integer.class),
        gameScoresAppearance(GameScoresAppearance.class),
    Colors(null),
        colorSchema(Integer.class),
        /** grouping **/
        backgroundColors(null),
        /* @Deprecated */
        //textColorDynamically(null),
        textColorDetermination(DetermineTextColor.class),
        textColors(null),
        showPlayerColorOn(null),
            PlayerColorsNewMatch(PlayerColorsNewMatch.class),
                PlayerColorsNewMatchA(String.class),
                PlayerColorsNewMatchB(String.class),
            showPlayerColorOn_Text(Boolean.class),

    Misc(null),
        resetPrefs(null),
    saveMatchesForLaterUsage(Boolean.class),
        //sortMatchesByEvent(null),

    LiveScorePrefs(null),
        LiveScoreOpenURL(null),
        liveScoreDeviceId(null),
        liveScoreDeviceId_customSuffix(String.class),
        postEveryChangeToSupportLiveScore(Boolean.class),
        turnOnLiveScoringForMatchesFromFeed(Boolean.class),

/*
    FCMPrefs(null),
        FCMEnabled(null),
        showToastMessageForEveryReceivedFCMMessage(null),
*/

    /* not really preferences but makes them manageable from 'parent' app */
    FromParent(null),
        //hideStartNewMatch(null),
        //hideSelectFromMyMatches(null),
        //disableEditPlayerNames(null),
        //captionForSelectNewMatch(null),
        captionForMessageMatchResult(String.class),
        captionForPostMatchResultToSite(String.class),
        iconForPostMatchResultToSite(String.class),
        captionForEmailMatchResult(String.class),

    MatchTabbed_defaultTab(MatchTabbed.SelectTab.class),
    ArchiveTabbed_defaultTab(String.class),

    groupArchivedMatchesBy(GroupMatchesBy.class),
    sortOrderOfArchivedMatches(SortOrder.class),

    targetDirForImportExport(String.class),

    ChromeCast(null),
        showCastButtonInActionBar(Boolean.class),
        useCastScreen(Integer.class),
            castScreenLogoUrl(String.class),
                castScreenShowLogo(Boolean.class),
            castScreenSponsorUrl(String.class),
                castScreenShowSponsor(Boolean.class),
          //Cast_ShowGraphDuringTimer(null),

    // BT mirror/sync
    /** e.g. for table tennis: referee holding 'controlling' device with A on left and B on right, while on mirrored device (tablet on referee table but facing the players) A is on the right and B is on the left */
    BTSync_keepLROnConnectedDeviceMirrored(Boolean.class),
    BTSync_showFullScreenTimer(Boolean.class),

    enableScoringByBluetoothConnection(null),
        lastConnectedBluetoothDevice(null),
    /** e.g use 'Previous song' for scoring for player A, and 'Next song' for scoring for B, and play/pause for 'Undo' */
    allowForScoringWithBlueToothConnectedMediaControlButtons(Boolean.class),

    wearable_(null),
        wearable_syncColorPrefs(Boolean.class),
        wearable_allowScoringWithHardwareButtons(Boolean.class),
        wearable_allowScoringWithRotary(Boolean.class),
        wearable_keepScreenOnWhen(null),

    AutomateWhatCanBeAutomatedPrefs(null),

    BluetoothLE(null),
        UseBluetoothLE(Boolean.class),
        BluetoothLE_Peripheral1(null),
        BluetoothLE_Peripheral2(null),
        BluetoothLE_Config(null),
            ShortDescription(null),
        BluetoothLE_AutoReconnect(null),
        NrOfSecondsBeforeNotifyingBLEDeviceThatConfirmationIsRequired(Integer.class),
        ShowFeedBackOnBLEButtonsPressedInfoMessages(Boolean.class),
        IgnoreAccidentalDoublePress_ThresholdInMilliSeconds(Integer.class),
        BLEBridge_ClassName(String.class),

    UseMQTT(Boolean.class),
        MQTTBrokerURL(String.class),
            MQTTBrokerURL_Custom(String.class),
        MQTTPublishJoinerLeaverTopic(String.class),
        MQTTPublishTopicMatch(String.class),
        MQTTPublishTopicUnloadMatch(String.class),
        MQTTSubscribeTopic_newMatch(String.class),
            MQTTSkipJsonKeys(null),
        MQTTPublishTopicChange(String.class),
        MQTTSubscribeTopicChange(String.class),
            MQTTOtherDeviceId(String.class),

    ImportExportReset(null),
        RemoteSettingsURL(String.class),
        RemoteSettingsURL_Default(String.class),
        RemoteSettingsURL_AlwaysShowLoadErrors(Boolean.class),

    useSinglesMatchesTab(Boolean.class),
    useDoublesMatchesTab(Boolean.class),
    useMyListFunctionality(Boolean.class),

    hideMenuItems(null)
    ;
    private Class clazz = String.class;
    PreferenceKeys(Class c) {
        this.clazz = c;
    }
    public Class getType() {
        return clazz;
    }

}
