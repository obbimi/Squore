#!/usr/bin/env bash

function showHelp {
cat <<!

Usage:

	$0 '(Squore|Badminton|TennisPadel|Racketlon|Tabletennis|CourtCare|UniOfNotthingham)'

Always change back to Squore before change to another 'Brand'
!
}
tobranded=${1:-Squore}
if [[ "${tobranded}" = "-h" ]]; then
	showHelp
	exit
fi

cd src
if [[ "$tobranded" = "Racketlon" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.racketlon.R\1~' ${f}
        #exit 1
    done
elif [[ "$tobranded" = "Tabletennis" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.tabletennis.R\1~' ${f}
        #exit 1
    done
elif [[ "$tobranded" = "Badminton" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.badminton.R\1~' ${f}
        #exit 1
    done
elif [[ "$tobranded" = "TennisPadel" ]]; then
    printf "Change to '${tobranded}'\n"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        printf "File %-72s to %s \n" $f $tobranded
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.tennispadel.R\1~' ${f}
        #exit 1
    done
elif [[ "$tobranded" = "CourtCare" ]]; then
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
elif [[ "$tobranded" = "Squore" ]]; then
    printf "Change back to '${tobranded}'\n"
    for f in $(egrep -irl "com.doubleyellow.(asbsquore|courtcaresquore|courtscore_uon|racketlon|tabletennis|badminton|tennispadel|squorewear).R" *); do
        printf "File %-72s back to %s normal\n" $f $tobranded
        sed -i 's~com.doubleyellow.\(asbsquore\|courtcaresquore\|courtscore_uon\|racketlon\|tabletennis\|badminton\|tennispadel\|squorewear\).R\([^a-z]\)~com.doubleyellow.scoreboard.R\2~' ${f}
        #exit 1
    done
    # comment out all brands ...
    sed -i 's~^\(\s*\)\(\w\+\s*(\s*SportType\.\)~\1//\2~'         com/doubleyellow/scoreboard/Brand.java
    # ... and uncomment Squore
    sed -i "s~^\(\s\+\)//\(Squore\s*(.*R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
else
	showHelp
fi

# simply actually set the active brand like Brand.brand = Brand.Squore
sed -i "s~Brand.\w\+;~Brand.${tobranded};~"                      com/doubleyellow/scoreboard/Brand.java
#vi +/Brand.                                                     com/doubleyellow/scoreboard/Brand.java

# ensure the chosen brand is uncommented in the Brands.java file (Squore brand should ALWAYS be uncommented)
if [[ "$tobranded" != "Squore" ]]; then
    sed -i "s~^\(\s\+\)//\(${tobranded}\s*(.*R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
fi

# change some defaults in xml files (in java is/should be taken care of by means of code)
cd ../res
if [[ "$tobranded" = "Racketlon" ]]; then
    for f in $(egrep -rl '@(string|fraction).*_Squash"'); do
        printf "File %-30s to %s strings\n" $f $tobranded
        sed -i 's~_Squash"~_Racketlon"~' ${f}
    done
elif [[ "$tobranded" = "Tabletennis" ]]; then
    for f in $(egrep -rl '@(string|fraction).*_Squash"'); do
        printf "File %-30s to %s strings\n" $f $tobranded
        sed -i 's~_Squash"~_Tabletennis"~' ${f}
    done
elif [[ "$tobranded" = "Badminton" ]]; then
    for f in $(egrep -rl '@(string|fraction).*_Squash"'); do
        printf "File %-30s to %s strings\n" $f $tobranded
        sed -i 's~_Squash"~_Badminton"~' ${f}
    done
elif [[ "$tobranded" = "TennisPadel" ]]; then
    for f in $(egrep -rl '@(string|fraction).*_Squash"'); do
        printf "File %-30s to %s strings\n" $f $tobranded
        sed -i 's~_Squash"~_TennisPadel"~' ${f}
    done
else
    # Squore
    for f in $(egrep -rl '@(string|fraction).*_(Racketlon|Tabletennis|Badminton|TennisPadel)"'); do
        printf "File %-30s to %s strings\n" $f $tobranded
        sed -i 's~_\(Racketlon\|Tabletennis\|Badminton\|TennisPadel\)"~_Squash"~' ${f}
    done
fi


cd ..
# comment out all Manifest file lines
sed -i 's~^\(\s\+\)srcFile~\1//srcFile~' build.gradle
# uncomment ManifestALL file lines
sed -i "s~^\(\s\+\)//\(srcFile\s\+'AndroidManifestALL\)~\1\2~" build.gradle
# uncomment the one Manifest file we are interested in
sed -i "s~^\(\s\+\)//\(srcFile\s\+'AndroidManifest${tobranded}\.\)~\1\2~" build.gradle
#vi +/Manifest${tobranded} build.gradle


#if [[ "$tobranded" == "SquoreWear" ]]; then
#    # for wear
#    # uncomment com.android.support:wear
#    sed -i "s~^\(\s*\)//\(\s*implementation\s\s*'com.android.support:wear\)~\1\2~" build.gradle
#    # minSdkVersion must be increased to 23
#    sed -i "s~^\(\s*minSdkVersion\)\s\s*[0-9][0-9]*~\1 23~"                        build.gradle
#else
#    # for not-wear
#    # minSdkVersion can increased to 19
#    sed -i "s~^\(\s*minSdkVersion\)\s\s*[0-9][0-9]*~\1 19~"                        build.gradle
#    # and if so, comment out com.android.support:wear
#    sed -i "s~^\(\s*\)\(\s*implementation\s\s*'com.android.support:wear\)~\1//\2~" build.gradle
#fi

