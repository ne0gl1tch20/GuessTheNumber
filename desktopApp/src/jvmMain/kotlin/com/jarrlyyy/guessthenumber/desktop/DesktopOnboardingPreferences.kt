package com.jarrlyyy.guessthenumber.desktop

import java.util.prefs.Preferences

/**
 * Desktop mirror of Android's global Web Play onboarding preferences.
 * Tutorial completion is global, not tied to an individual save slot.
 */
internal object DesktopOnboardingPreferences {
    private const val NODE = "GuessTheNumber/web-play-onboarding"

    private fun preferences(): Preferences = Preferences.userRoot().node(NODE)

    fun isFeatureTutorialCompleted(featureId: String): Boolean {
        require(featureId.matches(Regex("[A-Za-z0-9_-]{1,48}")))
        return preferences().getBoolean("nimmy_feature_$featureId", false)
    }

    fun completeFeatureTutorial(featureId: String) {
        require(featureId.matches(Regex("[A-Za-z0-9_-]{1,48}")))
        preferences().apply {
            putBoolean("nimmy_feature_$featureId", true)
            flush()
        }
    }

    fun isNimmyTutorialCompleted(): Boolean =
        preferences().getBoolean("nimmy_tutorial_completed", false)

    fun completeNimmyTutorial() {
        preferences().apply {
            putBoolean("nimmy_tutorial_completed", true)
            flush()
        }
    }
}
