#!/usr/bin/env bash

#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
#export JAVA_HOME=/usr/lib/jvm/java-10-openjdk
export JAVA_HOME=/osshare/software/oracle/java-8-oracle


#mykill gradle
#set -x
mffile=$(grep Manifest build.gradle | egrep -v '(ALL)' | grep -v '//' | cut -d "'" -f 2)
if [[ -z "${mffile}" ]]; then
    echo "Could not determine manifest file by looking in build.gradle"
    exit 1
fi
pkg=$(grep package= ${mffile} | perl -ne 's~.*"([a-z\.]+)"~$1~; print')

#vcFromManifest=$(cat ${mffile} | grep versionCode | sed -e 's~.*versionCode=.\([0-9]*\).*~\1~')
versionCodeX="X"
if [[ -z "$versionCodeX" ]]; then
cat <<!

	Please specify versioncode as first argument for building ${pkg}

    Known versioncodes based on git tags:
!
    grepfor=sq
    if [[ "$pkg" = "com.doubleyellow.tabletennis" ]]; then
        grepfor=tt
    fi
	git tag | grep ${grepfor}

    #echo "Version code in ${mffile}: $vcFromManifest"
	exit 1
else
    productFlavor="phoneTabletPost23"
    relapk=$(find . -name "*${productFlavor}-release.apk")
    if [[ -e ${relapk} ]]; then
        changedFiles="$(find . -newer ${relapk} | egrep -v '(intermediates)' | egrep '\.(java|xml|md|gradle)' | egrep -v '(\.idea/|/\.gradle/|/generated/|/reports/)')"
    else
        changedFiles="No apk to compare with"
    fi

    if [[ ${versionCodeX} -ne ${vcFromManifest} ]]; then
        echo "Not changing MANIFEST anymore" > /dev/null
        #sed -i "s~\(versionCode=.\)[0-9]*\(.\)~\1${versionCodeX}\2~" ${mffile}
    fi
	#head $mffile
	#exit 2 # TMP
fi
todayYYYYMMDD=$(date --date='1 day' +%Y-%m-%d)
if [[ -n "$(grep 'NO_SHOWCASE_FOR_VERSION_BEFORE ='  ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java | grep -v ${todayYYYYMMDD})" ]]; then
    echo "Adapting NO_SHOWCASE_FOR_VERSION_BEFORE to $todayYYYYMMDD in PreferenceValues.java"
    sed -i "s~\(NO_SHOWCASE_FOR_VERSION_BEFORE\s*=\s*.\)[0-9-]*~\1${todayYYYYMMDD}~" ./src/com/doubleyellow/scoreboard/prefs/PreferenceValues.java
fi

#vc=$(grep versionCode ${mffile} | perl -ne 's~[^\d]*(\d+)"~$1~; print')
#vc=$(grep versionCode build.gradle | perl -ne 's~.*"\d{4}(\d+).*~$1~; print' | sort -u)

#brand=$(grep app_name_short_brand_ ${mffile} | head -1 | perl -ne 's~.*_short_brand(\w+).*~$1~; print')
brand=$(egrep 'Brand\s+brand\s*=\s*Brand\.' src/com/doubleyellow/scoreboard/Brand.java | perl -ne 's~.*Brand\.(\w+);.*~$1~; print')

## will be repopulated by ./gradlew
#/bin/rm -rf -v .gradle
## will be repopulated during build
#/bin/rm -rf -v build
#/bin/rm -rfv $HOME/.android/build-cache

if [[ 1 -eq 2 ]]; then
    echo "TEMP EXIT"
    exit 1
fi

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
    echo "Cleaning ... $pkg"
    ./gradlew clean

    echo "Building ... $pkg"
    if ./gradlew assemble; then
        productFlavors="phoneTabletPre22 phoneTabletPost23 wearOs"
        for productFlavor in ${productFlavors}; do

            relapk=$(find . -name "*-${productFlavor}-release.apk")
            dbgapk=$(find . -name "*-${productFlavor}-debug.apk")

            # determine correct version number from manifest
            mergedManifest=$(find build/intermediates/merged_manifests/${productFlavor}Release -name AndroidManifest.xml)
            versionCode=$(head ${mergedManifest} | grep versionCode | sed -e 's~[^0-9]~~g')

            cp -v -p --backup ${relapk} ${targetdir}/Score-${brand}.${productFlavor}-${versionCode}.apk
            #read -p "Does copy look ok"
            if [[ 1 -eq 2 ]]; then
                if [[ -n "${relapk}" ]]; then
                    ls -l ${relapk}
                    echo "adb -s \${device} install -r Squore/${relapk}"
                fi
                if [[ -n "${dbgapk}" ]]; then
                    ls -l ${dbgapk}
                    echo "adb -s \${device} install -r Squore/${dbgapk}"
                fi
            fi

        done
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
        build_version_sdk=$(adb -s ${dvc} shell getprop ro.build.version.sdk | sed -e 's~[^0-9]~~')
        build_product_model=$(adb -s ${dvc} shell getprop ro.product.model)

        productFlavor="phoneTabletPost23"
        if [[ ${build_version_sdk} -lt 23 ]]; then
            productFlavor="phoneTabletPre22"
        fi
        if [[ "${build_product_model}" =~ "wear" ]]; then
            productFlavor="wearOs"
        fi
        mergedManifest=$(find build/intermediates/merged_manifests/${productFlavor}Release -name AndroidManifest.xml)
        versionCode=$(head ${mergedManifest} | grep versionCode | sed -e 's~[^0-9]~~g')

        set +x
        echo "Installing new ${productFlavor} version on device ${dvc}..."
        adb -s ${dvc} install -r ${targetdir}/Score-${brand}.${productFlavor}-${versionCode}.apk 2> tmp.adb.install # 1> /dev/null
        if grep failed tmp.adb.install; then
            echo "Uninstalling previous version to install new version ..."
            # uninstall previous app
            adb -s ${dvc} uninstall ${pkg}

            echo 'Installing new version (after uninstall) ...'
            adb -s ${dvc} install -r ${targetdir}/Squore${brand}.${versionCode}.apk 2> tmp.adb.install
        fi

        # launch the app
        echo "Launching the app ${pkg} ..."
        #set -x
        adb -s ${dvc} shell monkey -p ${pkg} -c android.intent.category.LAUNCHER 1 > /dev/null 2> /dev/null
        set +x
        # adb -s ${dvc} logcat
        echo "adb -s ${dvc} logcat | egrep '(SB|doubleyellow)' | egrep -v '(AutoResize)'"
    done

    if [[ -z "${devices}" ]]; then
        echo "############### No devices found to install the app..."
    fi

    rm tmp.adb.install
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
