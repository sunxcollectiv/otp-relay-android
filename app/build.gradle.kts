plugins {
    alias(libs.plugins.android.application)
    // Enables the Compose compiler. AGP 9 turns on the `compose` build feature
    // automatically when this plugin is present, so there is no buildFeatures block.
    alias(libs.plugins.compose.compiler)
}

// Baked into BuildConfig so the settings screen can say which commit a build
// came from: the version name alone does not change between dev builds. A
// trailing "+" means the working tree had uncommitted changes.
val gitCommit = providers.exec {
    commandLine("git", "describe", "--always", "--dirty=+", "--abbrev=7")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }

android {
    namespace = "com.collectivworks.otprelay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.collectivworks.otprelay"
        minSdk = 24
        // Held at 36 on purpose. Targeting 37 opts into Android 17's
        // FILTER_GENERIC_OTP, which withholds the SMS_RECEIVED broadcast for any
        // OTP-looking text for three hours, and that is the relay's whole job.
        // Only apps with a CompanionDeviceManager association (or a few system
        // roles) are exempt. Revisit before Play's target API deadline for 37.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GIT_COMMIT", "\"${gitCommit.get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
