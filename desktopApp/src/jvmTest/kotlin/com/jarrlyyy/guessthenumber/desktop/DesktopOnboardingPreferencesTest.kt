package com.jarrlyyy.guessthenumber.desktop

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.prefs.Preferences

class DesktopOnboardingPreferencesTest {
    @Test
    fun featureTutorialCompletionIsPersistedAndCanBeReadBack() {
        val featureId = "test_" + UUID.randomUUID().toString().replace("-", "")
        val preferences = Preferences.userRoot().node("GuessTheNumber/web-play-onboarding")
        try {
            assertFalse(DesktopOnboardingPreferences.isFeatureTutorialCompleted(featureId))

            DesktopOnboardingPreferences.completeFeatureTutorial(featureId)

            assertTrue(DesktopOnboardingPreferences.isFeatureTutorialCompleted(featureId))
        } finally {
            preferences.remove("nimmy_feature_$featureId")
            preferences.flush()
        }
    }

    @Test
    fun mainTutorialCompletionIsStoredGlobally() {
        val preferences = Preferences.userRoot().node("GuessTheNumber/web-play-onboarding")
        val original = preferences.get("nimmy_tutorial_completed", null)
        try {
            DesktopOnboardingPreferences.completeNimmyTutorial()
            assertTrue(DesktopOnboardingPreferences.isNimmyTutorialCompleted())
        } finally {
            if (original == null) preferences.remove("nimmy_tutorial_completed")
            else preferences.put("nimmy_tutorial_completed", original)
            preferences.flush()
        }
    }
}
