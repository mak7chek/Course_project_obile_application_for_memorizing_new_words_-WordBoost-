import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("kotlin-kapt")

}


android {
    namespace = "com.example.wordboost"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wordboost"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        val properties = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            properties.load(localPropsFile.inputStream())
        } else {
            throw GradleException("local.properties file not found")
        }

        val apiKey = properties.getProperty("DEEPL_API_KEY")
            ?: throw GradleException("DEEPL_API_KEY not found in local.properties")

        println("DEEPL_API_KEY from local.properties: $apiKey")

        buildConfigField("String", "DEEPL_API_KEY", "\"$apiKey\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx) // KTX runtime часто йде окремо

    // Явно вказуємо версію Material 3, якщо вона відрізняється від версії BOM за замовчуванням
    // Переконайтесь, що 1.3.2 сумісна з вашим BOM (1.8.0).
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2") // Версія Material 3 має бути однакова

    // Інші Compose-пов'язані бібліотеки, які можуть мати власні версії або не входити в BOM
    implementation("androidx.navigation:navigation-compose:2.7.7") // Залиште явну версію
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") // Залиште явну версію
    implementation("androidx.compose.runtime:runtime-livedata:1.8.0") // Залиште явну версію, якщо потрібна саме ця


    // Інші залежності вашого проєкту
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics")
    // libs.firebase.common.ktx, ймовірно, надається BOM або іншими залежностями Firebase

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")


    // Тестові залежності
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Використовуйте BOM і для тестів
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug залежності
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.room.common.jvm) // Залиште
}
apply(plugin = "com.google.gms.google-services")