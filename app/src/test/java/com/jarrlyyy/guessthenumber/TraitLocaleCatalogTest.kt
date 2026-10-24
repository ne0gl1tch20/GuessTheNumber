package com.jarrlyyy.guessthenumber

import com.jarrlyyy.guessthenumber.traits.TraitLocaleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TraitLocaleCatalogTest {
    private val catalog = TraitLocaleCatalog()
    private val english = """{"id":"remote_streak","title":"Streak Keeper","description":"Reach a long streak."}"""
    private val filipino = """{"id":"remote_streak","title":"Tagapangalaga ng Sunod-sunod","description":"Makamit ang mahabang sunod-sunod na tama."}"""
    private val chinese = """{"id":"remote_streak","title":"连胜守护者","description":"达成连续正确猜测目标。"}"""

    @Test fun validatesEnglishAndEveryTranslationHasSameKeysAndIdentity() {
        catalog.validate(mapOf("en" to english, "fil" to filipino, "zh-CN" to chinese), "remote_streak")
    }

    @Test fun resolvesExactLocaleThenLanguageThenEnglishFallback() {
        val files = mapOf("en" to english, "fil" to filipino, "zh-CN" to chinese)
        assertEquals("fil", catalog.resolve(files, "remote_streak", "fil-PH").locale)
        assertEquals("zh-CN", catalog.resolve(files, "remote_streak", "zh-CN").locale)
        assertEquals("en", catalog.resolve(files, "remote_streak", "ja-JP").locale)
        assertEquals("Streak Keeper", catalog.resolve(files, "remote_streak", "ja-JP").title)
    }

    @Test fun normalizesUnderscoreLocaleRequests() {
        assertEquals("fil", catalog.resolve(mapOf("en" to english, "fil" to filipino), "remote_streak", "fil_PH").locale)
    }

    @Test fun normalizesLocaleLanguageAndRegionCase() {
        assertEquals("zh-CN", catalog.resolve(mapOf("en" to english, "zh-CN" to chinese), "remote_streak", "ZH_cn").locale)
    }

    @Test fun rejectsMissingEnglishMalformedLocaleMismatchedIdAndMissingFields() {
        assertThrows(IllegalArgumentException::class.java) { catalog.validate(mapOf("fil" to filipino), "remote_streak") }
        assertThrows(IllegalArgumentException::class.java) { catalog.validate(mapOf("en_US" to english), "remote_streak") }
        assertThrows(IllegalArgumentException::class.java) {
            catalog.validate(mapOf("en" to english, "fil" to filipino.replace("remote_streak", "other_trait")), "remote_streak")
        }
        assertThrows(IllegalArgumentException::class.java) {
            catalog.validate(mapOf("en" to english, "fil" to """{"id":"remote_streak","title":"Titre"}"""), "remote_streak")
        }
    }

    @Test fun rejectsLocaleMetadataThatDoesNotMatchItsFilename() {
        assertThrows(IllegalArgumentException::class.java) {
            catalog.validate(mapOf("en" to """{"locale":"fil","id":"remote_streak","title":"Title","description":"Description"}"""), "remote_streak")
        }
    }

    @Test fun rejectsBlankOrOversizedLocalizedText() {
        assertThrows(IllegalArgumentException::class.java) {
            catalog.validate(mapOf("en" to """{"id":"remote_streak","title":"","description":"x"}"""), "remote_streak")
        }
        val huge = """{"id":"remote_streak","title":"Title","description":"${"x".repeat(TraitLocaleCatalog.MAX_LOCALE_CHARS)}"}"""
        assertThrows(IllegalArgumentException::class.java) {
            catalog.validate(mapOf("en" to huge), "remote_streak")
        }
    }
}
