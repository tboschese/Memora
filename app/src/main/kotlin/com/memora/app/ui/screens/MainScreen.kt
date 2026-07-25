package com.memora.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.memora.app.ui.AppViewModel
import com.memora.app.ui.HomeViewModel
import com.memora.app.ui.SearchViewModel
import com.memora.feature.digest.DigestViewModel

/** Abas principais pós-unlock. Ícones em emoji para não puxar a lib de material-icons. */
private enum class MainTab(val label: String, val icon: String) {
    TODAY("Hoje", "📅"),
    DIGEST("Digest", "📄"),
    SEARCH("Buscar", "🔎"),
}

/**
 * Casa da sessão destrancada: barra de navegação inferior entre "Hoje", "Digest" e "Buscar". Os três
 * ViewModels são criados uma vez (com chaves distintas) a partir do banco da sessão e sobrevivem à
 * troca de abas.
 */
@Composable
fun MainScreen(appViewModel: AppViewModel) {
    val home: HomeViewModel = viewModel(
        key = "home",
        factory = viewModelFactory { initializer { appViewModel.createHomeViewModel() } },
    )
    val digest: DigestViewModel = viewModel(
        key = "digest",
        factory = viewModelFactory { initializer { appViewModel.createDigestViewModel() } },
    )
    val search: SearchViewModel = viewModel(
        key = "search",
        factory = viewModelFactory { initializer { appViewModel.createSearchViewModel() } },
    )

    var tab by remember { mutableStateOf(MainTab.TODAY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.icon) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (tab) {
            MainTab.TODAY -> TodayContent(home, contentModifier)
            MainTab.DIGEST -> DigestContent(digest, contentModifier)
            MainTab.SEARCH -> SearchContent(search, contentModifier)
        }
    }
}
