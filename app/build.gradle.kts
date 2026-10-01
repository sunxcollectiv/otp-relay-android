import java.util.Properties

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

// Release signing. Values come from local.properties (gitignored) on a dev
// machine, or from environment variables on CI. Missing values leave the
// release build type unsigned rather than failing, so `assembleDebug` and IDE
// syncs keep working on a machine without the key.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(name: String): String? =
    localProps.getProperty("release.$name")
        ?: providers.environmentVariable("RELEASE_" + name.uppercase()).orNull
val releaseStoreFile = signingValue("storeFile")
val releaseStorePassword = signingValue("storePassword")
val releaseKeyAlias = signingValue("keyAlias")
val releaseKeyPassword = signingValue("keyPassword")
val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "app.otprelay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.otprelay"
        minSdk = 24
        // Held at 36 on purpose. Targeting 37 opts into Android 17's
        // FILTER_GENERIC_OTP, which withholds the SMS_RECEIVED broadcast for any
        // OTP-looking text for three hours, and that is the relay's whole job.
        // Only apps with a CompanionDeviceManager association (or a few system
        // roles) are exempt. Revisit before Play's target API deadline for 37.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "GIT_COMMIT", "\"${gitCommit.get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Name the App Bundle by version (otp-relay-1.0.0-release.aab) so uploads
    // to Play are distinguishable in a downloads folder. Bundle outputs cannot
    // be renamed per-output the way APKs are below, so this goes through the
    // archive base name instead; the APK block overrides it for APKs anyway.
    base.archivesName.set("otp-relay-${defaultConfig.versionName}")

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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


// Name release APKs by version so downloads from the site are distinguishable
// (otp-relay-1.0.0.apk rather than app-release.apk). Debug builds keep the
// default name, which is what Android Studio and `adb install` scripts expect.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.versionName.map { "otp-relay-$it.apk" }
            )
        }
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
