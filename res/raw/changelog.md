## 4.59 (okt 2025)

- support new WSF rules for pause before games
  - 60 seconds between warmup and start of first game
  - 120 seconds between games
- minor improvements


## 4.58 (july 2025)

- target sdk 35 enforced by google 
- minor improvements

## 4.57 (may/june 2025)

- german translation (Thanks to Ralph Bieber)
- You can now use a URL from which to fetch settings. 
  - Typically used to more easily have all devices for a certain event or league use the same settings.

## 4.56 (april 2025)

- feed improvements
  - storing password for posting result to a website (issue 81)
  - prevent invalid scores (issue 78)
- cast improvements
  - fix for cast screen stuck in 'Loading...'

## 4.55 (jan 2025)

- first version with MQTT capabilities (experimental)
  - Score mirroring can be achieved (similar functionality as Bluetooth-mirror)
  - 'Local' live score on a LAN when running a local mqtt broker 
- improvements in mirroring
  - Bug fixes in Bluetooth-mirroring
  - Allow the 'slave' device of a mirrored pair to have a 'presentation' layout when in landscape orientation
    - showing complete score more pleasing to the eye   
 
## 4.54 (march 2024)

- allow specifying different layout for landscape display
  - mainly to have better readable score elements
- allow turning on 'blink' visual feedback after a score change
  - Specifically handy if score is controlled remotely.

## 4.53 (nov 2023)

- If match is manually ended because of Injury or Conduct, ensure the winner is specified if the result is posted to a website
- Option to use Power Play (experimental)

## 4.53 (sept 2023)

- option to specify 'assessor'
- make referee/marker/assessor visible in 'Score Details'
- allow to change 'match date' for 'Stored matches'
  - e.g. when entering matches into Squore that were initially scored on paper 
- option to allow negative scores when using Handicap option  
 
## 4.52 (june 2023)

- send info about state of timer to livescore pages for better experience
- Sync last used player colors to wearable


__EOF__

git clone https://github.com/obbimi/Squore.git Squore.tmp
cd Squore.tmp
#git checkout 0c2614e8

cp -rpv ../Squore/gradle .
cp -pv  ../Squore/gradlew .
cp -pv  ../Squore/google-services.json .
cp -pv  ../Squore/res/raw/squore_iddo.zip ./res/raw/.

echo "sdk.dir=/osshare/software/google/android-sdk-linux" > local.properties
./change.R.package.sh TennisPadel
./clean.and.assemble.sh 508
