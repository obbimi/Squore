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
parentBrand="${2}"
if [[ "${tobranded}" = "-h" ]]; then
	showHelp
	exit
fi

brandMfFile=AndroidManifest${tobranded}.xml
if [[ ! -e ${brandMfFile} ]]; then
    echo "\
ERROR : Unknown brand ${tobranded}

Allowed are: ${allowedInput}
" > /dev/stderr
	exit
else
    brandPkg=$(grep package= ${brandMfFile} | perl -ne 's~.*"([a-z\.]+)"~$1~; print')
    read -t 2 -p "New package ${brandPkg}"
fi

if [[ -z "${parentBrand}" ]]; then
    parentBrand="$(cat ${brandMfFile} | grep parentBrand | sed -e 's~parentBrand\s*=\s*~~' | tr -d ' ')"
    read -t 1 -p "Backup brand from ${brandMfFile} ==> '${parentBrand}'. Continuing...."
    echo
fi

function correctSportSpecificResource {
    from=$1
    to=$2

    read -t 1 -p "Correcting from ${from} to ${to}. Continue ?"
    echo

    echo -n > notcorrected.txt
    echo -n > corrected.txt

    let correctedCnt=0
    let keptCnt=0
    # list all renamed USED resource names and ...
    for res in $(egrep -r '@(array|bool|fraction|integer|integer-array|string|string-array).*__[A-Z][A-Za-z]+' * | perl -ne 's~.*@(array|bool|fraction|integer|integer-array|string|string-array)\/(\w+__\w+).*~$2~; print' | sort -u); do
        # ... check that the appropriate res definition exists
        if egrep -q -r "name=.${res}." *; then
            if echo ${res} | grep -q "__${from}"; then
                nrOfOccurrencesAsExpr=$(egrep -h -c -r "@.*${res}" * | egrep -v '^0$' | xargs | sed -e 's/\ /+/g')
                nrOfOccurrences=$((${nrOfOccurrencesAsExpr}))
                #read -t 3 -p "Nr of occurrences of valid ${res} : ${nrOfOccurrences}"
                let keptCnt=keptCnt+nrOfOccurrences
                printf "%2d (+%d) %-72s OK \n" ${keptCnt} ${nrOfOccurrences} ${res} >> notcorrected.txt
            fi
        else
            #echo "XXXXXXXXXXXXXXXXXX Is NOT available : ${res}"

            if [[ -n "${2}" ]]; then
                newRes=$(echo "${res}" | sed -e "s~__${from}~__${to}~")
                for f in $(grep -rl "$res" *); do
                    nrToCorrect=$(grep -c ${res} ${f})
                    #if [[ ${nrToCorrect} -gt 1 ]]; then
                    #    echo "================>>>>>>>>>>>>>>>> ${nrToCorrect} corrections for ${res} to ${newRes} in ${f}"
                    #fi

                    sed -i "s~${res}~${newRes}~" ${f}
                    let correctedCnt=correctedCnt+nrToCorrect

                    printf "%2d (+%d) Corrected %-72s from %-32s to %-32s \n" ${correctedCnt} ${nrToCorrect} ${f} ${res} ${newRes} >> corrected.txt
                done
            fi
        fi
    done

    cat notcorrected.txt
    cat corrected.txt

    if [[ ${correctedCnt} -gt 0 ]]; then
        (
        echo "=================================================="
        echo "CORRECTED ${correctedCnt} resources from ${from} to ${to}"
        echo "KEPT ${keptCnt} resources for ${from}"
        echo "=================================================="
        ) | tee -a correctSportSpecificResource.txt
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

# derive current package by looking for .R reference in Brand.java
pkgFrom=$(egrep 'import[ ]+.*\.R;' com/doubleyellow/scoreboard/main/ScoreBoard.java | perl -ne 's~import\s+([\w\.]+)\.R;~\1~; print')
read -t 2 -p "From package ${pkgFrom}"
if [[ -z "${pkgFrom}" ]]; then
    echo "Could not determine 'from' package"
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
#tobrandedLC=$(echo ${tobranded} | tr 'ABCEDFGHIJKLMNOPQRSTUVWXYZ' 'abcedfghijklmnopqrstuvwxyz')
#if [[ "$tobranded" = "Squore" ]]; then
#    tobrandedLC='scoreboard'
#fi
#for f in $(egrep -irl 'com\.doubleyellow\.[a-z]+\.R[^a-z]' *); do
#    cat ${f} | perl -ne "s~com\.doubleyellow\.(?!base.R)[a-z]+\.R([^A-Za-z'])~com.doubleyellow.${tobrandedLC}.R\$1~; print" > ${f}.1.txt
for f in $(egrep -irl "${pkgFrom}\.R[^a-z]" *); do
    cat ${f} | perl -ne "s~(import\s+|\()${pkgFrom}\.R([^A-Za-z'])~\$1${brandPkg}.R\$2~; print" > ${f}.1.txt
    if [[ -n "$(diff ${f} ${f}.1.txt)" ]]; then
        printf "File %-72s to %s \n" ${f} ${tobranded}
        mv ${f}.1.txt ${f} #.2.txt
    else
        rm ${f}.1.txt
    fi
done
echo '=================================='
#read -t 10 -p ' Java files changed. Continue ... ? '

# change some defaults in xml files (in java is/should be taken care of by means of code)
cd ../res

####################################################
# Change menu/layout/prefences xml files
####################################################

echo '=================================='
fromSuffix="(Squash|Default)"
toSuffix=${tobranded}
if [[ "$tobranded" = "Squore" ]] ; then
    fromSuffix="[A-Z][a-z][A-Za-z]+"
    toSuffix=Squash
fi
for f in $(egrep -rl "@(string|fraction).*__${fromSuffix}\""); do

    cat ${f} | perl -ne "s~__${fromSuffix}\"~__${toSuffix}\"~; print" > ${f}.1.xml
    if [[ -n "$(diff ${f} ${f}.1.xml)" ]]; then
        printf "File %-30s to %s strings\n" ${f} ${tobranded}
        #meld         ${f} ${f}.1.xml
        #rm -v ${f}.1.xml # TEMP
        mv ${f}.1.xml ${f}
    else
        rm ${f}.1.xml
    fi
    #read -t 10 -p ' Continue ? '
done
echo '=================================='
#read -t 10 -p 'menu/layout/preferences files processed. Continue ? '
if [[ -e correctSportSpecificResource.txt ]]; then
    rm -v correctSportSpecificResource.txt
fi
if [[ -n "${parentBrand}" ]]; then
    correctSportSpecificResource ${toSuffix} ${parentBrand}
fi
if [[ "${tobranded}" = "Padel" || "${parentBrand}" = "Padel" ]]; then
    correctSportSpecificResource Padel TennisPadel
    correctSportSpecificResource       TennisPadel Default
fi

if [[ -e correctSportSpecificResource.txt && -n "$(grep 'to Default' correctSportSpecificResource.txt)" ]]; then
    echo "To Default already happened"
else
    correctSportSpecificResource ${toSuffix} Default
fi


if [[ -e correctSportSpecificResource.txt ]]; then
    echo
    echo '######## SUMMARY ############'
    cat correctSportSpecificResource.txt
fi

####################################################
# Change build.gradle
####################################################

cd ..

# change manifest file name to one of brand we want to generate apk for
cat build.gradle | perl -ne "s~(srcFile\s+')AndroidManifest[A-Z][a-z][A-Za-z]+.xml~\1${brandMfFile}~; print" > build.gradle.tmp
if [[ -n "$(diff build.gradle.tmp build.gradle)" ]]; then
    mv build.gradle.tmp build.gradle
fi
