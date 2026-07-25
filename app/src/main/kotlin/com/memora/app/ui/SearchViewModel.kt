package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.data.RoomSearchIndex
import com.memora.core.common.time.DayRange
import com.memora.feature.search.SearchDocument
import com.memora.feature.search.SearchQueryParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Busca na tela: a cada mudança da query, refaz a busca sobre o dia via [RoomSearchIndex]. Query
 * vazia zera os resultados (a UI mostra a dica, não "tudo").
 */
class SearchViewModel(
    private val index: RoomSearchIndex,
    clock: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val range: DayRange = DayRange.containing(clock, zone)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchDocument>>(emptyList())
    val results: StateFlow<List<SearchDocument>> = _results.asStateFlow()

    fun onQueryChange(text: String) {
        _query.value = text
        viewModelScope.launch {
            _results.value = index.searchDay(SearchQueryParser.parse(text), range)
        }
    }
}
