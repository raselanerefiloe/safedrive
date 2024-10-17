plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.10"
}

android {
    namespace = "com.drivesolutions.safedrive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.drivesolutions.safedrive"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")

    //Tensorflow
    //noinspection GradleDependency
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation ("org.tensorflow:tensorflow-lite-metadata:0.1.0")
    // Google Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // CameraX core library using the camera2 implementation
    val camerax_version = "1.5.0-alpha02"
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    // If you want to additionally use the CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    // If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-view:${camerax_version}")
    // If you want to additionally use the CameraX Extensions library
    implementation("androidx.camera:camera-extensions:${camerax_version}")
    // Appwrite
    implementation("io.appwrite:sdk-for-android:6.0.0")
    // OSMDroid for OpenStreetMap support in Android
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    implementation("com.google.accompanist:accompanist-permissions:0.36.0")


    // Jetpack Compose core dependencies
    implementation("androidx.compose.ui:ui:1.7.3")
    implementation("androidx.compose.runtime:runtime:1.7.3")
    implementation("androidx.compose.foundation:foundation:1.7.3")
    implementation("androidx.compose.animation:animation:1.7.3")
    implementation("androidx.compose.material:material:1.7.3")


    debugImplementation("androidx.compose.ui:ui-tooling:1.7.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.3")

    // Jetpack Compose Material 3 (newer UI components)
    implementation("androidx.compose.material3:material3:1.3.0")

    // Jetpack Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Navigation for Views/Fragments Integration (optional, if you're using them)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.2")

    // Dynamic Feature Module support for Navigation
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.8.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.2")


    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Add the Compose Compiler dependency compatible with Kotlin 1.9.10
    implementation("androidx.compose.compiler:compiler:1.5.3")
}