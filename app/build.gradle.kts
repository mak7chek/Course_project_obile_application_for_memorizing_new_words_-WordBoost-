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
    testOptions {
        unitTests {
            isReturnDefaultValues = true // ось так
        }
    }
}

dependencies {
    // Test Implementation Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.mockk:mockk:1.13.10")
    // testImplementation("io.mockk:mockk-agent-jvm:1.13.10") // Розкоментуй, якщо потрібно

    // Android Test Implementation Dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Implementation Dependencies
    implementation(platform("androidx.compose:compose-bom:2024.05.00")) // BOM для Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Оновлено

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // Оновлено

    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Рекомендовано стабільну
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Має відповідати версії retrofit

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Debug Implementation Dependencies
    debugImplementation("androidx.compose.ui:ui-tooling") // Версія з compose-bom
    debugImplementation("androidx.compose.ui:ui-test-manifest") // Версія з compose-bom
    implementation(libs.androidx.media3.common.ktx) // Заміни на пряме посилання, якщо знаєш версію, або якщо використовуєш Version Catalog, перевір аліас
    implementation(libs.androidx.room.common.jvm)   // Аналогічно
}
apply(plugin = "com.google.gms.google-services")