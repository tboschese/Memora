plugins {
    id("memora.android.library.compose")
}

android {
    namespace = "com.memora.feature.digest"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.digest)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
