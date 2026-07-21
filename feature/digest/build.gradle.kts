plugins {
    id("memora.android.library.compose")
}

android {
    namespace = "com.memora.feature.digest"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
