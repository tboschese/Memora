plugins {
    id("memora.android.library.compose")
}

android {
    namespace = "com.memora.feature.settings"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
