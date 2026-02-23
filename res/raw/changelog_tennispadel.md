## 4.59 (Feb 2026)

- Add option to play golden point 'only' after third deuce a.k.a. in Padel as 'Star Point'

## 4.58 (July 2025)

- target sdk 35 enforced by google
- minor improvements
- Allow quickly adding matches for 'rotating doubles'
 
## 4.55 (Jan 2025)

- first version with MQTT capabilities (experimental)
  - Score mirroring can be achieved (similar functionality as Bluetooth-mirror)
  - 'Local' live score on a LAN when running a local mqtt broker
- improvements in mirroring
  - Bug fixes in Bluetooth-mirroring
  - Allow the 'slave' device of a mirrored pair to have a 'presentation' layout when in landscape orientation
    - showing complete score more pleasing to the eye

## 4.53 (Dec 2023)
 
- Experimental: allow scoring by using a specific keys on bluetooth connected keyboard
  - Pg Up  , A : score point for player A
  - Pg Down, B : score point for player B
  - Delete , U : undo last score
- The above hopefully also allows e.g. for controlling score using 'slide presenters'
- Allow to start tie-break one game early
- Bugfixes
  - Match/Set duration incorrect values at end of match
  - Correct server indication if sets ends in 7-5
   
## 4.53 (August 2023)

- additional setting for 'Golden Point': 
  - Off
  - Pro (On First Deuce)
  - Amateur (On Second Deuce)

## 4.52 (July 2023)

- add option to 'Changes Sides' setting called 'After First Point In Tiebreak'
  - This allows to, combined with 'Every 4 Point In a Tiebreak', to configure the so called 'Coman' tiebreak

## 4.51 (March 2023)

- minor improvements for working with feeds
- bugfix for posting result to website (totalpointsplayer1, totalpointsplayer2) 

## 4.50 (okt 2022)

- on wearable, consult the current time without leaving the score app
  - long press both score buttons 
- added a few missing Czech translations (thanks to Josef Hohenberger)

## 4.49 (July 2022)

Improvements in:

- Indicate details of tiebreak score
- Allow indication for when it is time for 'New balls': additional optin
  - After First 11, Then Each 13th game

## 4.49 (May 2022)

Improvements in:

- 'Speech' during tiebreak
- Fields in 'New match with larger font' screen

## 4.48 (May 2022)

Allow indication for when it is time for 'New balls'

- After First  7, Then Each  9th game
- After First  9, Then Each 11th game
- Before Set 3

Improvements for
 
- Undo back into previous game
- Spoken scores (speech)
- Dialogs on android television

## 4.47 (April 2022)

Improvements in 

- Scoring via media playback control buttons (bluetooth)
- My List and default match format

## 4.47 (March 2022)

- Improvements
    - show floating message when Golden Point is played
    - allow to adjust score to easily pick up in the middle of a match already in progress
- Bugfix
    - Share Match > Score Summary fixed    
    - App sometimes froze on setball for certain settings    

## 4.45 (jan 2022)

- Allow scoring via media playback control buttons (bluetooth). e.g.
    - use 'Previous song' for scoring for player A, 
    - use 'Next song' for scoring for B, 
    - and 'Play/Pause' for Undo
    - You need to specifically enable this options in 'Settings > Behaviour'
- Improvements for casting 

## 4.44 (jan 2022)

- Improvement for wearables:
    - allow scoring points with hardware buttons (if 2 additional hardware buttons are available besides the main OS button)
- Color preferences from phone synced to wearable (experimental)    
- Other minor improvements

## 4.43 (dec 2021)

- feed improvements for team vs team: 
    - allow specifying player `teamPlayers.avatar`
    - allow specifying team `team.abbreviation` and team `team.color`

## 4.42 (okt 2021)

- Improvement for wearables:
    - allow scoring points with the rotary/bezel
    - allow to specify 'Golden point' when entering new match
- Bugfix for feeds for Android Pie (Android 9, 2018, SDK 28) and older

## 4.40 (sep 2021)

- Improvements in bluetooth mirroring
    - Allow for full screen timer on 'Slave'
    - Other minor improvements

## 4.39 (feb 2021)

- Portuguese translation. Thanks to Gonçalo Afonso.
- Allow to play games with a golden point: decisive point when 40-40 (no AD)

## 4.38 (nov 2020)

- Re-use chosen colors for players in subsequent matches ( Settings > Appearance > Colors )

## 4.37 (okt 2020)

- new options for the final set
  - no games - tie break to 7 (or 10)
  - games to 12 - followed by tie break to 7 (or 10)

## 4.36 (june 2020)

- The app has been translated into Spanish. Thank you very much Salvador Martinez!

## 4.33 (april 2020)

- added option for final set
  - play tie break to 7, or
  - play tie break to 10

## 4.32 (april 2020)

- WearOS version of the app is now available
  - the basic functionality has been ported to the wearable app
  - communication/sync between handheld and wear version of app is possible

## 4.31 (march 2020)

- allow configuration on swap sides
  - when it should happen
  - should it happen automatically or should it be 'suggested'
- two 'modes' two show scores of previous sets
  - nr of sets won
  - end score of previous sets
- Chrome cast

## 4.30 (feb 2020)

- first version of the app converted for Tennis/Padel focused on
  - store/restore sets in/from json format
  - undo score taking 'sets' into account
  - (auto) swap sides
    - after odd nr of games
    - after 6 points in tiebreak
  - Show timer floating button only between odd games
  - Set duration in addition to game/match duration

- Double check
  - Serve side in doubles
  - Terminology in settings

- Known issues
  - Score history details: showing games of last set, should be sets
  - Blue tooth mirroring
  - manually change score dialog
  - Quick intro
    - intermediate dummy scores not tennis/padel like

- TODO
  - Online Help pages
  - revisit 'Shared scoresheet'
