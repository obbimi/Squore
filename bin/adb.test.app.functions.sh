#!/usr/bin/env sh

function _getMiddleOf() {
    iLow=$(echo "$1"  | cut -d ' ' -f $3)
    iHigh=$(echo "$2" | cut -d ' ' -f $3)
    iMiddle=$(( ($iLow+$iHigh)/2 ))
    echo $iMiddle
}
function hideOnScreenKeyboard() {
    # TODO: on api 22 i have seen this close the match dialog if just opened and now keyboard is shown
    doSleep 1
    keyBoard=$(adbshell dumpsys input_method | grep 'mInputShown=true')
    while [[ -n "${keyBoard}" ]]; do
        echo "On screen keyboard detected... closing (${keyBoard})"
        adbshell input keyevent 111
        #typeEscape
        doSleep 1
        keyBoard=$(adbshell dumpsys input_method | grep 'mInputShown=true')
    done
}
function getCurrentScore() {
    _getTopLeftXY "btn_score2"
     egrep "btn_score[1-2]" ${xmldump_file_work} | grep "text" | sed 's~.*text="~~' | sed 's~".*~~'
}

function adbshell() {
    #set -x
    cmd="$*"
    if [[ -n "${ADB_DEVICE}" ]]; then
        # working with a device that has no grep or sed installed
	    #echo ${cmd}
	    adb -s ${ADB_DEVICE} shell "${cmd}"
	else
	    ${cmd}
	fi
	#set +x
}

# generic detection of dimension of device or emulator
export W=0; # $(adbshell dumpsys display | egrep 'mDisplayWidth'  | sed 's~^.*=~~')
export H=0; # $(adbshell dumpsys display | egrep 'mDisplayHeight' | sed 's~^.*=~~')
if [[ $W -eq 0 ]]; then
    dims=$(adbshell dumpsys display | egrep mOverrideDisplayInfo | sed 's~.*, \(app.*\), largest.*~\1~')
    echo $dims
    W=$(echo $dims | sed 's~.*app \([0-9]*\) x \([0-9]*\).*~\1~')
    H=$(echo $dims | sed 's~.*app \([0-9]*\) x \([0-9]*\).*~\2~')
    echo "W x H = $W x $H"
else
    # for device without hardware buttons
    W=$(($W/16*15))
fi
if [[ $H -gt $W ]]; then
    T=$W
    W=$H
    H=$T
fi
if [[ $W -eq 0 ]]; then
    echo "Could not determine width/height"
fi

#set -x
if [[ -n "$W" ]]; then
    centerScreen="$(($W/2)) $(($H/2))"
fi

function isNumber() {
    if let a=0+$1 2> /dev/null; then
        return 0
    else
        return 1
    fi
}
function doSleep () {
    sleepAmount=${1:-2}
    echo "Sleeping ${sleepAmount} ..."
    sleep ${sleepAmount}
}

pointsA=0
pointsB=0
ssprefix=1
function screenshot() {
	adbshell /system/bin/screencap -p /sdcard/$(printf '%02d.%s.png' ${ssprefix} $1)
    let ssprefix=ssprefix+1
}
function doBack() {
	adbshell input keyevent 4
	doSleep ${1:-1}
}
function typeText () {
    echo "Typing ${1}"
    adbshell input text "${1}"
}
function typeEscape () {
    echo "Pressing escape to close some popup/keyboard..."
    adbshell input keyevent 111
}
# KEYCODE_MEDIA_NEXT 87
# KEYCODE_NAVIGATE_NEXT 261
# KEYCODE_ESCAPE 111
function typeNext() {
    # next field
    if [[ 1 -eq 1 ]]; then
        adbshell input keyevent 66 # 66=enter
    else
        # not easy, might be very small button
        tap $(($W/100*99)) $(($H/100*99)) # assuming 'next' button on keyboard in is right bottom
    fi
    doSleep 1
}
function typeDone() {
    # next field
    tap $(($W/100*99)) $(($H/100*99)) # assuming 'done' button on keyboard in is right bottom
    doSleep 1
}
function swipeUndo () {
    if [[ "$1" = "score_A" ]]; then
        HswipeScale12 1 4
    fi
    if [[ "$1" = "score_B" ]]; then
        HswipeScale12 11 8
    fi
}
function HswipeScale12 () {
    wStart=$(($W/12*${1:-3}))
    wEnd=$(($W/12*${2:-7}))
    hStartEnd=$(( $H / 12 * ${3:-6} ))
    adbshell input touchscreen swipe $wStart $hStartEnd $wEnd $hStartEnd 200
    doSleep 1
}
function VswipeScale12 () {
    hStart=$(($H/12*${1:-3}))
    hEnd=$(($H/12*${2:-7}))
    wStartEnd=$(( $W / 12 * ${3:-6} ))
    adbshell input touchscreen swipe $wStartEnd $hStart $wStartEnd $hEnd 200
    doSleep 1
}
function showMenuDrawer () {
    adbshell input touchscreen swipe $(($W/100)) $(($H/2)) $(($W/4)) $(($H/2)) 200
    doSleep 1
    #set -x

    #totalNrOfMenuItems=12
    #menuItem=${1:-0}
    #if [[ ${menuItem} -lt 0 ]]; then
    #    # we want a menu item counting from the bottom
    #    let menuItem=totalNrOfMenuItems+menuItem
    #    VswipeScale12 7 3 2
    #    VswipeScale12 11 2 2
    #fi
    #echo "Menu item ${menuItem}"
    #if [[ ${menuItem} -gt 0 ]]; then
    #    tap $(($W/6)) $(($H/$totalNrOfMenuItems*${menuItem} + $HSB)) "Menu ${menuItem} of ${totalNrOfMenuItems}"
    #    doSleep 2
    #fi
    #set +x
}

