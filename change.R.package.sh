#!/usr/bin/env bash

tobranded=${1:-Squore}

echo "Change to '${tobranded}'"

cd src
if [ "$tobranded" = "ASB" ]; then
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.asbsquore.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "Racketlon" ]; then
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.racketlon.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "Tabletennis" ]; then
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.tabletennis.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "CourtCare" ]; then
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtcaresquore.R\1~' ${f}
        #exit 1
    done
elif [ "$tobranded" = "UniOfNotthingham" ]; then
    for f in $(egrep -irl 'com.doubleyellow.scoreboard.R[^a-z]' *); do
        echo "File $f"
        sed -i 's~com.doubleyellow.scoreboard.R\([^a-z]\)~com.doubleyellow.courtscore_uon.R\1~' ${f}
        #exit 1
    done
else
    for f in $(egrep -irl "com.doubleyellow.(asbsquore|courtcaresquore|courtscore_uon|racketlon|tabletennis).R" *); do
        echo "File $f back to normal"
        sed -i 's~com.doubleyellow.\(asbsquore\|courtcaresquore\|courtscore_uon\|racketlon\|tabletennis\).R\([^a-z]\)~com.doubleyellow.scoreboard.R\2~' ${f}
        #exit 1
    done
    # comment out all brands ...
    sed -i 's~^\(\s*\)\(\w\+\s*(\s*SportType\.\)~\1//\2~'      com/doubleyellow/scoreboard/Brand.java
    # ... and uncomment Squore
    sed -i "s~^\(\s\+\)//\(Squore.\+R.string.app_name\)~\1\2~" com/doubleyellow/scoreboard/Brand.java
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

rm AndroidManifest.xml
ln -s AndroidManifest${tobranded}.xml AndroidManifest.xml
exit

