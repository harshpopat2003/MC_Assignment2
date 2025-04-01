plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.mc_assignment2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mc_assignment2"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
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

    // Traditional UI
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)

    // WorkManager
    implementation(libs.androidx.work)

    // Google Maps
    implementation(libs.google.maps)
    implementation (libs.androidx.cardview)
    implementation (libs.play.services.maps.v1910)



    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Add even though it may be in catalog
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Add even though it may be in catalog

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}