package com.memora.core.speaker

import kotlin.math.sqrt

/**
 * Resultado do enrollment: o [profile] gerado e a [cohesion] das amostras — a similaridade média de
 * cada amostra ao centroide, em `[0,1]`. Cohesion baixa sugere amostras inconsistentes (ruído,
 * ambientes muito diferentes, outra voz no meio) e é o gancho para a UI pedir re-treino (RF-22).
 */
data class EnrollmentResult(
    val profile: VoiceProfile,
    val cohesion: Float,
)

/**
 * Enrollment do dono (RF-18): funde as amostras de embedding coletadas (o fluxo real são ~2 min em
 * 2 ambientes) num único [VoiceProfile]. Puro e determinístico — o cálculo do embedding fica no
 * modelo (ECAPA-TDNN); aqui só se agrega o que ele produziu.
 *
 * O perfil é o **centroide normalizado** (L2) das amostras: a média capta a voz típica, e a
 * normalização deixa o vetor unitário, estabilizando o cosseno da classificação [decideSpeaker].
 */
object VoiceEnrollment {

    /** Mínimo de amostras para um perfil minimamente robusto (ao menos os 2 ambientes do RF-18). */
    const val MIN_SAMPLES = 2

    /**
     * Constrói o perfil a partir das [samples]. Exige ao menos [MIN_SAMPLES] embeddings, todos do
     * mesmo tamanho e não vazios. [threshold]/[unknownMargin] são repassados ao [VoiceProfile].
     */
    fun buildProfile(
        samples: List<FloatArray>,
        threshold: Float = 0.65f,
        unknownMargin: Float = 0.1f,
    ): EnrollmentResult {
        require(samples.size >= MIN_SAMPLES) {
            "enrollment precisa de ao menos $MIN_SAMPLES amostras, veio ${samples.size}"
        }
        val dim = samples.first().size
        require(dim > 0) { "embedding vazio" }
        require(samples.all { it.size == dim }) { "amostras de tamanhos diferentes" }

        val centroid = FloatArray(dim)
        for (sample in samples) {
            for (i in 0 until dim) centroid[i] += sample[i]
        }
        for (i in 0 until dim) centroid[i] /= samples.size

        val normalized = l2Normalize(centroid)
        val cohesion = samples
            .map { cosineSimilarity(it, normalized) }
            .average()
            .toFloat()

        return EnrollmentResult(
            profile = VoiceProfile(normalized, threshold, unknownMargin),
            cohesion = cohesion,
        )
    }
}

/** Normaliza L2 (vetor unitário). Um vetor nulo é devolvido como está — não há direção a preservar. */
internal fun l2Normalize(v: FloatArray): FloatArray {
    var norm = 0f
    for (x in v) norm += x * x
    if (norm == 0f) return v.copyOf()
    val inv = 1f / sqrt(norm)
    return FloatArray(v.size) { v[it] * inv }
}
