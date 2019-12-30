#!/usr/bin/env bash

#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
#export JAVA_HOME=/usr/lib/jvm/java-10-openjdk
export JAVA_HOME=/osshare/software/oracle/java-8-oracle


#mykill gradle

mffile=$(grep Manifest build.gradle | grep -v '//' | cut -d "'" -f 2)
pkg=$(grep package= ${mffile} | perl -ne 's~.*"([a-z\.]+)"~$1~; print')

vcFromManifest=$(cat ${mffile} | grep versionCode | sed -e 's~.*versionCode=.\([0-9]*\).*~\1~')
versionCode=$1
if [[ -z "$versionCode" ]]; then
cat <<!

	Please specify versioncode as first argument for building ${pkg}

    Known versioncodes based on git tags:
!
    grepfor=sq
    if [[ "$pkg" = "com.doubleyellow.tabletennis" ]]; then
        grepfor=tt
    fi
	git tag | grep ${grepfor}

    echo "Version code in ${mffile}: $vcFromManifest"
	exit 1
else
    set -x
    relapk=$(find . -name '*-release.apk')
    if [[ -e ${relapk} ]]; then
        changedFiles="$(find . -newer ${relapk} | egrep -v '(intermediates)' | egrep '\.(java|xml)' | grep -v '.idea/')"
    else
        changedFiles="No apk to compare with"
    fi
    set +x

    if [[ ${versionCode} -ne ${vcFromManifest} ]]; then
        sed -i "s~\(versionCode=.\)[0-9]*\(.\)~\1${versionCode}\2~" ${mffile}
    fi
	#head $mffile
	#exit 2 # TMP
fi
todayYYYYMMDD=$(date --date='1 day' +%Y-%m-%d)
if [[ -n "$(grep 'NO_SHOWCASE_FOR_VERSION_BEFORE ='  ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java | grep -v ${todayYYYYMMDD})" ]]; then
    echo "Adapting NO_SHOWCASE_FOR_VERSION_BEFORE to $todayYYYYMMDD in PreferenceValues.java"
    sed -i "s~\(NO_SHOWCASE_FOR_VERSION_BEFORE\s*=\s*.\)[0-9-]*~\1${todayYYYYMMDD}~" ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java
fi

vc=$(grep versionCode ${mffile} | perl -ne 's~[^\d]*(\d+)"~$1~; print')
#brand=$(grep app_name_short_brand_ ${mffile} | head -1 | perl -ne 's~.*_short_brand(\w+).*~$1~; print')
brand=$(egrep 'Brand\s+brand\s*=\s*Brand\.' src/com/doubleyellow/scoreboard/Brand.java | perl -ne 's~.*Brand\.(\w+);.*~$1~; print')

## will be repopulated by ./gradlew
#/bin/rm -rf -v .gradle
## will be repopulated during build
#/bin/rm -rf -v build
#/bin/rm -rfv $HOME/.android/build-cache

targetdir=/osshare/doc/squash/www.double-yellow.be/app
if [[ ! -e ${targetdir} ]]; then
    targetdir=/osshare/code/DoubleYellow/wwwdoubleyellow/app
fi

iStep=${2:-1}
echo "Changed files : $changedFiles"

if [[ -n "${changedFiles}" ]]; then
    echo "*** There were changes. ${changedFiles}"
    echo "*** Rebuilding..."
    if [[ ${iStep} -gt 2 ]]; then
        iStep=1
    fi
else
    echo "*** There were no changes. Not rebuilding..."
    if [[ ${iStep} -eq 1 ]]; then
        iStep=2
    fi
fi

if [[ ${iStep} -le 1 ]]; then
    echo "Cleaning ... $pkg $vc"
    ./gradlew clean

    echo "Building ... $pkg $vc"
    if ./gradlew assemble; then
        relapk=$(find . -name '*-release.apk')
        dbgapk=$(find . -name '*-minified.apk')
        cp -v -p --backup ${relapk} ${targetdir}/Squore${brand}.${vc}.apk
        #read -p "Does copy look ok"
        if [[ -n "${relapk}" ]]; then
            ls -l ${relapk}
            echo "adb install -r Squore/${relapk}"
        fi
        if [[ -n "${dbgapk}" ]]; then
            ls -l ${dbgapk}
            echo "adb install -r Squore/${dbgapk}"
        fi
    else
        echo '#################### Building failed #####################' > /dev/stderr
        exit 1
    fi
fi
#set -x
if [[ ${iStep} -le 2 ]]; then
    devices="$(adb devices | egrep -v '(List of|^$)' | sed 's~ *device~~')"
    for dvc in ${devices}; do
        #set -x
        echo 'Installing new version ...'
        adb -s ${dvc} install -r ${targetdir}/Squore${brand}.${vc}.apk 2> tmp.adb.install
        if grep failed tmp.adb.install; then
            echo "Uninstalling previous version to install new version ..."
            # uninstall previous app
            adb -s ${dvc} uninstall ${pkg}

            echo 'Installing new version (after uninstall) ...'
            adb -s ${dvc} install -r ${targetdir}/Squore${brand}.${vc}.apk 2> tmp.adb.install
        fi

        # launch the app
        echo 'Launching the app ${pkg} ...'
        adb -s ${dvc} shell monkey -p ${pkg} -c android.intent.category.LAUNCHER 1
        echo "adb -s ${dvc} logcat | egrep '(SB|doubleyellow)' | egrep -v '(AutoResize)'"
    done

    if [[ -z "${devices}" ]]; then
        echo "############### No devices found to install the app..."
    fi

    #rm tmp.adb.install
fi
# install a shortcut
#    pkg=com.doubleyellow.scoreboard
#
#    adb -d shell am broadcast \
#    -a com.android.launcher.action.INSTALL_SHORTCUT \
#    --es Intent.EXTRA_SHORTCUT_NAME "Squore" \
#    --esn Intent.EXTRA_SHORTCUT_ICON_RESOURCE \
#    $pkg/.activity
#