export xmldump_file_default=/sdcard/window_dump.xml
# must point to a dir that also exist on the device
export xmldump_file_work=/sdcard/windowDump.xml

function removeDumpFile() {
    rm ${xmldump_file_work}
}

function _getTopLeftXY() {
    resourceId="$1"
    refreshWindowDump=${2:-0}

    if [[ ! -e ${xmldump_file_work} ]]; then
        refreshWindowDump=1
    fi
    # to call this command only when layout changes (when activity changes)
    if [[ ${refreshWindowDump} -gt 0 ]]; then
        if adbshell uiautomator dump > /dev/null; then
            if [[ -n "${ADB_DEVICE}" ]]; then
                localDir=$(dirname ${xmldump_file_work})
                if [[ ! -e ${localDir} ]]; then
                    echo "Need to create writable local directory : $localDir"
                    echo "sudo mkdir /sdcard; sudo chown $USER:$USER /sdcard"
                    doSleep 5
                    exit
                fi
                #echo "pulling in file from device ${ADB_DEVICE} to grep from "
                adb -s ${ADB_DEVICE} pull ${xmldump_file_default} ${xmldump_file_work} > /dev/null
            else
                echo "cp file to work with on ${ADB_DEVICE}"
                set -x
                cp -v ${xmldump_file_default} ${xmldump_file_work}
                set +x
            fi
            # insert few line breaks for easy grepping
            sed -i 's~>\s*<~>\n<~g' ${xmldump_file_work}
        else
            echo "making new dump of gui layout failed" # most like on board timers are showing... GUI must be absolutely static
            doSleep 5
            exit
        fi
    fi

    topleftXY=$(egrep     "(${resourceId})" ${xmldump_file_work} | sed 's~.*bounds="\[~~'       | sed 's~\]\[.*~~' | tr ',' ' ')
    bottomRightXY=$(egrep "(${resourceId})" ${xmldump_file_work} | sed 's~.*bounds="\[.*\]\[~~' | sed 's~\].*~~'   | tr ',' ' ')
    if [[ -z "${topleftXY}" ]]; then
        # display all ids
        echo "*** Did not find ${resourceId}. Available are: "
        cat ${xmldump_file_work} | tr '>' '\n' | grep scoreboard.id | sed 's~.*scoreboard.id/\([^"]*\).*~\1~' | sort
        doSleep 5
        exit
    fi
    tmpX=$(_getMiddleOf "${topleftXY}" "${bottomRightXY}" 1)
    tmpY=$(_getMiddleOf "${topleftXY}" "${bottomRightXY}" 2)
    middleXY="${tmpX} ${tmpY}"

    export topleftXY
    export bottomRightXY
    export middleXY
}
function openDialogByClicking() {
    tabOnGUIElement "$1"
    removeDumpFile
}
function closeDialogByClicking() {
    tabOnGUIElement "$1"
    removeDumpFile
}
function closeDialogByEscape() {
    typeEscape
    removeDumpFile
}


function longpress() {
    _getTopLeftXY "$1" $2

    # last number is duration in ms
    echo "input touchscreen 2000 on middleXY ${middleXY} to simulate long click on ${1}"
    adbshell input touchscreen swipe ${middleXY} ${middleXY} 2000
    #echo "input touchscreen 2000 on topleftXY ${topleftXY} to simulate long click on ${1}"
    #adbshell input touchscreen swipe ${topleftXY} ${topleftXY} 2000
    #echo "input touchscreen 2000 on bottomRightXY ${bottomRightXY} to simulate long click on ${1}"
    #adbshell input touchscreen swipe ${bottomRightXY} ${bottomRightXY} 2000

    removeDumpFile # assume dialog will appear

    # allow dialog to show
    doSleep 1
}
function tapCenterScreen() {
    adbshell input tap ${centerScreen}
}
function tabOnGUIElement() {
    _getTopLeftXY "$1" $2

    echo "input tap on topleftXY ${topleftXY} to simulate click on ${1}"
    adbshell input tap ${topleftXY}
    #echo "input tap on bottomRightXY ${bottomRightXY} to simulate click on ${1}"
    #adbshell input tap ${bottomRightXY}
}
