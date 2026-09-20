plugins {
    id("com.washingtonpost.library")
}

android {
    namespace = "com.wapo.android.remotelog.splunk"

    buildTypes {
        release {
        }
    }

}

dependencies {
    implementation(project(":android-remotelog"))
    implementation(project(":android-commons"))
}
