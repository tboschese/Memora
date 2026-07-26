package com.memora.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.memora.app.ui.AppViewModel
import com.memora.app.ui.GlossaryViewModel
import com.memora.app.ui.HomeViewModel
import com.memora.app.ui.SearchViewModel
import com.memora.app.ui.TasksViewModel
import com.memora.feature.digest.DigestViewModel
import com.memora.feature.settings.SettingsViewModel

/** Abas principais pós-unlock. Ícones em emoji para não puxar a lib de material-icons. */
private enum class MainTab(val label: String, val icon: String) {
    TODAY("Hoje", "📅"),
    TASKS("Tarefas", "✓"),
    DIGEST("Digest", "📄"),
    SEARCH("Buscar", "🔎"),
    SETTINGS("Ajustes", "⚙️"),
}

/**
 * Casa da sessão destrancada: barra de navegação inferior entre "Hoje", "Digest" e "Buscar". Os três
 * ViewModels são criados uma vez (com chaves distintas) a partir do banco da sessão e sobrevivem à
 * troca de abas.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val settings: SettingsViewModel = viewModel(
        key = "settings",
        factory = viewModelFactory { initializer { appViewModel.createSettingsViewModel() } },
    )
    val tasks: TasksViewModel = viewModel(
        key = "tasks",
        factory = viewModelFactory { initializer { appViewModel.createTasksViewModel() } },
    )

    val allTasks by tasks.tasks.collectAsState()
    val pendingTasks = allTasks.count { !it.done }

    var tab by remember {
        mutableStateOf(MainTab.entries.getOrElse(appViewModel.selectedTabIndex) { MainTab.TODAY })
    }
    var showGlossary by remember { mutableStateOf(false) }

    if (showGlossary) {
        val glossary: GlossaryViewModel = viewModel(
            key = "glossary",
            factory = viewModelFactory { initializer { appViewModel.createGlossaryViewModel() } },
        )
        GlossaryScreen(glossary, onBack = { showGlossary = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memora") },
                actions = {
                    TextButton(onClick = { appViewModel.lock() }) { Text("🔒 Trancar") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            tab = t
                            appViewModel.selectedTabIndex = t.ordinal
                        },
                        icon = {
                            if (t == MainTab.TASKS && pendingTasks > 0) {
                                BadgedBox(badge = { Badge { Text("$pendingTasks") } }) { Text(t.icon) }
                            } else {
                                Text(t.icon)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (tab) {
            MainTab.TODAY -> TodayContent(home, contentModifier)
            MainTab.TASKS -> TasksContent(tasks, contentModifier)
            MainTab.DIGEST -> DigestContent(digest, contentModifier)
            MainTab.SEARCH -> SearchContent(search, contentModifier)
            MainTab.SETTINGS -> SettingsContent(
                settings,
                onOpenGlossary = { showGlossary = true },
                onExportHistory = { appViewModel.exportHistory() },
                onImportHistory = { appViewModel.importHistory(it) },
                modifier = contentModifier,
            )
        }
    }
}
