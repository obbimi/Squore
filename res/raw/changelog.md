## 4.50 (okt 2022)

- on wearable, consult the current time without leaving the score app
  - long press both score buttons
- added a few missing Czech translations (thanks to Josef Hohenberger)
- bugfix for the old hand-in-hand-out scoring format

## 4.48 (may 2022)

Improvements for 

- Dialogs on android television
- Specifying handicap format

## 4.47 (april 2022)

Improvements in 

- Scoring via media playback control buttons (bluetooth)
- My List and default match format

## 4.47 (march 2022)

Minor bugfixes.

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

- Bugfix for feeds for Android Pie (Android 9, 2018, SDK 28) and older
- Improvement for wearables:
    - allow scoring points with the rotary/bezel

## 4.40 (sep 2021)

- Improvements in bluetooth mirroring
    - Allow for full screen timer on 'Slave'
    - Other minor improvements

- Portuguese translation. Thanks to Gonçalo Afonso.

## 4.38 (nov 2020)

- Re-use chosen colors for players in subsequent matches ( Settings > Appearance > Colors )
- Introduction of 'Livescore Device Id' to allow more precise filtering on 'Live Score' web page

## 4.36 (june 2020)

- The app has been translated into Spanish. Thank you very much Salvador Martinez!

## 4.35 (may 2020)

- allowing the score to be spoken with 'text-to-speech' functionality
    - needs to be specifically enabled via _Settings_/_Speech_
    - first version with speech: let me know if you experience issues

## 4.32 (april 2020)

- WearOS version of the app is now available
  - the basic functionality has been ported to the wearable app
  - communication/sync between handheld and wear version of app is possible

## 4.29 (jan 2020)

Improvements in bluetooth mirroring
- ensure conducts are also shown on mirrored device

## 4.28 (dec 2019)

- allow group matches from a feed by 'court' (if 'court' is specified per match in the feed)
- turn on bluetooth automatically if 'Mirror score (Bluetooth)' is chosen

## 4.27 (nov 2019)

- Squore has been translated into Italian. Thank you very much Lior Lotem!

## 4.26 (okt 2019)

- allow specifying a player color already in main match setup screen
- bluetooth mirror improvements

## 4.25 (okt 2019)

- further improvements for selecting teams (and team players in a popup) when working with feeds

## 4.24 (sept 2019)

- improvements for selecting teams (and teamplayers in a popup) when working with feeds
- allow to play 'Total of X games' match format (as opposed to 'Best of X games')

## 4.21 (sept 2019)

- at start of match allow to adjust warmup duration ( e.g. to switch from 4 minutes (official) to 5 minutes (previous official value))

## 4.20 (august 2019)

- if matches have been shared, the 'Summary' of multiple matches will include the URLs
- additional 'Authentication' method for secured integrating with third party web sites

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

- bugfix in serveside/hand-out indication for doubles
- improve autocomplete for double player names
    - if you start typing a name in 'Player A1' and select e.g. 'Carlos/Diego' from the suggested list
        - field 'Player A1' is filled with 'Carlos' and 
        - field 'Player A2' is filled with 'Diego'

## 4.16 (dec 2018)

- allow 'undo' for both players by swiping left-to-right or right-to-left
    - for player who scored last this was already possible
    - for other player a dialog will pop-up to confirm the undo last score made for this player

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

## 4.12 (aug 2018)

- upgraded to use new google-play library to improve ChromeCast for e.g. Android 8

## 4.11 (june 2018)

- added 'Serve' as possible option when app is in 'record statistics' mode
- added Czech translations for new features/preferences

## 4.10 (march 2018)

- Configurable 'transparency' of the serve button of receiver
    - this makes the serve button of the server stand out a little more
- Improvements for using deviation color palette

## 4.09 (march 2018)

- optionally use a simpler dialog for manually starting a new match
    - shows only player names and match format fields with bigger font
    - to activate use Settings/Appearance/New match layout

## 4.08 (feb 2018)

- allow specifying a court for a match
    - specifically useful in combination with Live Score
- minor corrections for official announcements in German
- improvements for colors/color palettes    

## 4.07 (jan 2018)

