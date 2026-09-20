import com.washingtonpost.convention.Dependencies

buildscript {
    dependencies {
        classpath(Dependencies.Deps.androidGradlePlugin)
        classpath(Dependencies.Kotlin.plugin)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}


task clean(type: Delete) {
    delete rootProject.buildDir
}