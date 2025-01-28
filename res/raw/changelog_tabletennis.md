## 4.55 (jan 2025)

- first version with MQTT capabilities (experimental)
  - Score mirroring can be achieved (similar functionality as Bluetooth-mirror)
  - 'Local' live score on a LAN when running a local mqtt broker
- improvements in mirroring
  - Bug fixes in Bluetooth-mirroring
  - Allow the 'slave' device of a mirrored pair to have a 'presentation' layout when in landscape orientation
    - showing complete score more pleasing to the eye

## 4.53 (sept 2023)

- allow to change 'match date' for 'Stored matches'
    - e.g. when entering matches into app that were initially scored on paper

## 4.51 (march 2023)

- minor improvements for working with feeds

## 4.50 (okt 2022)

- on wearable, consult the current time without leaving the score app
    - long press both score buttons
- added a few missing Czech translations (thanks to Josef Hohenberger)

## 4.48 (may 2022)

Improvements for 

- Dialogs on android television
- Specifying handicap format

## 4.47 (april 2022)

Improvements in 

- Scoring via media playback control buttons (bluetooth)
- My List and default match format

## 4.47 (march 2022)

- Improvements
    - Allow to correct server if initial server indication was accidentally incorrect (Menu > Edit > Swap Server)

## 4.45 (jan 2022)

- Allow scoring via media playback control buttons. e.g.
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

- Fix in doubles receiver indication
    - In 'tie break'
    - Halfway last game
- Improvement for wearables:
    - allow scoring points with the rotary/bezel
- Bugfix for feeds for Android Pie (Android 9, 2018, SDK 28) and older

## 4.40 (sep 2021)

- Improvements in bluetooth mirroring
    - Allow for full screen timer on 'Slave'
    - Allow for players being displayed 'swapped' on 'Slave', relative to 'Master'
        - allowing for 'Slave' device to be used as old-fashioned 'flapping' scoreboard
    - Other minor improvements

- Portuguese translation. Thanks to Gonçalo Afonso.

## 4.38 (nov 2020)

- Keep track of who requested a timeout and when
- Re-use chosen colors for players in subsequent matches ( Settings > Appearance > Colors )

## 4.36 (june 2020)

- The app has been translated into Spanish. Thank you very much Salvador Martinez!

## 4.35 (may 2020)

- allowing the score to be spoken with 'text-to-speech' functionality
    - needs to be specifically enabled via _Settings_/_Speech_
    - first version with speech: let me know if you experience issues
- some bug fixes

## 4.33 (april 2020)

- WearOS version of the app is now available
  - the basic functionality has been ported to the wearable app
  - communication/sync between handheld and wear version of app is possible
- bugfixes in handicap and deviating 'nr of servers per player'

## 4.29 (jan 2020)

Improved 'toss' functionality at start of a match

- perform a toss
    - let winner choose 'serve' or 'receive'
    - let other player choose side/end of table

Improve server/receiver indication for doubles:    

- at start of each game, indicate which player of the serving team serves first 
- at start of first game, indicate which player of the receiving team receives first 


## 4.28 (dec 2019)

- turn on bluetooth automatically if 'Mirror score (Bluetooth)' is chosen
- minor improvements for feeds

## 4.26 (okt 2019)

- allow specifying a player color already in main match setup screen
- bluetooth mirror improvements

## 4.25 (okt 2019)

- further improvements for selecting teams (and team players in a popup) when working with feeds
- some bug fixes

## 4.23 (sept 2019)

- allow to play 'Total of X games' match format (as opposed to 'Best of X games')

## 4.20 (june 2019)

- chromecast improvements

## 4.19 (april 2019)

- android 9 chromecast improvements
- minor improvements
    - for player buttons for long names
    - for bluetooth mirroring
    - for doubles

## 4.19 (feb 2019)

- allow bluetooth-connection between two (already paired) devices each running the scoreboard
    - keeping the score on one device while the other device mirrors the same score
    - useful for mirroring score on second (larger) screen if no ChromeCast and/or internet available
    - can also be used to simply transfer the score from one device to another (once connection is lost, score remains on mirrored device)

## 4.18 (jan 2019)

- improvement for casting with Android 9
- other minor improvements

## 4.17 (jan 2019)

- improve autocomplete for double player names
    - if you start typing a name in 'Player A1' and select e.g. 'Carlos/Diego' from the suggested list
        - field 'Player A1' is filled with 'Carlos' and 
        - field 'Player A2' is filled with 'Diego'
- using latest 'google chrome cast' software library        

## 4.16 (dec 2018)

- allow 'undo' for both players by swiping left-to-right or right-to-left
    - for player who scored last this was already possible
    - for other player a dialog will pop-up to confirm the undo last score made for this player
- fix bug in serve sequence for doubles

## 4.15 (nov 2018)

