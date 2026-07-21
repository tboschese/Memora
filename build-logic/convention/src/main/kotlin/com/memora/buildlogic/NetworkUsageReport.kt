package com.memora.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Memora é **local-first**: tudo funciona offline. Acesso à rede é permitido, mas deve ser
 * sempre intencional e opt-in (ex.: backup no Drive, modelos pagos — features futuras).
 *
 * Esta task NÃO quebra o build. Ela apenas **relata** quais permissões de rede o manifest
 * mergeado (todas as deps) declara, para que nenhuma lib de analytics/telemetria entre
 * despercebida. É visibilidade, não bloqueio.
 */
abstract class ReportNetworkPermissionsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mergedManifest: RegularFileProperty

    @get:Internal
    abstract val variantName: Property<String>

    @TaskAction
    fun report() {
        val text = mergedManifest.get().asFile.readText()
        val networkPerms = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
        ).filter { text.contains(it) }

        if (networkPerms.isEmpty()) {
            logger.lifecycle("[Memora] '${variantName.get()}': nenhuma permissão de rede (offline).")
        } else {
            logger.lifecycle(
                "[Memora] '${variantName.get()}': permissões de rede presentes -> " +
                    "${networkPerms.joinToString()}. Confirme que são intencionais (sync/modelos), " +
                    "não telemetria acidental.",
            )
        }
    }
}

/** Registra o relatório por variante e o pendura no `check` (não-fatal). */
internal fun Project.registerNetworkUsageReport() {
    val androidComponents = extensions.getByType<ApplicationAndroidComponentsExtension>()
    androidComponents.onVariants { variant ->
        val taskName = "reportNetwork${variant.name.replaceFirstChar { it.uppercase() }}"
        val task = tasks.register<ReportNetworkPermissionsTask>(taskName) {
            group = "verification"
            description = "Lista as permissões de rede do manifest mergeado (informativo)."
            variantName.set(variant.name)
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.named("check").configure { dependsOn(task) }
    }
}
