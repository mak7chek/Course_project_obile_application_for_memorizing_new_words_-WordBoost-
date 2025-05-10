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
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class") // Для адаптивного UI
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2") // ЗАМІНИ НА АКТУАЛЬНУ ВЕРСІЮ або версію з libs

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7") // Твоя версія, перевір на актуальність

    // ViewModel Compose (для viewModel() у Composable)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2") // ЗАМІНИ НА АКТУАЛЬНУ ВЕРСІЮ або версію з libs

    // Material Icons Extended (якщо потрібні додаткові іконки)
    implementation("androidx.compose.material:material-icons-extended") // Версія з BOM

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0") // ЗАМІНИ НА АКТУАЛЬНУ ВЕРСІЮ або версію з libs

    // Room (локальна база даних)
    implementation("androidx.room:room-runtime:2.6.1") // Твоя версія, перевір на актуальність
    kapt("androidx.room:room-compiler:2.6.1")      // KAPT для Room
    implementation("androidx.room:room-ktx:2.6.1")       // Kotlin extensions для Room

    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Приклад актуальної версії, можеш залишити свою 33.12.0, якщо вона стабільна

    // Бібліотеки Firebase (версії будуть взяті з Firebase BOM)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // ЗАМІНИ НА АКТУАЛЬНУ СТАБІЛЬНУ ВЕРСІЮ
    implementation("com.squareup.retrofit2:converter-gson:2.11.0") // Версія має співпадати з retrofit

    // Gson
    implementation("com.google.code.gson:gson:2.10.1") // Твоя версія, виглядає нормально

    // Kotlin Coroutines
    // Версія 1.9.0 дуже нова (навіть для уявного травня 2025 може бути bleeding edge).
    // Використовуй останню стабільну. Наприклад, 1.8.0 або новішу, якщо вона є.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // ЗАМІНИ НА АКТУАЛЬНУ СТАБІЛЬНУ ВЕРСІЮ
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // Та сама версія

    // Тестові залежності (використовуй libs, якщо вони там визначені)
    testImplementation(libs.junit) // Припускаю, що libs.junit визначено
    androidTestImplementation(libs.androidx.junit) // Припускаю, що libs.androidx.junit визначено
    androidTestImplementation(libs.androidx.espresso.core) // Припускаю, що libs.androidx.espresso.core визначено
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00")) // Також використовуй BOM для тестів Compose
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Версія з BOM

    // Debug залежності (використовуй libs)
    debugImplementation(libs.androidx.ui.tooling) // Для Layout Inspector і т.д.
    debugImplementation(libs.androidx.ui.test.manifest)

    // Media3 (якщо ти його використовуєш для чогось, наприклад, аудіо)
     implementation(libs.androidx.media3.common.ktx) // У тебе було, залиш, якщо потрібно

    // Не зовсім зрозуміло, для чого ця залежність, якщо є room-runtime та room-ktx.
    // Можливо, вона транзитивна або специфічна для якогось випадку.
    implementation(libs.androidx.room.common.jvm)
}
apply(plugin = "com.google.gms.google-services")