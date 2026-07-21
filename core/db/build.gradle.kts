plugins {
    id("memora.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.memora.core.db"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room. A chave do SQLCipher é derivada em :core:security e injetada na abertura do
    // banco na Fase 1 (aqui ficam só entidades, DAOs e o contrato do database).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)
}
