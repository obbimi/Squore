# to create unsigned apk in github workflow
sed -e 's~^[ ]*signingConfig[ ]*sign~//signingConfig sign~' \
    -e 's~throw new~//throw new~' build.gradle > build.gradle.unsigned

# do not use com.google.gms (google-services.json) is not in repo
GSM_API_KEY="$1";
if [[ -z "${GSM_API_KEY}" ]]; then
    # don't use com.google.gsm
    grep -v "com.google.gms" build.gradle.unsigned > build.gradle.unsigned.nogms
    rm build.gradle.unsigned
    #diff build.gradle.unsigned.nogms build.gradle
    mv   build.gradle.unsigned.nogms build.gradle
else
    # prepare actual google-services.json with secrets from input variable
    mv   build.gradle.unsigned build.gradle
    # create a google-services.json with secrets
    sed -e "s~__GSM_API_KEY__~${GSM_API_KEY}~" google-services.no-secrets.json > google-services.json
fi


# create a dummy zip file to ensure compile completes for R.raw.squore_iddo
touch res/raw/squore_iddo.zip
