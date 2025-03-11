plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.twofauth.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.twofauth.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            resValue("string", "app_version_value", "${defaultConfig.versionName} (build ${defaultConfig.versionCode})")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            resValue("string", "app_version_value", "${defaultConfig.versionName} (build ${defaultConfig.versionCode}) (debug release)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.bastiaanjansen.otp)
    implementation(libs.gson)
}
