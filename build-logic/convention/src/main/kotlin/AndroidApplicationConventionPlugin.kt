import com.android.build.api.dsl.ApplicationExtension
import com.memora.buildlogic.configureAndroidCompose
import com.memora.buildlogic.configureKotlinAndroid
import com.memora.buildlogic.registerNetworkUsageReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            configureAndroidCompose(this)
            defaultConfig {
                targetSdk = 35
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        // Local-first: relata (não bloqueia) permissões de rede no manifest mergeado,
        // para que telemetria acidental não passe despercebida.
        registerNetworkUsageReport()
    }
}
