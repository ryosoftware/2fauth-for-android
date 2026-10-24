plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.File;

fun increaseVersionCode(): Int {
    val properties = Properties()
    val file = rootProject.file("version.properties")
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
        throw FileNotFoundException("File 'version.properties' not found")
    }
}

android {
    namespace = "com.twofauth.android"
    compileSdk = 37

    defaultConfig {
        applicationId = namespace

        minSdk = 29
        targetSdk = compileSdk

        versionCode = increaseVersionCode()
        versionName = "3.33"

         buildConfigField(
                "String",
                "TAG",
                "\"TwoFAuth\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
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
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.bastiaanjansen.otp)
    implementation(libs.gson)
    implementation(libs.sql.cypher)
    implementation(libs.sqlite)
}

