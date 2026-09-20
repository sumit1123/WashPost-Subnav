# android-classic

## Cloning the repository
Make sure to setup SSH access first: https://docs.github.com/en/authentication/connecting-to-github-with-ssh.

`git clone git@github.com:WashPost/android-classic.git`

### Update the kmp-classic submodule after cloning the repository

In the cloned repository, run `git submodule update --init --recursive`  
Copy the `local.properties` from the root to `kmp-classic`  
Check kmp-classic [README](https://github.com/WashPost/kmp-classic/blob/main/README.md) for more details on how to work with the submodule.  
An older JDK version may cause issues. Run `brew install temurin@21` and point Android studio to use it by:  
* Run `/usr/libexec/java_home -V` to get the path to the JDK 21
* Settings →  Build, Execution, Deployment → Build Tools → Gradle and add the path from the previous step to "Gradle JDK" section.

## Building the project

### Selecting the run configuration
There are two run configurations in the project: "android-tablet" and "android-wear". In most cases you will use "android-tablet". You only want to use "android-wear" when building the app for smart watches. Chose the proper run configuration in the top right area in Android Studio.

### Selecting the build variant
There are two build variants in the project: "playstore" and "amazon". They produce the proper builds for Google Playstore and Amazon App store. The main difference between the builds are billing system and push notifications infrastructure. You can select the build variant in "Build Variants" tab in Android Studio (bottom left area).

### Building from command line
`./gradlew :android-tablet:assemblePlaystoreDebug`

where: 
* `android-tablet` is a run configuration
* `Playstore` is a build flavor
* `Debug` is a build type.

Another example: `./gradlew :android-tablet:assembleAmazonRelease`

## Branches and tags

`develop` is the default branch, sometimes referred as "mainstream".

Feature branches start with the prefix `feature`, followed by a JIRA ticket. Example: `feature/AWA-7426`

Bug fixig branches start with the prefix `fix`, followed by a JIRA ticket. Example: `fix/AWA-8125`

Hot fix branches start with the prefix `hotfix`, followed by a release version. Example: `hotfix/v6.35.1_4.19.1`

All releases are tagged with git tags when the builds are submitted to the stores: https://github.com/WashPost/android-classic/tags

## Modules
The project follows [Feature Module Architecture](https://developer.android.com/topic/modularization).

* android-tablet - the main app module
* android-wear - smart watches app
* android-articles - a set of handful widgets for Article Recirculation Module (cards carousel at the bottom of an article)
* android-audio - the audio library. Used in podcasts, human-read articles, audio carousels and many more.
* android-comics - data layer to get comics from comics API
* android-commons - shared common code used across the app
* android-customnav - a module to handle user-customizable list of sections
* android-follow - a feature to follow an author and their content.
* android-foryou - a module to get and display content, provided by ForYou Flex API
* android-gdpr - a utility module to receive and obey General Data Protection Regulation law
* android-live-views - a module to communicate with Live API and display live updates
* android-paywall - common code used for billing and paywalling
* android-paywall-amazon - amazon specific code for paywall. For amazon builds only.
* android-paywall-playstore - google playstore specific code for paywall. For playstore builds only.
* android-posttv - a video library for the app
* android-push - push notification library for the app
* android-recirculation - UI components to build a carousel of cards on homepage and articles
* android-remotelog-library - a module that sends custom logs to logs API
* android-save - a module to retrieve and store articles that the user saves to favorites. The articles are syncronized between user's devices.
* android-zendesk - a module that allows the user to report a bug or reach out to customer support
* postkit/adsInf - Ads module
* postkit/androidext - legacy module for common code
* postkit/articles - a module to receive and render articles (outdated, see `articles2` package)
* postkit/com.wapo.rainbow.article.model - POJO models for articles (outdated, see `articles2` package)
* postkit/logger - local logs helper
* postkit/nightmode - helper code for nightmode functionality
* postkit/notifications - Alerts tab UI and persistent data
* postkit/search - Search feature of the app (outdated, see `search2` package)
* postkit/sections - Get and render homepage and other sections of the app.
* postkit/wapocontent - data and image loading helpers
* postkit/wapotext - helper code for text styling and formatting
* postkit/wapoviews - reusable UI components
* postkit/wpvolley - a fork of android-volley framework. Used for some networking operations.
* kmp-classic - git submodule of the kmp codebase. Contains shared code between the Android and iOS apps. 
