# GitHub: actions (comparable with Gitlab: pipelines)

## Build

For each brand:

- Goto [actions](https://github.com/obbimi/Squore/actions)
  - Choose 'Build APK & Create Release' (buildrelease.yml) > Run workflow (on master)
  - Wait ... (Runs for about 5 minutes)
- Check [release](https://github.com/obbimi/Squore/releases) 
  - ensure to be signed in since just created release is in Draft and not publicly visible
  - 'Edit' the release that is in 'Draft'
  - Optionally add 'Notes' and click 'Publish release'
    - Next to the .apk files, source files will become visible

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

- Check with the android app [Obtainium](https://github.com/ImranR98/Obtainium) that updates are available.

## Deploy to Google

- Goto [actions](https://github.com/obbimi/Squore/actions)
  - Choose [Upload apk's from a github release to Google Play Store as draft](https://github.com/obbimi/Squore/actions/workflows/deploy.yml) > Run workflow
  - provide 'Brand' and 'Version' to deploy (Version should typically be 'latest')
    - Runs for less than a minute 
    - Several runs can be started simultaneously
- Goto [Play Console](https://play.google.com/console/u/0/developers/5046226336743383720/app-list)
  - Select the Correct app (Brand) and go to
    - Test and Release > Release overview
    - Under 'Latest release' there should be 'Open testing' version in status 'Draft' with latest apk files
      - [Badminton  ](https://play.google.com/console/u/0/developers/5046226336743383720/app/4974076001360456618/releases/overview)
      - [Squore     ](https://play.google.com/console/u/0/developers/5046226336743383720/app/4972255383319852172/releases/overview)
      - [Tabletennis](https://play.google.com/console/u/0/developers/5046226336743383720/app/4975506867766261096/releases/overview)
      - [TennisPadel](https://play.google.com/console/u/0/developers/5046226336743383720/app/4972207514792479457/releases/overview)
    - choose
      - 'Edit release' (right pointing arrow)
      - "next', choose previous or give new release notes
      - 'Save', followed by 'Go to overview'
      - 'Send change for review'

## Troubleshooting

- In case of 'Bad credentials' in 'Create upload .apk to /releases'
    - check 'Settings > Security > Secrets and variables > Actions'
    - please note all 4 secrets: GSM_API_KEY, KEYSTORE_BASE64, KEYSTORE_PASSWORD, GH_ACCESS_TOKEN need to be present in each repo
        - <https://github.com/obbimi/Badminton/settings/secrets/actions>
        - <https://github.com/obbimi/Squore/settings/secrets/actions>
        - <https://github.com/obbimi/Tabletennis/settings/secrets/actions>
        - <https://github.com/obbimi/TennisPadel/settings/secrets/actions>
