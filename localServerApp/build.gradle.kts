import java.util.Properties

plugins {
    id("com.android.application") version "8.7.3"
}

android {
    namespace = "org.schabi.newpipe.localserver"
    compileSdk = 35

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "org.schabi.newpipe.localserver"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                val stream = localPropertiesFile.inputStream()
                localProperties.load(stream)
                stream.close()
            }
            val storeFileProp = localProperties.getProperty("signing.storeFile")
            if (storeFileProp != null) {
                storeFile = file(storeFileProp)
                storePassword = localProperties.getProperty("signing.storePassword")
                keyAlias = localProperties.getProperty("signing.keyAlias")
                keyPassword = localProperties.getProperty("signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":extractor"))
    implementation(libs.squareup.okhttp)
    implementation(libs.google.jsr305)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("org.java-websocket:Java-WebSocket:1.5.3")
}
