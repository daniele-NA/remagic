@file:Suppress("UnstableApiUsage", "NewerVersionAvailable","GradleDependency","UseTomlInstead")

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.diffplug.spotless") version "8.0.0" // == CODE REFACTORING == //
    id("com.google.gms.google-services") version "4.4.3"
    kotlin("plugin.serialization") version "2.1.10"
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
} else {
    throw GradleException("local.properties not found, cannot read ARCFOUR keys")
}

// INCLUDING FIREBASE SDK CPP //
val firebaseCppSdkDir = localProps.getProperty("firebase_cpp_sdk_dir")
    ?: throw GradleException("firebase_cpp_sdk_dir property not found in local.properties")
val cppDir = file(firebaseCppSdkDir)
if (cppDir.exists() && cppDir.isDirectory) {
    apply(from = "${cppDir.path}/Android/firebase_dependencies.gradle")
} else {
    throw GradleException("Invalid firebase_cpp_sdk_dir property ,current cpp dir detected =>  $cppDir")
}

android {
    namespace = "com.crescenzi.remagic"
    compileSdk = 36

    ndkVersion = "29.0.13846066"  // 16 KB alignment required

    defaultConfig {
        applicationId = "com.crescenzi.remagic"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        androidResources.localeFilters.add("it")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        externalNativeBuild{
            cmake{
                // Pass the firebase cpp sdk dir to native side
                arguments(
                    "-DFIREBASE_CPP_SDK_DIR=$firebaseCppSdkDir",

                    // API & ARCFOUR //
                    "-DARCFOUR_SECRET_KEY=${localProps.getProperty("arcfour.secret.key")}",
                    "-DREGOLO_API_KEY=${localProps.getProperty("regolo.encrypted.key")}"
                )
            }
        }
    }

    buildTypes {
        release {
            isShrinkResources = false
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/native/CMakeLists.txt")
        }
    }
}

// == LAUNCH WITH : ./gradlew spotlessApply == //
spotless {
    kotlin {
        target("**/*.kt")
        licenseHeader(
            """      
/*
MIT License

Copyright (c) 2025 [Daniele]
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
            """.trimIndent()
        )
    }
}




dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)


    implementation(libs.splash)
    implementation(libs.koin)
    implementation(libs.koin.insert)
    implementation(libs.lottie)
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
}