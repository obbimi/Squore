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

## 4.38 (nov 2020)

- Re-use chosen colors for players in subsequent matches ( Settings > Appearance > Colors )

## 4.36 (june 2020)

- The app has been translated into Spanish. Thank you very much Salvador Martinez!

## 4.32 (april 2020)

- support for Wearables
- minor other improvements

## 4.20 (july 2019)

- improvement for Android 9
- minor improvements
    - for player buttons for long names
    - for bluetooth mirroring

## 4.19 (feb 2019)

- allow bluetooth-connection between two (already paired) devices each running the scoreboard
    - keeping the score on one device while the other device mirrors the same score
    - useful for mirroring score on second (larger) screen if no ChromeCast and/or internet available
    - can also be used to simply transfer the score from one device to another (once connection is lost, score remains on mirrored device)

## 4.17 (jan 2019)

- improve autocomplete for double player names
- Other minor improvements

## 4.12 (aug 2018)

- upgraded to use new google-play library to improve ChromeCast for e.g. Android 8

## 4.07 (31 jul 2017)

- improvements for live scoring

## 4.07 (18 jul 2017)

- Added easy 'change side/swap players' support
    - flipping device forward 180 degrees (e.g. to show score to players will flip the player names so it still looks naturally for them too)
    - Halfway of set, when players should change ends, scoreboard suggest to swap/flip players on scoreboard as well

## 4.06 (16 jul 2017)

- Fix for downloading backup files for Android 7
- Improved sharing for Doubles matches

## 4.05 (11 jul 2017)

- improved match(ball) 'calculation' in last set if started with 2 points difference
- added missing Dutch translations

## 4.04 (09 jul 2017)

- allow playing all racketlon disciplines in a different order
    - at startup select first discipline
    - between subsequent sets allow specifying unplayed disciplines
    - without any specific action the default order (Tabletennis, Badminton, Squash, Tabletennis) will be maintained
- improvements for Doubles

## 4.03 (07 jul 2017)

- fix issues with scoring after `Adjust Score` was used

## 4.02 (04 jul 2017)

- serve indication not with numbers during tabletennis (looks a bit like 'number of games won' at first glance)
- other minor fixes

## 4.01 (24 jun 2017)


- match result summary should state something like +24 or +15, in stead of separate set results
- separate url for help/sharing/matches/feeds (<http://racketlon.double-yellow.be> in stead of <http://squore.double-yellow.be>)
- enable `Adjust Score` dialog

## 4.00 (16 jun 2017)

- First version. Stable 'Squore' app converted from Squash to Racketlon

- Additions/modifications
    - indicate serve side according to Racketlon rules
    - use `Set ball` in stead of `Game ball`
    - in racketlon it can be `Matchball` in the middle of a set
    - `Gummi-arm point` message if applicable
    - `Change sides message` halfway set (except for squash of course)
    - best of x sets is not an option for racketlon, by default play to 21 in stead of 11
    - time between sets defaults to 180 seconds (3 minutes)
    - besides the set score of finished sets there is a '+xx' indicator to show a players current advantage
    - removed tabs 'Statistics' and 'Conduct/appeal overview' as they are 'less' applicable for Racketlon 
    - `Quick intro` is adapted to use Racketlon terms
    - `Appeal` and `Conduct` dialogs have been 'disabled'
    - provide link to Racketlon rules 
    - Hidden Squash specific preferences:
        - Winners/Error/Statistics
        - Match format

__EOF__

- TODO
    - when adding dummy score keep R and L in line with possibilities for Racketlon
    - adapt to support doubles the Racketlon way (squash is played as single, half way the set players exchange places)
    - Usage of the word 'Game' in preference screen should be 'Set'
    - Hide preferences:
        - Automate
        - Announcement feature
        - End game 'Suggest'

I have made a spin-of of my fairly widely use squash app 'Squore' to an app for Racketlon. 
I don't know if there is any real need for it, but at least now you know that it exists.

Ik heb een spin-of van Squore (Scorebord voor Squash) voor Racketlon gemaakt. 
Ik weet niet of er echt behoefte aan is... maar ik kon in de Google Play Store nog helemaal niets voor Racketlon vinden. 
Nu is er in elk geval al wel eentje.

<https://play.google.com/store/apps/details?id=com.doubleyellow.racketlon>

Suggesties zijn welkom.