#!/usr/bin/env bash

#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
#export JAVA_HOME=/usr/lib/jvm/java-10-openjdk
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
#export JAVA_HOME=/osshare/software/oracle/java-8-oracle
if [[ ! -e $JAVA_HOME ]]; then
    export JAVA_HOME=/cygdrive/c/localapps/jdk1.8.0_231
fi
if [[ ! -e $JAVA_HOME ]]; then
    # ubuntu shell
    #export JAVA_HOME=/mnt/c/localapps/jdk1.8.0_231
    #export JAVA_HOME=C:/localapps/jdk1.8.0_231
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
fi
if [[ ! -e $JAVA_HOME ]]; then
    echo "java home not correctly configured: $JAVA_HOME"
    exit 1
fi

mffile=$(grep Manifest build.gradle | grep -E -v '(ALL)' | grep -v '//' | cut -d "'" -f 2)
if [[ -z "${mffile}" ]]; then
    echo "Could not determine manifest file by looking in build.gradle"
    exit 1
fi
pkg=$(grep package= ${mffile} | perl -ne 's~.*"([a-z\.]+)".*~$1~; print')

# check if a new version code for the brand for which we will be creating an apk is specified
if [[ -n "$1" ]]; then
    versionCodeFromCommandLine=$1
    echo "Changing build.gradle to use versioncode $versionCodeFromCommandLine"
    #grep versionCode build.gradle | sed -i "s/10000 + [0-9][0-9][0-9]/10000 + ${versionCodeFromCommandLine}/"
    sed -i "s/10000 + [0-9][0-9]*/10000 + ${versionCodeFromCommandLine}/" build.gradle
fi

brand=$(grep -E 'Brand\s+brand\s*=\s*Brand\.' src/com/doubleyellow/scoreboard/Brand.java | perl -ne 's~.*Brand\.(\w+);.*~$1~; print')
echo "Manifest file : ${mffile}"
echo "Package       : ${pkg}"
echo "Brand         : ${brand}"
hasNewVersionCode=$(git diff build.gradle | grep -E '^\+' | grep versionCode | sed 's~.*0000\s*+\s*~~' | sort | tail -1) # holds only the last 3 digits
echo "hasNewVersionCode: $hasNewVersionCode"
BU_DIR=/osshare/code/gitlab/double-yellow.be/app
if [[ ! -e ${BU_DIR} ]]; then
	  BU_DIR=/mnt/c/code/gitlab/double-yellow.be/app
fi

productFlavor="phoneTabletPost23"
relapk=$(find . -name "*${productFlavor}-Xrelease.apk")
if [[ ! -e ${relapk} ]]; then
    relapk=$(find ${BU_DIR} -name "Score-${brand}.${productFlavor}*${hasNewVersionCode}.apk")
fi

