plugins {
    id("memora.android.library")
}

android {
    namespace = "com.memora.core.digest"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
