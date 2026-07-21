package com.memora.core.common.log

/**
 * Logging seguro (default de privacidade).
 *
 * Nenhum log deve conter transcrição, coordenadas ou conteúdo de anotação. Em vez de
 * um `Log.d(tag, texto livre)` (fácil de vazar conteúdo por engano), o app loga eventos
 * ESTRUTURADOS: um nome de evento + campos que só aceitam identificadores e métricas.
 *
 * O que pode ser logado: ids opacos, contagens, durações, flags, nomes de estágio.
 * O que NUNCA pode: texto transcrito, lat/lng, corpo de anotação, embeddings.
 */
object SafeLog {

    /** Backend plugável — em testes captura, em produção encaminha para logcat. */
    fun interface Sink {
        fun emit(level: Level, event: String, fields: Map<String, String>)
    }

    enum class Level { DEBUG, INFO, WARN, ERROR }

    @Volatile
    var sink: Sink = NoopSink

    private object NoopSink : Sink {
        override fun emit(level: Level, event: String, fields: Map<String, String>) = Unit
    }

    fun d(event: String, vararg fields: Field) = log(Level.DEBUG, event, fields)
    fun i(event: String, vararg fields: Field) = log(Level.INFO, event, fields)
    fun w(event: String, vararg fields: Field) = log(Level.WARN, event, fields)
    fun e(event: String, vararg fields: Field) = log(Level.ERROR, event, fields)

    private fun log(level: Level, event: String, fields: Array<out Field>) {
        sink.emit(level, event, fields.associate { it.key to it.render() })
    }

    /**
     * Campo de log seguro. O tipo fechado impede passar `String` livre: só há construtores
     * para id opaco, contagem, duração, flag e estágio.
     */
    sealed interface Field {
        val key: String
        fun render(): String
    }

    /** Identificador opaco (ex.: sessionId, chunkId). Não é conteúdo. */
    data class Id(override val key: String, val value: String) : Field {
        override fun render() = value
    }

    data class Count(override val key: String, val value: Long) : Field {
        override fun render() = value.toString()
    }

    data class DurationMs(override val key: String, val value: Long) : Field {
        override fun render() = "${value}ms"
    }

    data class Flag(override val key: String, val value: Boolean) : Field {
        override fun render() = value.toString()
    }

    /** Nome de estágio/enum do pipeline (constante de código, não dado do usuário). */
    data class Stage(override val key: String, val value: String) : Field {
        override fun render() = value
    }
}
