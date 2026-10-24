package com.jarrlyyy.guessthenumber.traits

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TraitLocalizedContent(
    val traitId: String,
    val locale: String,
    val title: String,
    val description: String
)

/** Validates trait locale files and resolves exact, language-only, then English fallback. */
class TraitLocaleCatalog(
    private val json: Json = Json { ignoreUnknownKeys = false }
) {
    fun validate(localeFiles: Map<String, String>, expectedTraitId: String): TraitLocaleCatalog {
        require(expectedTraitId.matches(Regex("[a-z][a-z0-9_.-]{1,63}"))) { "Invalid trait ID." }
        require(localeFiles.size in 1..MAX_LOCALES) { "Locale count is outside the supported limit." }
        require(localeFiles.containsKey("en")) { "Every trait must include the English fallback locale." }
        require(localeFiles.keys.all { LOCALE_PATTERN.matches(it) }) { "Invalid locale code." }
        val parsed = localeFiles.mapValues { (locale, source) ->
            require(source.isNotBlank() && source.length <= MAX_LOCALE_CHARS) { "Locale file is empty or too large: $locale" }
            val obj = json.parseToJsonElement(source).jsonObject
            require(obj.keys == REQUIRED_KEYS) { "Locale keys do not match the trait locale schema: $locale" }
            require(obj["id"]!!.jsonPrimitive.content == expectedTraitId) { "Locale trait ID mismatch: $locale" }
            require(obj["title"]!!.jsonPrimitive.content.isNotBlank()) { "Missing localized title: $locale" }
            require(obj["description"]!!.jsonPrimitive.content.isNotBlank()) { "Missing localized description: $locale" }
            obj
        }
        val englishKeys = parsed.getValue("en").keys
        require(parsed.values.all { it.keys == englishKeys }) { "Locale keys differ from English fallback." }
        return this
    }

    fun resolve(localeFiles: Map<String, String>, expectedTraitId: String, requestedLocale: String): TraitLocalizedContent {
        validate(localeFiles, expectedTraitId)
        val parsed = localeFiles.mapValues { (_, source) -> json.parseToJsonElement(source).jsonObject }
        val localeParts = requestedLocale.replace('_', '-').split('-')
        val normalized = localeParts.first().lowercase() + localeParts.drop(1).joinToString("") { "-" + it.uppercase() }
        val language = normalized.substringBefore('-')
        val chosen = when {
            normalized in parsed -> normalized
            language in parsed -> language
            else -> "en"
        }
        val entry = parsed.getValue(chosen)
        return TraitLocalizedContent(
            traitId = entry["id"]!!.jsonPrimitive.content,
            locale = chosen,
            title = entry["title"]!!.jsonPrimitive.content,
            description = entry["description"]!!.jsonPrimitive.content
        )
    }

    companion object {
        const val MAX_LOCALE_CHARS = 8_192
        const val MAX_LOCALES = 16
        private val LOCALE_PATTERN = Regex("[a-z]{2,3}(-[A-Z]{2})?")
        private val REQUIRED_KEYS = setOf("id", "title", "description")
    }
}
