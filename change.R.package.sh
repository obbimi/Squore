#!/usr/bin/env bash

# derive allowed brands from existing Manifest files
allowedInput=$(ls -1 AndroidManifest*.xml | grep -v ALL | perl -ne 's~AndroidManifest(.+).xml~$1~; print' | sort )

function showHelp {
cat <<!

Usage:

	$0 '($( echo ${allowedInput} | tr ' \n' '||'))'

Always change back to Squore (the default) before change to another 'Brand'

!
}
tobranded=${1:-Squore}
if [[ "${tobranded}" = "-h" ]]; then
	showHelp
	exit
fi
if [[ ! -e AndroidManifest${tobranded}.xml ]]; then
    echo "\
ERROR : Unknown brand ${tobranded}

Allowed are: ${allowedInput}
" > /dev/stderr
	exit
fi

function correctSportSpecificResource {
    from=$1
    to=$2

    let correctedCnt=0
    let keptCnt=0
    # list all renamed USED resource names and ...
    for res in $(egrep -r '@(array|bool|fraction|integer|integer-array|string|string-array).*__[A-Z][A-Za-z]+' * | perl -ne 's~.*@(array|bool|fraction|integer|integer-array|string|string-array)\/(\w+).*~$2~; print' | sort -u); do
        # ... check the appropriate res definition exists
        if egrep -q -r "name=.$res." *; then
            if echo ${res} | grep "__${from}"; then
                let keptCnt=keptCnt+1
            fi
        else
            #echo "XXXXXXXXXXXXXXXXXX Is NOT available : ${res}"

            if [[ -n "${2}" ]]; then
                newRes=$(echo "${res}" | sed -e "s~__${from}~__${to}~")
                for f in $(grep -rl "$res" *); do
                    printf "Correcting %-72s from %-32s to %-32s \n" ${f} ${res} ${newRes}
                    sed -i "s~${res}~${newRes}~" ${f}
                    let correctedCnt=correctedCnt+1
                done
            fi
        fi
    done

    if [[ ${correctedCnt} -gt 0 ]]; then
        echo "=================================================="
        echo "CORRECTED ${correctedCnt} resources from ${from} to ${to}"
        echo "KEPT ${keptCnt} resources for ${from}"
        echo "=================================================="
    fi
}

cd src

####################################################
# Change Brand.java
####################################################

# check it exists
if ! egrep -q "${tobranded}.*SportType" com/doubleyellow/scoreboard/Brand.java; then
    echo "ERROR: Brand ${tobranded} not found in Brand.java" > /dev/stderr

    egrep -l "${tobranded}.*SportType" ../res/values/*.xml

    exit 1
fi

if [[ "$tobranded" = "Squore" ]]; then
    # comment out all brands ...
    sed -i 's~^\(\s*\)\(\w\+\s*(\s*SportType\.\)~\1//\2~'         com/doubleyellow/scoreboard/Brand.java
    # ... and uncomment Squore (always required)
    sed -i "s~^\(\s\+\)//\(Squore\s*(.*R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
fi
# ensure the chosen brand is uncommented in the Brands.java file (Squore brand should ALWAYS be uncommented)
if [[ "$tobranded" != "Squore" ]]; then
    sed -i "s~^\(\s\+\)//\(${tobranded}\s*(.*R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
fi

# simply actually set the active brand like Brand.brand = Brand.Squore
sed -i "s~Brand.\w\+;~Brand.${tobranded};~"                      com/doubleyellow/scoreboard/Brand.java
#vi +/Brand.                                                     com/doubleyellow/scoreboard/Brand.java



####################################################
# Change <other>.java
####################################################

echo '=================================='
printf "Change to '${tobranded}'\n"
tobrandedLC=$(echo ${tobranded} | tr 'ABCEDFGHIJKLMNOPQRSTUVWXYZ' 'abcedfghijklmnopqrstuvwxyz')
if [[ "$tobranded" = "Squore" ]]; then
    tobrandedLC='scoreboard'
fi
for f in $(egrep -irl 'com\.doubleyellow\.[a-z]+\.R[^a-z]' *); do
    cat ${f} | perl -ne "s~com\.doubleyellow\.(?!base.R)[a-z]+\.R([^A-Za-z'])~com.doubleyellow.${tobrandedLC}.R\$1~; print" > ${f}.1.txt
    if [[ -n "$(diff ${f} ${f}.1.txt)" ]]; then
        printf "File %-72s to %s \n" $f $tobranded
        mv ${f}.1.txt ${f} #.2.txt
    else
        rm ${f}.1.txt
    fi
    #sed -i "s~com\.doubleyellow\.[a-z]*\.R\([^A-Za-z]\)~com.doubleyellow.${tobrandedLC}.R\1~" ${f}
    #exit 1
done
echo '=================================='
read -p ' Java files changed. Continue ... ? '

if [[ "$tobranded" = "CourtCare" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtcaresquore.R\1~' ${f}
        #exit 1
    done
elif [[ "$tobranded" = "UniOfNotthingham" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtscore_uon.R\1~' ${f}
        #exit 1
    done
fi

# change some defaults in xml files (in java is/should be taken care of by means of code)
cd ../res

####################################################
# Change menu/layout/prefences xml files
####################################################

echo '=================================='
fromSuffix=Squash
toSuffix=${tobranded}
if [[ "$tobranded" = "Squore" ]] ; then
    fromSuffix="[A-Z][a-z][A-Za-z]+"
    toSuffix=Squash
fi
for f in $(egrep -rl "@(string|fraction).*__${fromSuffix}\""); do

    cat ${f} | perl -ne "s~__${fromSuffix}\"~__${toSuffix}\"~; print" > ${f}.1.xml
    if [[ -n "$(diff ${f} ${f}.1.xml)" ]]; then
        printf "File %-30s to %s strings\n" $f $tobranded
        #meld         ${f} ${f}.1.xml
        #rm -v ${f}.1.xml # TEMP
        mv ${f}.1.xml ${f}
    else
        rm ${f}.1.xml
    fi
    #read -p ' Continue ? '
done
echo '=================================='
read -p 'menu/layout/preferences files processed. Continue ? '

if [[ "$tobranded" = "Padel" ]]; then
    correctSportSpecificResource Padel TennisPadel
    correctSportSpecificResource       TennisPadel Empty
fi

correctSportSpecificResource ${tobranded} Empty

####################################################
# Change build.gradle
####################################################

cd ..
# comment out all Manifest file lines
sed -i 's~^\(\s\+\)srcFile~\1//srcFile~' build.gradle
# uncomment ManifestALL file lines
sed -i "s~^\(\s\+\)//\(srcFile\s\+'AndroidManifestALL\)~\1\2~" build.gradle
# uncomment the one Manifest file we are interested in
sed -i "s~^\(\s\+\)//\(srcFile\s\+'AndroidManifest${tobranded}\.\)~\1\2~" build.gradle
#vi +/Manifest${tobranded} build.gradle
