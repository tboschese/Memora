import com.android.build.api.dsl.LibraryExtension
import com.memora.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("memora.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val extension = extensions.getByType(LibraryExtension::class.java)
        configureAndroidCompose(extension)
    }
}
