package com.memora.core.models.fake

import com.memora.core.models.ModelRegistry
import com.memora.core.models.ModelStatus

/** Fake do [ModelRegistry]: devolve estados programados. Útil para testar telas de "modelo ausente". */
class FakeModelRegistry(
    private val current: List<ModelStatus> = emptyList(),
) : ModelRegistry {
    override fun statuses(): List<ModelStatus> = current
    override suspend fun verify(): List<ModelStatus> = current
}
