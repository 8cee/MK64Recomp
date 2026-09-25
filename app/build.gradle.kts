plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.eightcee.mk64recomp"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.eightcee.mk64recomp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release { isMinifyEnabled = false }
    }
    buildFeatures { buildConfig = true }
    kotlinOptions { jvmTarget = "17" }
}
