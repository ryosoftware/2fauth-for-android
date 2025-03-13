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
        versionCode = 7
        versionName = "1.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            resValue("string", "app_version_value", "${defaultConfig.versionName} (build ${defaultConfig.versionCode})")
            resValue("bool", "is_debug_version", "false");
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            resValue("string", "app_version_value", "${defaultConfig.versionName} (build ${defaultConfig.versionCode}) (debug release)")
            resValue("bool", "is_debug_version", "true");
        }
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
