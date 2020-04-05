#!/usr/bin/env sh

# start in landscape with Phone top to the left (possible on screen software buttons on the right)
# use settings
# - full screen
# - back key is 'undo score (no confirm)'
# - no automatic timers or announcements
# typically start at 6-9 in the 3th game

#set -x
startAt=${1:-1}
endAfter=${2:-6}
#set +x

# INITIALIZE
if [[ -e .brand.txt ]]; then
    brandDefault=$(cat .brand.txt)
else
    brandDefault=Squore
fi
brand=${3:-${brandDefault}}
brandLC=$(echo $brand | tr 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' 'abcdefghijklmnopqrstuvwxyz')

echo $brand > .brand.txt

pkg=com.doubleyellow.${brandLC}
isSquash=0
nrOfPointsToWinGame=11
#set -x
if [[ "${brandLC}" == "squore" ]]; then
    isSquash=1
    nrOfPointsToWinGame=11
    pkg=com.doubleyellow.scoreboard
fi
if [[ "${brandLC}" == "tabletennis" ]]; then
    nrOfPointsToWinGame=11
fi
if [[ "${brandLC}" == "badminton" ]]; then
    nrOfPointsToWinGame=21
fi
set +x

if [[ -n "${ADB_DEVICE}" ]]; then
    echo "Running from laptop for ${ADB_DEVICE}"
else
    if which adb > /dev/null; then
        devices="$(adb devices | egrep -v '(List of|^$)' | sed 's~ *device~~')"
        if [[ -z "$devices" ]]; then
            # no devices... start known emulators
            if which emulator; then
                cd $(dirname $(which emulator))
                emulators="$(emulator -list-avds)"
                for em in ${emulators}; do
                    echo ${em}
                    emulator @${em} &
                done
                # to allow close emulators you do not want to use
                cat <<EO_HELP

Emulators have been started. Please:
- close unwanted emulators
- rotate other to landscape and
    - no timers
    - fullscreen
When done, rerun script

EO_HELP
                exit
            fi
        fi
            # get info about currently installed
            #adb shell dumpsys package ${pkg}

        for dvc in ${devices}; do

            #set -x
            #if which adb; then
                build_version_sdk=$(adb -s ${dvc} shell getprop ro.build.version.sdk | sed -e 's~[^0-9]~~')
                build_product_model=$(adb -s ${dvc} shell getprop ro.product.model)

                productFlavor="phoneTabletPost23"
                if [[ ${build_version_sdk} -lt 23 ]]; then
                    productFlavor="phoneTabletPre22"
                fi
                if echo "${build_product_model}" | grep "wear"; then
                    productFlavor="wearOs"
                fi
            #fi

            # install the app
            if [[ ${startAt} -eq 0 ]]; then
                #set -x
                relapk=$(find /osshare/doc/squash/www.double-yellow.be/app/ -name '*.apk' | egrep -i "Score-${brand}.${productFlavor}.*[0-9]{4}" | sort | tail -1)
                if [[ -e ${relapk} ]]; then
                    adb -s ${dvc} uninstall ${pkg}    2> /dev/null
                    adb -s ${dvc}   install ${relapk}
                else
                    echo "No such file ${relapk}"
                fi
                # start the app
                adb -s ${dvc} shell monkey -p ${pkg} -c android.intent.category.LAUNCHER 1 > tmp.start.log
                if [[ 1 -eq 1 ]]; then
                    echo "Unable to start app ${pkg} ... "
                    exit 1
                fi
                # stop showcase
                adb -s ${dvc} shell input keyevent 4
                adb -s ${dvc} shell input keyevent 4
            else
                echo "Assuming app is installed on ${dvc}"
            fi
            # start the app
            if ! adb -s ${dvc} shell monkey -p ${pkg} -c android.intent.category.LAUNCHER 1; then
                echo "Unable to start app ${pkg} on ${dvc} ... "

                relapk=$(find .. -name "*-${productFlavor}-release.apk")

                echo "Installing ${relapk} version on device ${dvc}..."
                adb -s ${dvc} install -r ${relapk}

                exit 1
            fi

            if adb shell which sed | grep 'not found'; then
                echo "Script NOT invokable on device"
                echo "Script not moved to device. E.g sed,grep not supported on older APIs"
                # invoked this script again with single target device specified in ADB_DEVICE
                export ADB_DEVICE=${dvc}
                ./adb.test.app.sh $1 $2 $3
            else
                export ADB_DEVICE=''
                echo "Script invokable on device"
                # push script files to the device and execute
                adb -s ${dvc} push ./adb.test.* /sdcard/.
                # start the test with the script running on the device
                adb -s ${dvc} shell "cd /sdcard; sh adb.test.app.sh ${startAt} ${endAfter} ${brand}"
            fi
        done
        exit
    fi
fi

# clean screenshots from previous run
if [[ -e adb.test.app.functions.sh ]]; then
    #source adb.test.app.functions.sh
    . ./adb.test.app.functions.sh
    echo "Loaded support functions"
else
    echo "sh file with support functions not found"
    pwd
    ls -l
    exit
fi

m_step=0
function doStep () {
    let m_step=m_step+1
    if [[ ${startAt} -le ${m_step} ]] && [[ ${m_step} -le ${endAfter} ]] ; then
        echo "Executing step $m_step : $1"
        return 0
    else
        echo "Not executing step $m_step : $1 ($startAt to $endAfter)"
        return 1
    fi
}

# adb shell dumpsys activity top | grep Editor          # check if an editor dialog is showing
# adb shell dumpsys activity top | egrep 'Line.+,[1-9]' # analyze how 'high' each setting is, and allow for more accurate clicks
# adb shell dumpsys input_method | grep mInputShown     # check if an on screen keyboard is there
# adb shell dumpsys input_method | grep mServedView     # mServedView=com.doubleyellow.scoreboard.view.PlayerTextView if entering match
                                                        # mServedView=android.widget.ExpandableListView if list is presented
# adb shell dumpsys input_method | grep mCurRootView    # mCurRootView=null if dialog is showing
                                                        # mCurRootView=DecorView@63584a4[Appearance] to determine which subscreen of preferences is showing
                                                        # mCurRootView=DecorView@c755f40[ArchiveTabbed]
# adb shell dumpsys activity top | grep ActionMenuItemView # to check for visible menu items and thus knowing which tab is active from e.g. MatchTabbed
# start new game
removeDumpFile
doSleep 2

if doStep "Start new game"; then
    doBack
    doSleep 2
    showMenuDrawer
    tabOnGUIElement "New singles match" # sb_enter_singles_match
    removeDumpFile

    doSleep 4
    hideOnScreenKeyboard # just to be save

    tabOnGUIElement "match_playerB"
    typeText ${brand}$(date +%S)
    hideOnScreenKeyboard

    tabOnGUIElement "match_playerA"
    typeText Iddo$(date +%M)
    hideOnScreenKeyboard

    #doSleep 1
    #typeEscape
    doSleep 2
    tabOnGUIElement "mt_cmd_ok"
    removeDumpFile
fi

# choose colors
if doStep "Set colors"; then
    longpress btn_side1
    tapCenterScreen
    closeDialogByClicking "OK"

    longpress btn_side2
    closeDialogByClicking "OK"
fi

# mark a game
if doStep "Toss"; then
    openDialogByClicking "float_toss"
    tabOnGUIElement "Toss"
    doSleep 6
    #set -x
    if [[ ${isSquash} -eq 1 ]]; then
        closeDialogByEscape
    else
        # choose serve side
        tabOnGUIElement "Serve"
        doSleep 2

        # choose side
        tabOnGUIElement "Right"
        doSleep 2
        #closeDialogByEscape
    fi
    set +x
fi

if doStep "Mark game"; then
    if [[ ${isSquash} -eq 1 ]]; then
    openDialogByClicking "sb_official_announcement"
    closeDialogByClicking "Start"
    fi

    # appeals A
    if [[ ${isSquash} -eq 1 ]]; then
    openDialogByClicking  "txt_player1"
    closeDialogByClicking "Stroke"
    openDialogByClicking  "txt_player1"
    closeDialogByClicking "Yes Let"
    openDialogByClicking  "txt_player1"
    closeDialogByClicking "No Let"
    fi

    # score A
    tabOnGUIElement "btn_score1"
    tabOnGUIElement "btn_score1"
    tabOnGUIElement "btn_score1"

    # appeals B
    if [[ ${isSquash} -eq 1 ]]; then
    openDialogByClicking  "txt_player2"
    closeDialogByClicking "Stroke"
    openDialogByClicking  "txt_player2"
    closeDialogByClicking "Yes Let"
    openDialogByClicking  "txt_player2"
    closeDialogByClicking "No Let"
    fi

    # score B
    tabOnGUIElement "btn_score2"
    tabOnGUIElement "btn_score2"
    tabOnGUIElement "btn_score2"
    doSleep 2

    # conduct
    if [[ ${isSquash} -eq 1 ]]; then
    longpress "txt_player2"
    closeDialogByClicking "Conduct stroke"
    doSleep 2

    longpress "txt_player1"
    tabOnGUIElement "Time wasting"
    closeDialogByClicking "Conduct warning"
    doSleep 2
    fi

    tabOnGUIElement "btn_score2"
    tabOnGUIElement "btn_score1"

    getCurrentScore
    finishGameFor 1 ${nrOfPointsToWinGame}

    #tap endGameOkPlustimerButton
fi
if doStep "Start timer"; then
    # start timer from popup dialog
    doBack
    #tap timerButton
    #tap announcementButton
    openDialogByClicking "sb_official_announcement"
    closeDialogByClicking "Start"
fi
if doStep "Show game scores"; then
    #longpress gameScores
    doBack

    # toggle full screen
    longpress gamescores
    longpress btn_gameswon1
    longpress btn_gameswon1
    longpress gamescores
    doBack

    # toggle gameswon/game-scores
    doSleep 3
    tabOnGUIElement gamescores
    tabOnGUIElement btn_gameswon1
    doSleep 3
    tabOnGUIElement btn_gameswon1
    tabOnGUIElement gamescores
    #tap actionBarScoreDetails
fi
if doStep "Undo by swipe"; then
    #HswipeScale12
    #doBack
    tabOnGUIElement "btn_score1"
    swipeUndo score_A
    tabOnGUIElement "btn_score2"
    swipeUndo score_B
fi
exit
#sleep 5
tap $gameScores 3 graph.game.3

# pull in all screenshots that exist on the device now
adb pull /sdcard/sb/ .

# rotate them all 90 degrees
for file in $(ls sb.*.png); do
	convert ${file} -rotate "$rotate" tst.png
	mv tst.png ${file}
	#display ${file} &
done

newdir=1
while [ -e $newdir ]; do
	let newdir=newdir+1
done
mkdir $newdir
mv *.png $newdir/.
if which mirage 2> /dev/null; then
	mirage $newdir/*.png &
fi

# back to start situation (assuming back key is configured as 'undo score')
for c in $(seq 1 5); do
	doBack
done
longpress $sB # switch colorscheme for next run