- more flexibility when manually entering upcoming matches to ref ('My Matches')
    - choose between poule matches, or
    - matches between teams, where
        - each team member plays one match against one team member of the other team
        - each team member plays matches against all team members of the other team

## 4.14 (okt 2018)

- to have a less 'busy' screen:
    - results of previous games or only show as 'number of games won' (e.g. first 1-0, then 2-0 etc)
    - To toggle (back) to the old 'full games scores' (e.g first only 11-9, then 11-9 and 11-7 etc)
        - tap the games scores, or
        - change the appropriate option in Appearance section of the apps settings screen
- Improvements for showing stored matches

## 4.13 (sep 2018)

- add support for 'Expedite system', activating the setting under `Timers/Expedite System` it will
    - make server switch after each point
    - server is indicated by an 'X' (X-pedite)
    - clicking on the serve side button without the 'X' allows to make correction on who is about to serve  
- add support for 'Toweling down' (setting under `Timers/Toweling Down`)
    - every 6 points (configurable) a count down timer of 60 seconds (configurable) will appear, stopping the game timer
        - the dialog will have a 'Resume game' button, to be pressed when play is resumed
        - if you are not using the dialog, the timer element can be long pressed to trigger the 'Resume Game'
    - should be typically 'activated' if also using the 'Expedite system' to have an accurate game duration
- new preference under Behaviour/Other to choose 
    - if number of serves is counting 
        - down (default, number of serves left) or,
        - up    
- upgraded to use new google-play library to improve ChromeCast for e.g. Android 8

## 4.11 (june 2018)

- Fix issue with unresponsive score button at start of new game
- Other minor improvements

## 4.10 (march 2018)

- Configurable 'transparency' of the serve button of receiver
    - this makes the serve button of the server stand out a little more
- Improvements for using deviation color palette

## 4.09 (march 2018)

- optionally use a simpler dialog for manually starting a new match
    - shows only player names and match format fields with bigger font
    - to activate use Settings/Appearance/New match layout
- allow specifying a table for a match
    - specifically useful in combination with Live Score

## 4.04 (19 jul 2017)

- allow for serving x times (in stead of always 2 times) before serve goes to the other player
- minor bugfixes

## 4.03 (18 jul 2017)

- Added easy 'change side/swap players' support
    - flipping device forward 180 degrees (e.g. to show score to players will flip the player names so it still looks naturally for them too)
    - Between games and halfway of game, when players should change ends, scoreboard can/will swap/flip players on scoreboard as well

## 4.02 (16 jul 2017)

- fixed 'live score' url
- fixed to small timer in portrait mode
- other minor bugfixes

## 4.01 (09 jul 2017)

- First version. Stable 'Squore' app converted from supporting Squash only to support Table Tennis

- Modifications
    - indicate server according to table tennis rules
    - `Change sides message` halfway last game
    - time between game defaults to 60 seconds
    - removed tabs 'Statistics' and 'Conduct/appeal overview' as they are 'not' applicable for table tennis 
    - `Quick intro` is adapted to use table tennis terms
    - `Appeal` and `Conduct` dialogs have been 'removed'
    - provide link to table tennis rules 

__EOF__

Per Section 2.15 of the ITTF Laws of Table Tennis, 
The Expedite System shall come into operation after 10 minutes’ play in a game or 
at any time requested by both players or pairs.  However, the system shall not be 
introduced in a game if at least 18 points have been scored.  During a game, the 
umpire (or assistant umpire) keeps track of total play time.  When 10 minutes play 
time in a game is reached, the umpire institutes The Expedite System.  If the ball 
is in play when the 10 minutes is reached, the umpire shall interrupt play and 
restart the point under the rules of The Expedite System.  The Expedite System 
requires each player to alternate one serve (instead of two) at a time until 
the end of the game.  If the receiving player makes 13 correct returns in a rally, 
the point is awarded to the receiver.  Once introduced, The Expedite System shall 
remain in operation until the end of the match.

- TODO
    - when sharing matches: do not show L and R in score sequence
    - adapt to support doubles the tabletennis way 
    - Hide preferences:
        - Automate
        - Announcement feature
        - End game 'Suggest'

Feed from <http://www.ittf.com/tournaments/#2017>

I have made a spin-of of my fairly widely use squash app 'Squore' to an app for Table Tennis. 
I know there are already several apps to do this, but as far as I know none of them have support for:
- Chrome Cast
- Graphs showing scoring history per game
- integration with tournamentsoftware.com
- Sharing results to have live scoring

Another plus is that this app is completely free and ad-free.

<https://play.google.com/store/apps/details?id=com.doubleyellow.tabletennis>

Suggestions are welcome.

<https://www.ittf.com/?media-alias=2018_ITTF_Handbook>
2.15 Expedite
3.4.4.1.2 Toweling down