if [[ -e ${relapk} ]]; then
    apkFileTime=$(find ${relapk} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")

    echo "Comparing changetime of files against ${relapk} (${apkFileTime})"
    changedFiles="$(find . -type f -newer ${relapk} | grep -E -v '(intermediates)' | grep -E '\.(java|xml|md|gradle)' | grep -E -v '(\.idea/|/\.gradle/|/generated/|/reports/)')"
else
    changedFiles="No release apk to compare with"
fi

# make a small modification in preferences date value so 'Quick Intro' will not play for 1 or 2 days (to allow better PlayStore automated testing)
#if grep -q ${mffile} .gitignore; then
if [[ -z "${hasNewVersionCode}" ]]; then
    echo "Not modifying PreferenceValues.java for build ${mffile}"
else
    todayYYYYMMDD=$(date --date='1 day' +%Y-%m-%d)
    if [[ -n "$(grep 'NO_SHOWCASE_FOR_VERSION_BEFORE ='  ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java | grep -v ${todayYYYYMMDD})" ]]; then
        echo "Adapting NO_SHOWCASE_FOR_VERSION_BEFORE to $todayYYYYMMDD in PreferenceValues.java"
        sed -i "s~\(NO_SHOWCASE_FOR_VERSION_BEFORE\s*=\s*.\)[0-9-]*~\1${todayYYYYMMDD}~" ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java
        #echo "TEMPORARY NOT CHANGING DATE TO TEST CASTING AND CHRONOS"
    fi
fi

if [[ -z "${hasNewVersionCode}" ]]; then
    if [[ -n "$(grep versionCode build.gradle)" ]]; then
        echo "Specify new version code for ${brand}"
        read -p "Open build.gradle [Y/n] ?" ANWSER
        if [[ "${ANWSER:-y}" = "y" ]]; then
            vi +/versionCode build.gradle
            exit 1
        fi
    else
        read -t 10 -p "Warning : continue without changing version code for ${brand} ?"
    fi
fi

## will be repopulated by ./gradlew
#/bin/rm -rf -v .gradle
## will be repopulated during build
#/bin/rm -rf -v build
#/bin/rm -rfv $HOME/.android/build-cache

if [[ 1 -eq 2 ]]; then
    echo "TEMP EXIT"
    exit 1
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

ADB_COMMAND=$(which adb || which adb.exe)
if [[ ${iStep} -le 1 ]]; then
    echo "Cleaning ... ${pkg}"
    ./gradlew clean

    echo "Building ... ${pkg}"
    if ./gradlew assemble; then
        productFlavors="phoneTabletPre22 phoneTabletPost23 wearOs"
        for productFlavor in ${productFlavors}; do

            relapk=$(find . -name "*-${productFlavor}-release.apk")
            dbgapk=$(find . -name "*-${productFlavor}-debug.apk")

            # determine correct version number from manifest
            if [[ -e build/intermediates/merged_manifests/${productFlavor}Release ]]; then
                mergedManifest=$(find build/intermediates/merged_manifests/${productFlavor}Release -name AndroidManifest.xml)
                echo "Merged manifest: ${mergedManifest}"
                versionCode=$(head ${mergedManifest} | grep versionCode | sed -e 's~[^0-9]~~g')
                echo "Merged manifest versionCode: ${versionCode}"

                if [[ -e ${BU_DIR} ]]; then
                    if [[ -e ${relapk} ]]; then
                        cp -v -p --backup ${relapk} ${BU_DIR}/Score-${brand}.${productFlavor}-${versionCode}.apk
                    else
                        echo "No release file. Maybe because no signingconfig in build.gradle ?!"
                        cp -v -p --backup ${dbgapk} ${BU_DIR}/Score-${brand}.${productFlavor}-${versionCode}.DEBUG_NO_RELEASE.apk
                    fi
                else
                    echo "NOT making backup in non existing directory ${BU_DIR}"
                fi
            else
                echo "WARN: could not find build/intermediates/merged_manifests/${productFlavor}Release"
            fi

            #read -p "Does copy look ok"
            if [[ 1 -eq 2 ]]; then
                if [[ -n "${relapk}" ]]; then
                    ls -l ${relapk}
                    echo "${ADB_COMMAND} -s \${device} install -r Squore/${relapk}"
                fi
                if [[ -n "${dbgapk}" ]]; then
                    ls -l ${dbgapk}
                    echo "${ADB_COMMAND} -s \${device} install -r Squore/${dbgapk}"
                fi
            fi

        done
    else
        echo '#################### Building failed #####################' > /dev/stderr
        exit 1
    fi
fi
if [[ ${iStep} -le 2 ]]; then
    devices="$(${ADB_COMMAND} devices | grep -E -v '(List of|^$)' | sed 's~ *device~~')"
    echo "Devices: ${devices}"
    for dvc in ${devices}; do
        if [[ -z "${dvc}" ]]; then
            continue 
        fi
        build_version_sdk=$(    ${ADB_COMMAND} -s ${dvc} shell getprop ro.build.version.sdk | sed -e 's~[^0-9]~~')
        build_product_model=$(  ${ADB_COMMAND} -s ${dvc} shell getprop ro.product.model)
        build_characteristics=$(${ADB_COMMAND} -s ${dvc} shell getprop ro.build.characteristics) # "emulator,nosdcard,watch",default
        echo "Device ${dvc} : Version= ${build_version_sdk}, Model= ${build_product_model}, Characteristicts= ${build_characteristics}"
        productFlavor="phoneTabletPost23"
        if [[ ${build_version_sdk} -lt 23 ]]; then
            productFlavor="phoneTabletPre22"
        fi
        #if [[ "${build_product_model}" =~ "wear" ]]; then
        #    productFlavor="wearOs"
        #fi
        if [[ "${build_characteristics}" =~ "watch" ]]; then
            productFlavor="wearOs"
        fi
        #mergedManifest=$(find build/intermediates/merged_manifests/${productFlavor}Release -name AndroidManifest.xml)
        #versionCode=$(head ${mergedManifest} | grep versionCode | sed -e 's~[^0-9]~~g')

        #apkFile=${BU_DIR}/Score-${brand}.${productFlavor}-${versionCode}.apk
        apkFile=${BU_DIR}/Score-${brand}.${productFlavor}*${hasNewVersionCode}.apk
        echo "Installing new ${productFlavor} version ${hasNewVersionCode} on device ${dvc}... (${apkFile})"

#echo "[TMP] Uninstalling previous version of ${pkg} ..."
#adb -s ${dvc} uninstall ${pkg}
        apkFile=$(echo ${apkFile} | sed 's~/mnt/c~c:~') # in ubuntu shell the apk should be passed without the /mnt/c path
        ${ADB_COMMAND} -s ${dvc} install -r ${apkFile} 2> tmp.adb.install # 1> /dev/null
        if grep failed tmp.adb.install; then
            echo "Uninstalling previous version of ${pkg} to install new version ..."
            # uninstall previous app
            ${ADB_COMMAND} -s ${dvc} uninstall ${pkg}

            echo "Installing new version ${hasNewVersionCode} (after uninstall) ..."
            ${ADB_COMMAND} -s ${dvc} install -r ${apkFile} 2> tmp.adb.install
        fi

        # launch the app
        echo "Launching the app ${pkg} ..."
        ${ADB_COMMAND} -s ${dvc} shell monkey -p ${pkg} -c android.intent.category.LAUNCHER 1 > /dev/null 2> /dev/null
        set +x
        # adb -s ${dvc} logcat
        echo "${ADB_COMMAND} -s ${dvc} logcat | grep -E '(SB|doubleyellow)' | grep -E -v '(AutoResize)'"
    done

    if [[ -z "${devices}" ]]; then
        echo "############### No devices found to install the app..."
    fi

    rm tmp.adb.install 2> /dev/null
fi
# install a shortcut
#    pkg=com.doubleyellow.scoreboard
#
#    adb -d shell am broadcast \
#    -a com.android.launcher.action.INSTALL_SHORTCUT \
#    --es Intent.EXTRA_SHORTCUT_NAME "Squore" \
#    --esn Intent.EXTRA_SHORTCUT_ICON_RESOURCE \
#    ${pkg}/.activity
#
