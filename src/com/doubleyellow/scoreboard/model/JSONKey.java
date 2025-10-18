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

public enum JSONKey {
	score,
		result,
		gamescores,
		gameStartScoreOffset,
		server,
		serveSide,
		isHandOut,
		timerInfo,
		isGameBall,
		isMatchBall,
		isVictoryFor,
	referee,
		markers,
		assessors,
	players,
		colors,
		countries,
		clubs,
		avatars,
		playerids,
		firstName,
		lastName,
	format,
	  //numberOfPointsToWinGame, // = PreferenceKeys
	  //numberOfGamesToWinMatch, // = PreferenceKeys
			/** total-of-x-games (true) in stead of best-of-x-games (false) */
			playAllGames,
		mode,
		// tabletennis specific key
		nrOfServersPerPlayer,
    	handicapFormat,
		/** @Deprecated  */
		nrOfGamesToWinMatch,
    	// squash specific keys
		useHandInHandOutScoring,
		tiebreakFormat,
		doublesServeSequence,
        // racketlon specific keys
        disciplineSequence,
		maxNrOfPowerPlays,
		nrOfPowerPlaysUsed,
		currentRallyIsPowerPlayFor,
	when,
		/** yyyyMMdd format */
		date,
		/** HHMM format */
		time,
	timing,
		start,
		end,
		offsets,
	statistics,
	conductCalls,
		call,
		type,
		player,
		game,
	event,
		name,
		division, field,
		round,
		location,
	    court,
	lockState,
		winnerBecauseOf,
	sport,
	appName,
	appPackage,
	metadata,
		source,
		sourceID, id,
		sourcePostResultUrl,
		sourceFeedbackState,
		shareURL,
		additionalPostParams,
	wifi,
	device,
		batteryPercentage,
		batteryCharging,

  //player,    // duplicate
	  //name,  // duplicate
	    club,  // club (tournament) or team (league)
			abbreviation, // team/club abbreviation to show between brackets in the name
			teamId,
			teamPlayers,
				seqNo,
			color,
	    country,
	    seed,
	    avatar,

	Message,
}

