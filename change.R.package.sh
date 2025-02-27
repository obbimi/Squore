#!/usr/bin/env bash

defaultTimeout=2

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
    brandPkg=$(grep package= ${brandMfFile} | perl -ne 's~.*"([a-z\.]+)".*~$1~; print')
    if [[ -z "$brandPkg" ]]; then
        echo "Could not determine package from ${brandMfFile}"
        exit
    fi
    if [[ $(echo "$brandPkg" | wc -l) -gt 1 ]]; then
        echo "Invalid multilined package $brandPkg derived from ${brandMfFile}"
        exit
    fi
    read -t $defaultTimeout -p "New package ${brandPkg}"
fi

if [[ -z "${parentBrand}" ]]; then
    parentBrand="$(cat ${brandMfFile} | grep parentBrand | sed -e 's~parentBrand\s*=\s*~~' | tr -d ' ')"
    read -t $defaultTimeout -p "Backup brand from ${brandMfFile} ==> '${parentBrand}'. Continuing in $defaultTimeout...."
    echo
fi

function correctSportSpecificResource {
    from=$1
    to=$2

    read -t $defaultTimeout -p "Correcting from ${from} to ${to}. Continue ?"
    echo

    echo -n > notcorrected.txt
    echo -n > corrected.txt

    let correctedCnt=0
    let keptCnt=0
    # list all renamed USED resource names and ...
    for res in $(grep -E -r '@(array|bool|fraction|integer|integer-array|string|string-array|color).*__[A-Z][A-Za-z]+' * | perl -ne 's~.*@(array|bool|fraction|integer|integer-array|string|string-array|color)\/(\w+__\w+).*~$2~; print' | sort -u); do
        # ... check that the appropriate res definition exists
        if grep -E -q -R "name=.${res}." *; then
            if echo ${res} | grep -q "__${from}"; then
                nrOfOccurrencesAsExpr=$(grep -E -h -c -r "@.*${res}" * | grep -E -v '^0$' | xargs | sed -e 's/\ /+/g')
                nrOfOccurrences=$((${nrOfOccurrencesAsExpr}))
                if [[ "${to}" = "XDefault" ]]; then
                    read -t $defaultTimeout -p "Nr of occurrences of valid ${res} : ${nrOfOccurrences}"
                fi
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

                    # remember current modification time of file
                    oldFileTime=$(find ${f} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")

                    sed -i "s~${res}~${newRes}~" ${f}
                    let correctedCnt=correctedCnt+nrToCorrect

                    # restore modification time of file
                    touch -t "${oldFileTime}" ${f}

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
if ! grep -E -q "${tobranded}.*SportType" com/doubleyellow/scoreboard/Brand.java; then
    echo "ERROR: Brand ${tobranded} not found in Brand.java" > /dev/stderr

    grep -E -l "${tobranded}.*SportType" ../res/values/*.xml

    exit 1
fi

# derive current package by looking for .R reference in ScoreBoard.java
pkgFrom=$(grep -E 'import[ ]+.*\.R;' com/doubleyellow/scoreboard/main/ScoreBoard.java | perl -ne 's~import\s+([\w\.]+)\.R;~\1~; print')
read -t $defaultTimeout -p "From package ${pkgFrom}"
if [[ -z "${pkgFrom}" ]]; then
    echo "Could not determine 'from' package"
    exit 1
fi

####################################################
# Change Brand.java
####################################################
f=com/doubleyellow/scoreboard/Brand.java

oldFileTime=$(find ${f} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")
if [[ "$tobranded" = "Squore" ]]; then
    # comment out all brands ...
    sed -i 's~^\(\s*\)\(\w\+\s*(\s*SportType\.\)~\1//\2~'         ${f}
    # ... and uncomment Squore (always required)
    sed -i "s~^\(\s\+\)//\(Squore\s*(.*R.string.app_name\)~\1\2~" ${f}
fi
# ensure the chosen brand is uncommented in the Brands.java file (Squore brand should ALWAYS be uncommented)
if [[ "$tobranded" != "Squore" ]]; then
    sed -i "s~^\(\s\+\)//\(${tobranded}\s*(.*R.string.app_name\)~\1\2~" ${f}
fi

# simply actually set the active brand like Brand.brand = Brand.Squore
sed -i "s~Brand.\w\+;~Brand.${tobranded};~"                      ${f}
#vi +/Brand.                                                     ${f}

touch -t "${oldFileTime}" ${f}

####################################################
# Change <other>.java
####################################################
read -t $defaultTimeout -p "Brand.java adapted. Continue?"

echo '=================================='
printf "Change to '${tobranded}'\n"
#tobrandedLC=$(echo ${tobranded} | tr 'ABCEDFGHIJKLMNOPQRSTUVWXYZ' 'abcedfghijklmnopqrstuvwxyz')
#if [[ "$tobranded" = "Squore" ]]; then
#    tobrandedLC='scoreboard'
#fi
for f in $(grep -E -il -R "${pkgFrom}\.R[^a-z]" *); do
    oldFileTime=$(find ${f} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")
    cat ${f} | perl -ne "s~(import\s+|\()${pkgFrom}\.R([^A-Za-z'])~\$1${brandPkg}.R\$2~; print" > ${f}.1.txt
    if [[ -n "$(diff ${f} ${f}.1.txt)" ]]; then
        printf "File %-72s to %s \n" ${f} ${tobranded}
        mv ${f}.1.txt ${f} #.2.txt
    else
        rm ${f}.1.txt
    fi
    touch -t "${oldFileTime}" ${f}
done
echo '=================================='
read -t $defaultTimeout -p ' Java files changed. Continue ... ? '

# change some defaults in xml files (in java is/should be taken care of by means of code)
cd ../res

####################################################
# Change menu, layout and preferences xml files
####################################################

echo '=================================='
fromSuffix="(Squash|Default)"
toSuffix=${tobranded}
if [[ "$tobranded" = "Squore" ]] ; then
    fromSuffix="[A-Z][A-Za-z]+" # replace ALL left behinds back to Squash/Default
    toSuffix=Squash
fi
for f in $(grep -E -R -l "@(string|fraction|color).*__${fromSuffix}\"" | sort); do
    oldFileTime=$(find ${f} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")

    cat ${f} | perl -ne "s~__${fromSuffix}\"~__${toSuffix}\"~; print" > ${f}.1.xml
    if [[ -n "$(diff ${f} ${f}.1.xml)" ]]; then
        printf "File %-30s to %s strings\n" ${f} ${tobranded}
        #meld         ${f} ${f}.1.xml
        #rm -v ${f}.1.xml # TEMP
        mv ${f}.1.xml ${f}
        #cp -v ${f}.1.xml ${f}
    else
        rm ${f}.1.xml
    fi
    #read -t $defaultTimeout -p ' Continue ? '
    touch -t "${oldFileTime}" ${f}
done
echo '=================================='
#read -t $defaultTimeout -p 'menu/layout/preferences files processed. Continue ? '
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

f=build.gradle
oldFileTime=$(find ${f} -maxdepth 0 -printf "%Ty%Tm%Td%TH%TM.%.2TS")

# change manifest file name to one of brand we want to generate apk for, ensuring NOT to change the AndroidManifestALL references
cat ${f} | perl -ne "s~(srcFile\s+')AndroidManifest[A-Z][A-Za-z]+.xml~\1${brandMfFile}~; print" > ${f}.tmp
if [[ -n "$(diff ${f}.tmp ${f})" ]]; then
    mv ${f}.tmp ${f}
else
    rm ${f}.tmp
fi

# change 'namespace "com.doubleyellow.tennispadel"'
cat ${f} | perl -ne "s~(namespace\s+')${pkgFrom}(')~\1${brandPkg}\2~; print" > ${f}.tmp
if [[ -n "$(diff ${f}.tmp ${f})" ]]; then
    mv ${f}.tmp ${f}
else
    rm ${f}.tmp
fi

touch -t "${oldFileTime}" ${f}
