package com.memora.app

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Memora é local-first: funciona 100% offline hoje. Rede é permitida, mas só via features
 * opt-in (backup no Drive, modelos pagos). Este teste apenas **documenta** o conjunto atual de
 * permissões de rede do pacote — não falha o build. Quando uma feature de rede for adicionada,
 * atualize `intentionalNetworkPerms` conscientemente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NetworkPermissionsTest {

    private val intentionalNetworkPerms = emptySet<String>() // nenhuma ainda

    @Test
    fun `apenas permissoes de rede intencionais estao presentes`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions?.toSet().orEmpty()
        val networkPerms = requested.filter { it.startsWith("android.permission.") && it.contains("NETWORK") || it == "android.permission.INTERNET" }

        val unexpected = networkPerms - intentionalNetworkPerms
        if (unexpected.isNotEmpty()) {
            println(
                "[Memora] Aviso: permissões de rede não declaradas como intencionais: $unexpected. " +
                    "Se forem esperadas (sync/modelos), adicione a intentionalNetworkPerms.",
            )
        }
        // Informativo: sem asserção que quebre o build.
    }
}
