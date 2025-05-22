plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.upm.btb.accelerometeriot"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.upm.btb.accelerometeriot"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // JSON handling (for example, org.json)
    implementation("com.google.code.gson:gson:2.8.8")


    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
