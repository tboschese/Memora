plugins {
    id("memora.android.library")
}

android {
    namespace = "com.memora.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
