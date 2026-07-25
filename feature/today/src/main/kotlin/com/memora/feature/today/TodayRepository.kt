package com.memora.feature.today

import com.memora.core.common.time.DayRange
import kotlinx.coroutines.flow.Flow

/**
 * Leitura da timeline do dia. Interface no feature; a implementação real (sobre os DAOs do Room)
 * fica em `:app`, a raiz de composição — assim `:core:db` não vira dependência da UI e o ViewModel
 * pode ser testado com um fake em memória. Os limites `[fromMs, toMs)` vêm de [DayRange].
 */
interface TodayRepository {
    fun observeUtterances(range: DayRange): Flow<List<TodayItem.Utterance>>

    fun observeGaps(range: DayRange): Flow<List<TodayItem.Gap>>
}
