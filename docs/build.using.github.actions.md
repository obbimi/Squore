# GitHub: actions (comparable with Gitlab: pipelines)

## Build

For each brand:

- Goto [actions](https://github.com/obbimi/Squore/actions)
    - Choose 'Build APK & Create Release' (buildrelease.yml) > Run workflow 
    - Wait ... (Runs for about 5 minutes)
- Check [release](https://github.com/obbimi/Squore/releases) 
  - ensure to be signed in since just created release is in Draft and not publicly visible
  - 'Edit' the release that is in 'Draft'
  - Press 'Publish release'
    - Next to apk files, source files will become visible

- Badminton
    - [build](https://github.com/obbimi/Badminton/actions/workflows/buildrelease.yml)
    - [verify draft and publish release](https://github.com/obbimi/Badminton/releases)
- Squore
  - [build](https://github.com/obbimi/Squore/actions/workflows/buildrelease.yml)
  - [verify draft and publish release](https://github.com/obbimi/Squore/releases)
- Tabletennis
  - [build](https://github.com/obbimi/Tabletennis/actions/workflows/buildrelease.yml)
  - [verify draft and publish release](https://github.com/obbimi/Tabletennis/releases)
- Tennis/Padel
  - [build](https://github.com/obbimi/TennisPadel/actions/workflows/buildrelease.yml)
  - [verify draft and publish release](https://github.com/obbimi/TennisPadel/releases)

- Check with the android app 'Obtainium' that updates are available.

Publish Released APK to Google > Run workflow 

## Deploy to Google

- Goto [actions](https://github.com/obbimi/Squore/actions)
    - Choose [Publish Released APK to Google](https://github.com/obbimi/Squore/actions/workflows/deploy.yml) > Run workflow
    - provide 'Brand' and 'VersionCode'
    - Runs for less than a minute
- Goto [Play Console](https://play.google.com/console/u/0/developers/5046226336743383720/app-list)
    - Select the Correct app (Brand) and go to
      - Test and Release > Release overview
      - Under 'Latest release' there should be 'Open tesing' version in status 'Draft' with latest apk files

## Troubleshooting

- In case of 'Bad credentials' in 'Create upload .apk to /releases'
    - check 'Settings > Security > Secrets and variables > Actions'
    - please note all 4 secrets: GSM_API_KEY, KEYSTORE_BASE64, KEYSTORE_PASSWORD, GH_ACCESS_TOKEN need to be present in each repo
        - <https://github.com/obbimi/Badminton/settings/secrets/actions>
        - <https://github.com/obbimi/Squore/settings/secrets/actions>
        - <https://github.com/obbimi/Tabletennis/settings/secrets/actions>
        - <https://github.com/obbimi/TennisPadel/settings/secrets/actions>
