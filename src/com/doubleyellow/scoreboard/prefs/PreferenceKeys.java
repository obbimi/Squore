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
            swapPlayersBetweenGames,
            swapPlayersHalfwayGame,
            swapPlayersOn180DegreesRotationOfDeviceInLandscape,

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
        additionalPostKeyValuePairs,
        postDataPreference,
        maximumCacheAgeFlags,
        maximumCacheAgeFeeds,
        /** in seconds */
        feedReadTimeout,
        FlagsURLs,
            prefetchFlags,
/*
            FeedName,
            FeedURL_players,
            FeedURL_matches,
            PostURL_matchResult,
*/
        autoSuggestToPostResult,
        savePasswords,

        hideCompletedMatchesFromFeed,

        refereeList,
            refereeName,
            markerName,
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
        timerPauseBetweenGames,
        timerPauseBetweenGames_values,
      //timerInjury,
            timerSelfInflictedInjury,
            timerContributedInjury,
            timerOpponentInflictedInjury,
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
        numberOfGamesToWinMatch,
        numberOfPointsToWinGame,
        /** tabletennis specific */
        numberOfServesPerPlayer,
        useHandInHandOutScoring,
        tieBreakFormat,
        /** Racketlon specific */
        disciplineSequence,

        doublesServeSequence,
        /** to start with a score other than 0-0 */
        handicapFormat,

    StartupAction,

    Visuals,
        indicateGameBall,
        //floatingMessageForGameBall,
        floatingMessageForGameBallOn,
        keepScreenOn,
        keepScreenOnWhen,
        //showScoringHistoryInMainScreen,
        showScoringHistoryInMainScreenOn,

        AppealHandGestureIconSize,
        showChoosenDecisionShortly,
            showChoosenDecisionDuration_Appeal,
            showChoosenDecisionDuration_Conduct,
        showCountryAs,
            hideFlagForSameCountry,

    Appearance,
        //TextSizeScoreAsBigAsPossible,

    Colors,
        colorSchema,
        /** grouping **/
        backgroundColors,
        /* @deprecated */
        //textColorDynamically,
        textColorDetermination,
        textColors,
        showPlayerColorOn,

    Misc,
        resetPrefs,
    saveMatchesForLaterUsage,
        //sortMatchesByEvent,

    LiveScorePrefs,
        LiveScoreOpenURL,

    /* not really preferences but makes them manageable from 'parent' app */
    FromParent,
        //hideStartNewMatch,
        //hideSelectFromMyMatches,
        //disableEditPlayerNames,
        //captionForSelectNewMatch,
        captionForMessageMatchResult,
        captionForPostMatchResultToSite,
        captionForEmailMatchResult,

    MatchTabbed_defaultTab,
    ArchiveTabbed_defaultTab,

    groupArchivedMatchesBy,
    sortOrderOfArchivedMatches,

    targetDirForImportExport,

    Cast_ShowGraphDuringTimer,
    //showGraphDuringTimerOn,
}
