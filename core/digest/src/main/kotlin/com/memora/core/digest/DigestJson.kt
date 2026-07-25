package com.memora.core.digest

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Valida e parseia a saída (JSON) do LLM local num [Digest] confiável (§5.5, "saída JSON validada
 * por schema"). Modelos locais erram formato: em vez de confiar no texto cru, exigimos a forma certa
 * e **sanitizamos** — JSON malformado ou sem `summary` retorna `null` (nada de digest-lixo na tela);
 * listas ausentes viram vazias; itens não-string são descartados; strings são trimadas e as vazias,
 * removidas. O [epochDay] vem do chamador — nunca do modelo.
 */
object DigestJson {

    fun parse(epochDay: Long, json: String): Digest? {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            return null
        }

        val summary = root.optString("summary").trim()
        if (summary.isEmpty()) return null // um digest sem resumo é inútil

        return Digest(
            epochDay = epochDay,
            summary = summary,
            decisions = root.stringList("decisions"),
            myActionItems = root.stringList("myActionItems"),
            themes = root.stringList("themes"),
        )
    }

    /** Lê um array de strings do campo [name], descartando itens não-string e strings em branco. */
    private fun JSONObject.stringList(name: String): List<String> {
        val array: JSONArray = optJSONArray(name) ?: return emptyList()
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val item = array.opt(i)
            if (item is String) {
                val trimmed = item.trim()
                if (trimmed.isNotEmpty()) out += trimmed
            }
        }
        return out
    }
}
