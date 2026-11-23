plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.libertiesevents"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.libertiesevents"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    // Firebase BoM manages compatible Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))

// Firebase Authentication for login and registration
    implementation("com.google.firebase:firebase-auth")

// Firebase Realtime Database for events and subscriptions
    implementation("com.google.firebase:firebase-database")
}