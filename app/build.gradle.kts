plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.File;

fun increaseVersionCode(): Int {
    val properties = Properties()
    val file = File("version.propierties")
    if (file.exists()) {
        file.inputStream().use {
            properties.load(it)
        }
        val new_version_code = properties["VERSION_CODE"].toString().toInt() + 1
        properties["VERSION_CODE"] = new_version_code.toString()
        file.writer().use {
            properties.store(it, null)
        }
        return new_version_code
    } 
    else {
        throw FileNotFoundException("File 'version.propierties' not found")
    }
}

android {
    namespace = "com.twofauth.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.twofauth.android"
        minSdk = 29
        targetSdk = 35
        versionCode = increaseVersionCode()
        versionName = "3.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            resValue("string", "app_version_name_value", "${defaultConfig.versionName}")
            resValue("integer", "app_version_number_value", "${defaultConfig.versionCode}")
            resValue("bool", "is_debug_version", "false");
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            resValue("string", "app_version_name_value", "${defaultConfig.versionName}")
            resValue("integer", "app_version_number_value", "${defaultConfig.versionCode}")
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
    implementation(libs.androidx.lifecycle)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.bastiaanjansen.otp)
    implementation(libs.gson)
    implementation(libs.sql.cypher)
    implementation(libs.sqlite)
}

