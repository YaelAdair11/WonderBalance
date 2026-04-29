plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.wonderbalance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.wonderbalance"
        minSdk = 26
        targetSdk = 36
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    val version_room = "2.7.0"
    val version_lifecycle = "2.8.7"
    val version_nav = "2.8.9"

    // UI base (usando referencias del catálogo)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Room con KSP
    implementation("androidx.room:room-runtime:$version_room")
    implementation("androidx.room:room-ktx:$version_room")
    ksp("androidx.room:room-compiler:$version_room")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$version_lifecycle")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$version_lifecycle")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:$version_nav")
    implementation("androidx.navigation:navigation-ui-ktx:$version_nav")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit para consumir APIs
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}