- Add support for JSON feeds to select matches from
    - More flexible than current 'plain text' feeds
    - sample json can be found [here](http://squore.double-yellow.be/demo/demo.matches.json)
    - ask your clubs webmaster to provided a similar feed with matches to be played at/for your club
- More info about working with feeds within Squore can be found [here](http://squore.double-yellow.be/#Feeds) 

## 4.06 (dec 2017)

- Squore has been translated into Czech. Thank you very much Filip Hurta!

## 4.05 (nov 2017)

- allow selecting reasons when manually ending match
    - Conduct Match, or
    - Retired because of injury
- Showing the timer in the device notification area as soon as Squore app has moved into the background. Allows to quickly
    - Check time left without re-opening Squore
    - Switch back to Squore by clicking the notification timer
- feed improvements
    - support redirects for custom feeds
    - allow specifying club names matches in feed (between brackets)

## 4.04 (okt 2017)

- some improvements to icons/images used
    - appeal dialog
    - toss icon
- bugfix when selecting players with short names
- moved several menu options into the 'navigation drawer'
    - opening a menu like in the top-right corner is 'old fashioned'
    - more commonly used options are moved to the navigation drawer (swipe to the right starting from the left edge, or press the icon on the top-left)    

## 4.03 (aug 2017)

- much improved page for [Live Score](http://squore.double-yellow.be/live)
    - if all matches at your tournament are reffed using score
        - you will have a nice overview of all matches on all courts
        - not just nice, but it will making planning next matches easier
        - switching to the 'Presentation' tab and using 'Full Screen' you have a nice screen to present to somewhere at your venue using e.g. Chrome browser in combination with Chrome Cast
- allow starting a match by pressing on single player in player list from feed
- other minor fixes

## 4.02 (jul 2017)

- _End Game_ button needs confirmation if score not appropriate to end game
- In `Stored matches` prevent accidental deletes
    - `Delete` menu option is moved to last position
    - `Delete` will ask for confirmation
- Minor changes to official announcements to be in line with WSF guidelines
- Allow swiping down to refresh in match lists: Matches from Feed, Your matches, Stored matches
- Fix for downloading backup files for Android 7
- Improved sharing for Doubles matches
- For racketlon lovers: there is a special racketlon version of Squore now: [Racketlon Score](market://details?id=com.doubleyellow.racketlon)

## 3.67 (15 mar 2017)

- optionally show field/division on main screen on chromecast (and optionally on device)

## 3.66 (26 feb 2017)

- allow selecting multiple matches at once (Stored Matches) and sharing a summary of the matches. E.g. after several matches have been played between 2 clubs.
    - summary of each match
    - summary of total of matches, games and points won

## 3.65 (23 jan 2017)

- choosing 'Back' in Match Format dialog after selecting the match from a list, now returns to the list
    - e.g. if you accidentally selected an incorrect match
- re-selecting matches from 'My List' with a corresponding recently reffed match (within a timeframe of a few hours), will continue this stored match in stead of starting a new one
- duration of match and/or game is now shown on screen in a small chronometer. 
    - this can be turned off in _Settings_

## 3.64 (24 dec 2016)

- added **A1/B1/B2 then A1/A2/B1/B2** as possible doubles serving sequence 
- new menu option **End match** to end a match when desired. E.g. when playing a limited period of time.

## 3.63 (20 dec 2016)

- bugfix for unresponsive menu items in 'New match' on some devices

## 3.62 (11 dec 2016)

- bugfixes for doubles

## 3.61 (27 nov 2016)

- when exporting matches:
    - have option to upload zip file with all matches to squore server
    - communicate/share the URL of the zip file to easily download it, allowing you to 
        - save it on any desired location
        - import the matches back into Squore installed on an other device 

## 3.60 (12 nov 2016)

### New
- allow specifying club names for players. See:
    <http://squore.double-yellow.be/#Club_Country>

### Improvements
- bug fix for Tie break format 'Sudden Death'
- improvements for NFC/AndroidBeam. See: <http://squore.double-yellow.be/#Transfer>
- improvements for ChromeCast
- other minor improvements

## 3.58 (16 sep 2016)

### New
* Option to specify a country per player
    - auto filled if selecting a player from a feed where the country is specified between brackets
    - to manually enter the country start typing the name of the country (autocomplete will kick in)
    
* If countries are selected/specified the corresponding flag can be displayed on (configurable) 
    - the device
    - the chrome cast screen
* If countries are selected/specified  
    - full country name is presented in official announcement dialogs
    
### Improvements
- Several minor improvements

## 3.57 (29 may 2016)

### New
* Show tiny chronometer at end of 'Pause' timer (allowing you to see how many time a player is 'late' getting back on court)

### Improvements
- Improvements for ChromeCast

