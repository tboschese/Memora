plugins {
    id("memora.android.library")
}

android {
    namespace = "com.memora.core.security"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Store de setup do PIN (salt + verifier) cifrado pelo Android Keystore.
    implementation(libs.androidx.security.crypto)
}
