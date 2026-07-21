plugins {
    id("memora.android.application")
    id("memora.android.hilt")
}

android {
    namespace = "com.memora.app"

    defaultConfig {
        applicationId = "com.memora.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // Core
    implementation(projects.core.common)
    implementation(projects.core.security)
    implementation(projects.core.db)
    implementation(projects.core.audio)
    implementation(projects.core.transcription)

    // Features
    implementation(projects.feature.today)
    implementation(projects.feature.digest)
    implementation(projects.feature.notes)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)
    implementation(projects.feature.onboarding)

    // AndroidX / Compose host
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    // Room in-memory para o teste de integração do pipeline de transcrição (sem SQLCipher).
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
}
