#!/usr/bin/env bash

function showHelp {
cat <<!

Usage:

	$0 '(Squore|Racketlon|Tabletennis|CourtCare|UniOfNotthingham)'

Always change back to Squore before change to another 'Brand'
!
}
tobranded=${1:-Squore}
if [ "${tobranded}" = "-h" ]; then
	showHelp
	exit
fi

cd src
if [ "$tobranded" = "ASB" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.asbsquore.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "Racketlon" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.racketlon.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "Tabletennis" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.tabletennis.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "CourtCare" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtcaresquore.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "UniOfNotthingham" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtscore_uon.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "Squore" ]; then
    echo "Change to '${tobranded}'"
    for f in $(egrep -irl "com.doubleyellow.(asbsquore|courtcaresquore|courtscore_uon|racketlon|tabletennis).R" *); do
        echo "File $f back to normal"
        sed -i 's~com.doubleyellow.\(asbsquore\|courtcaresquore\|courtscore_uon\|racketlon\|tabletennis\).R\([^a-z]\)~com.doubleyellow.scoreboard.R\2~' ${f}
        #exit 1
    done
    # comment out all brands ...
    sed -i 's~^\(\s*\)\(\w\+\s*(\s*SportType\.\)~\1//\2~'      com/doubleyellow/scoreboard/Brand.java
    # ... and uncomment Squore
    sed -i "s~^\(\s\+\)//\(Squore.\+R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
else
	showHelp
fi

# simply actually set the active brand like Brand.brand = Brand.Squore
sed -i "s~Brand.\w\+;~Brand.${tobranded};~"                      com/doubleyellow/scoreboard/Brand.java
#vi +/Brand.                                                     com/doubleyellow/scoreboard/Brand.java

# ensure the chosen brand is uncommented in the Brands.java file (Squore brand should ALWAYS be uncommented)
sed -i "s~^\(\s\+\)//\(${tobranded}.\+R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java

# change some defaults in xml files (in java is/should be taken care of by means of code)
cd ../res
if [ "$tobranded" = "Racketlon" ]; then
    for f in $(egrep -rl '@string.*_(Squash|Tabletennis)"'); do
        echo "File $f to Racketlon strings"
        sed -i 's~_\(Squash\|Tabletennis\)"~_Racketlon"~' ${f}
    done
elif [ "$tobranded" = "Tabletennis" ]; then
    for f in $(egrep -rl '@string.*_(Squash|Racketlon)"'); do
        echo "File $f to Tabletennis strings"
        sed -i 's~_\(Squash\|Racketlon\)"~_Tabletennis"~' ${f}
    done
else
    for f in $(egrep -rl '@string.*_(Racketlon|Tabletennis)"'); do
        echo "File $f back to Squash strings"
        sed -i 's~_\(Racketlon\|Tabletennis\)"~_Squash"~' ${f}
    done
fi


cd ..
sed -i 's~^\(\s\+\)srcFile~\1//srcFile~' build.gradle
sed -i "s~^\(\s\+\)//\(srcFile\s\+'AndroidManifest${tobranded}\)~\1\2~" build.gradle
#vi +/Manifest${tobranded} build.gradle

exit
rm AndroidManifest.xml
ln -s AndroidManifest${tobranded}.xml AndroidManifest.xml
exit

