package com.example.f1app

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val LANGUAGE_RUSSIAN = "Русский"
    private const val LANGUAGE_ENGLISH = "English"

    fun applyLanguage(
        context: Context,
        language: String
    ): Context {

        val languageCode = when (language) {
            LANGUAGE_ENGLISH -> "en"
            else -> "ru"
        }

        val locale = Locale(languageCode)

        Locale.setDefault(locale)

        val configuration =
            Configuration(context.resources.configuration)

        configuration.setLocale(locale)

        return context.createConfigurationContext(
            configuration
        )
    }
}