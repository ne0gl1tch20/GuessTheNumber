package com.jarrlyyy.guessthenumber.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.jarrlyyy.guessthenumber.data.repository.LocaleManager

val LocalAppLocaleManager = staticCompositionLocalOf<LocaleManager> {
    error("LocalAppLocaleManager was not provided")
}

@Composable
fun localizedText(english: String): String =
    LocalAppLocaleManager.current.getStringByEnglish(english)
