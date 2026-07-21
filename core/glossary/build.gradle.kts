plugins {
    id("memora.android.library")
}

android {
    namespace = "com.memora.core.glossary"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
