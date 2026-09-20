# kmp-classic
KMP Repo

## Get Started - iOS
- Install XCode and follow the instructions on the ios-classic-app repo to get started with the iOS app.
- Install kdoctor - a tool that analyzes the dev environment: `brew install kdoctor`
- Install the JDK: `brew install temurin@21`
- Install the XCode-kotlin plugin: `brew install xcode-kotlin`
- Set it up with your XCode installation: `xcode-kotlin install`
- Restart your IDE and terminal ( CMD + Q )
- Run kdoctor in your terminal to check if all dependencies are loaded right. If not, fix the ones that are not ( we do not use CocoaPods )

## Local dev flow - iOS
- Get the required dependencies
- Clone the kmp-classic repo
- cd into the repo and run
- `./gradlew spmDevBuild -PspmBuildTargets=ios_simulator_arm64`
 the spmDevBuild builds for all target architectures. More than likely, you would do dev on the simulator. For all architectures, run `./gradlew spmDevBuild`
- Drag and drop the repo to XCode. The KMPShared package brought in through SPM should disappear using the local repo
- If you want to run the app on a physical device, run `./gradlew :KMPShared:assembleKMPSharedDebugXCFramework`, then clean and build XCode
- Make changes to either repo
- git restore Package.swift and then commit to kmp-classic
- Update the tag of KMPShared in Xcode if applicable. Delete reference to kmp-classic
- Commit to ios-classic-app

## Local dev flow - Android
- In the Android codebase, kmp-classic is added as a git submodule 
- Clone the `android-classic` repo and run  `git submodule update --init --recursive` to get the code for kmp-classic
- In android-classic, `cd kmp-classic` and run  
`git checkout <branchname>` to set the branch for kmp-classic  
OR  
`git checkout -b <branch> origin/<branch>` to create a new branch and set the upstream branch for kmp-classic
- Make changes, commit to kmp-classic and then commit to android-classic. The commit to android-classic should include the change to the kmp-classic submodule reference ( the commit hash that it points to )

## Releasing a new version
- Merge your branch and other feature branches to main
- Open `gradle.properties` and update the version number to the next release version
- Commit and push the change to main
- Go to the Actions tag in the `kmp-classic` repo and run the __KMMBridge-Release__ action on the main branch
- This will create a new tag and an xcframework linked to the tag
- Update the tag reference in the ios-classic-app repo to the new tag and update android-classic's submodule reference as well 
