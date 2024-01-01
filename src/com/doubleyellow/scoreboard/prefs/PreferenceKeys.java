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

/**
 * Collection of keys used to store preferences
 */
public enum PreferenceKeys {
    viewedChangelogVersion,
    SBPreferences, // the root of preferences.xml

    Behaviour,
        continueRecentMatch,
        endGameSuggestion,
        /* @deprecated */
        OrientationPreference,
        showActionBar,
        showFullScreen,
        showTextInActionBar,
        hapticFeedbackPerPoint,
        hapticFeedbackOnGameEnd,
        numberOfCharactersBeforeAutocomplete,
        numberOfCharactersBeforeAutocompleteCountry,
        NewMatchesType,
            maxNumberOfPlayersInGroup,
        showTips,
        /** @deprecated */
        lockMatch,
        lockMatchMV,
            numberOfMinutesAfterWhichToLockMatch,
        BackKeyBehaviour,
        VolumeKeysBehaviour,
        showFieldDivisionOn,
        hideFieldDivisionWhenGameInProgress,

        changeSides,
            /** @deprecated boolean */
            swapPlayersBetweenGames,
            swapPlayersHalfwayGame,
            changeSidesWhen_GSM,
            useChangeSidesFeature,
            swapPlayersOn180DegreesRotationOfDeviceInLandscape,
        /* Tabletennis: Expedite system */
        modeUsageCategory,
            autoShowModeActivationDialog,
            showModeDialogAfterXMins,
        /* Tabletennis: toweling down */
        pauseGame,
            showGamePausedDialog,
            autoShowGamePausedDialogAfterXPoints,
        /* Tabletennis, Racketlon */
        numberOfServiceCountUpOrDown,
    statistics,
        recordRallyEndStatsAfterEachScore,
        recordRallyEndStatsDetails,

    Sharing,
        smsResultToNr,
        mailResultTo,
        mailFullScoringSheet,

    webIntegration,
        useFeedAndPostFunctionality,
        Feeds,
            /** Technical */
            FeedFeedsURL,
            feedPostUrls,
            useFeedNameAsEventName,
            /** Stored an integer, pointing to the 'Active' feed url */
            feedPostUrl,

            tournamentWasBusy_DaysBack,
            tournamentWillStartIn_DaysAhead,
            tournamentMaxDuration_InDays,
            removeSeedingAfterMatchSelection,
            switchToPlayerListIfMatchListOfFeedIsEmpty,
            myUsedFeedTypes,
        additionalPostKeyValuePairs,
        postDataPreference,
        maximumCacheAgeFlags,
        maximumCacheAgeFeeds,
        /** in seconds */
        feedReadTimeout,
        allowTrustAllCertificatesAndHosts,
        FlagsURLs,
            prefetchFlags,
/*
            FeedName,
            FeedURL_players,
            FeedURL_matches,
            PostURL_matchResult,
*/
        /** if true, ensure it is NOT triggered if ShareAction = PostResult */
        autoSuggestToPostResult,
        savePasswords,

        groupMatchesInFeedByCourt,
        hideCompletedMatchesFromFeed,

        refereeList,
            refereeName,
            markerName,
            assessorName,
        playerList,
        myMatches,
            matchList,
            removeMatchFromMyListWhenSelected,
            useGroupNameAsEventData,

        matchListValidUntill,

        OfficialSquashRulesURLs,

    Contacts,
        readContactsForAutoCompletion,
        readContactsForAutoCompletion_max,
        onlyForContactGroups,

    eventList,
        eventLast,
    divisionList,
        divisionLast,
    roundList,
        roundLast,
    courtList,
        courtLast,
    locationList,
        locationLast,
    clubList,
        clubListLastA,
        clubListLastX,

    Brand,
        squoreBrand,
        showBrandLogoOn,
        hideBrandLogoWhenGameInProgress,
    showNewMatchFloatButton,
    Timers,
        //showTimersAutomatically,
        useTimersFeature,
        showLastGameInfoInTimer,
        showPauseButtonOnTimer,
        showHideButtonOnTimer, // no actual preference (yet) for showcase
        showCircularCountdownInTimer,
        cancelTimerWhenTimeIsUp,
        timerWarmup,
        timerWarmup_values,
        timerPauseBetweenGames,
        timerPauseBetweenGames_values,
      //timerInjury,
            timerSelfInflictedInjury,
            timerSelfInflictedBloodInjury,
            timerContributedInjury,
            timerOpponentInflictedInjury,
            // mid-game timers (tabletennis)
            timerTowelingDown,
            timerTimeout,
        useSoundNotificationInTimer,
        useVibrationNotificationInTimer,
        showAdjustTimeButtonsInTimer,
        showUseAudioCheckboxInTimer,
        timerViewType,
        showTimeIsAlreadyUpFor_Chrono,
        showMatchDurationChronoOn,
        showLastGameDurationChronoOn,
    //showWhoServesDialog,
    useTossFeature,
    useSpeechFeature,
        speechPitch,
        speechRate,
        speechPauseBetweenParts,
    useShareFeature,
        shareAction,
    //showOfficalAnnouncements,
    useOfficialAnnouncementsFeature,
        officialAnnouncementsLanguage,
    //showTieBreakDialog,
    showDetailsAtEndOfGameAutomatically,

