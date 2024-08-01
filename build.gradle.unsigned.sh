# to create unsigned apk in github workflow
sed -e 's~^[ ]*signingConfig[ ]*sign~//signingConfig sign~' \
    -e 's~throw new~//throw new~' build.gradle > build.gradle.unsigned
diff build.gradle.unsigned build.gradle
mv build.gradle.unsigned build.gradle
