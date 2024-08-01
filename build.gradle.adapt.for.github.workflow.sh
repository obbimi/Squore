# to create unsigned apk in github workflow
sed -e 's~^[ ]*signingConfig[ ]*sign~//signingConfig sign~' \
    -e 's~throw new~//throw new~' build.gradle > build.gradle.unsigned

# do not use com.google.gms (google-services.json) is not in repo
grep -v "com.google.gms" build.gradle.unsigned > build.gradle.unsigned.nogms
rm build.gradle.unsigned

diff build.gradle.unsigned.nogms build.gradle
mv   build.gradle.unsigned.nogms build.gradle