    //showWhoServesFAB,
    //showOfficialAnnouncementsFAB,
    //showTimersFAB,

    MatchFormat,
        /** = numberOfSetsToWin for GSMModel */
        numberOfGamesToWinMatch, // also used as JSONKey
            /** special case: not best-of-x, but total-of-x games */
            playAllGames,
        /* = numberOfGamesToWinSet for GSMModel model */
        numberOfPointsToWinGame, // also used as JSONKey
        /** tabletennis specific */
        numberOfServesPerPlayer,
        useHandInHandOutScoring,
        tieBreakFormat,
        /** Racketlon specific */
        disciplineSequence,

        doublesServeSequence,
        /** to start with a score other than 0-0 */
        handicapFormat,
            allowNegativeHandicap,

        /** GSMModel*/
        finalSetFinish, // also used as JSONKey
        newBalls,       // also used as JSONKey
            newBallsXGamesUpFront,
        /** @deprecated */
        goldenPointToWinGame, // also used as JSONKey
        goldenPointFormat, // also used as JSONKey
        StartTiebreakOneGameEarly, // also used as JSONKey
    PowerPlay,
        usePowerPlay,
        numberOfPowerPlaysPerPlayerPerMatch,

    StartupAction,

    Visuals,
        indicateGameBall,
        indicateGoldenPoint,
        //floatingMessageForGameBall,
        floatingMessageForGameBallOn,
        //keepScreenOn,
        keepScreenOnWhen,
        //showScoringHistoryInMainScreen,
        showScoringHistoryInMainScreenOn,

        AppealHandGestureIconSize,
        showChoosenDecisionShortly,
            showChoosenDecisionDuration_Appeal,
            showChoosenDecisionDuration_Conduct,
        showAvatarOn,
            hideAvatarForSameImage,
        showCountryAs,
            hideFlagForSameCountry,

    Appearance,
        //TextSizeScoreAsBigAsPossible,
        scorelineLayout,
        newMatchLayout,
        serveButtonTransparencyNonServer,
        gameScoresAppearance,
    Colors,
        colorSchema,
        /** grouping **/
        backgroundColors,
        /* @deprecated */
        //textColorDynamically,
        textColorDetermination,
        textColors,
        showPlayerColorOn,
            PlayerColorsNewMatch,
                PlayerColorsNewMatchA,
                PlayerColorsNewMatchB,
            showPlayerColorOn_Text,

    Misc,
        resetPrefs,
    saveMatchesForLaterUsage,
        //sortMatchesByEvent,

    LiveScorePrefs,
        LiveScoreOpenURL,
        liveScoreDeviceId,
        postEveryChangeToSupportLiveScore,
        turnOnLiveScoringForMatchesFromFeed,

    FCMPrefs,
        FCMEnabled,
        FCMDeviceId,
        showToastMessageForEveryReceivedFCMMessage,

    /* not really preferences but makes them manageable from 'parent' app */
    FromParent,
        //hideStartNewMatch,
        //hideSelectFromMyMatches,
        //disableEditPlayerNames,
        //captionForSelectNewMatch,
        captionForMessageMatchResult,
        captionForPostMatchResultToSite,
        iconForPostMatchResultToSite,
        captionForEmailMatchResult,

    MatchTabbed_defaultTab,
    ArchiveTabbed_defaultTab,

    groupArchivedMatchesBy,
    sortOrderOfArchivedMatches,

    targetDirForImportExport,

    ChromeCast,
        useCastScreen,
            castScreenLogoUrl,
                castScreenShowLogo,
            castScreenSponsorUrl,
                castScreenShowSponsor,
          //Cast_ShowGraphDuringTimer,

    // BT mirror/sync
    /** e.g. for table tennis: referee holding 'controlling' device with A on left and B on right, while on mirrored device (tablet on referee table but facing the players) A is on the right and B is on the left */
    BTSync_keepLROnConnectedDeviceMirrored,
    BTSync_showFullScreenTimer,

    enableScoringByBluetoothConnection,
        lastConnectedBluetoothDevice,
    /** e.g use 'Previous song' for scoring for player A, and 'Next song' for scoring for B, and play/pause for 'Undo' */
    allowForScoringWithBlueToothConnectedMediaControlButtons,

    wearable_,
        wearable_syncColorPrefs,
        wearable_allowScoringWithHardwareButtons,
        wearable_allowScoringWithRotary,
        wearable_keepScreenOnWhen,

    AutomateWhatCanBeAutomatedPrefs,

    UseBluetoothLE,
    BluetoothLE_Peripheral1,
    BluetoothLE_Peripheral2,
    BluetoothLE_Config,
}
