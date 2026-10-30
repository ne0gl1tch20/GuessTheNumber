package com.jarrlyyy.guessthenumber.desktop

import java.util.Properties

/**
 * Packaged builds cannot enable developer destinations with a launch argument alone.
 * The build must explicitly opt in with -Pgtn.desktop.debugToolsBuild=true, and the
 * process must also be launched with -Dgtn.desktop.debugTools=true.
 */
internal object DesktopDebugToolsPolicy {
    const val ENABLE_PROPERTY = "gtn.desktop.debugTools"

    private val debugOnlyDestinations = setOf(
        DesktopDestination.DEVELOPER_SETTINGS,
        DesktopDestination.DEVELOPER_CONSOLE,
        DesktopDestination.PROCESS_INSPECTOR,
        DesktopDestination.LUA_TESTING_HUB
    )

    fun enabled(): Boolean = isEnabled(
        buildAllowsDeveloperTools(),
        System.getProperty(ENABLE_PROPERTY)
    )

    internal fun isEnabled(buildAllowsDeveloperTools: Boolean, launchFlag: String?): Boolean =
        buildAllowsDeveloperTools && launchFlag == "true"

    private fun buildAllowsDeveloperTools(): Boolean = runCatching {
        val properties = Properties()
        DesktopDebugToolsPolicy::class.java.classLoader
            .getResourceAsStream("desktop-build.properties")
            ?.use(properties::load)
        properties.getProperty("debugToolsBuildAllowed") == "true"
    }.getOrDefault(false)

    fun isDeveloperOnly(destination: DesktopDestination): Boolean =
        destination in debugOnlyDestinations

    fun visibleDestinations(
        destinations: List<DesktopDestination>,
        allowDeveloperTools: Boolean = enabled()
    ): List<DesktopDestination> =
        if (allowDeveloperTools) destinations else destinations.filterNot(::isDeveloperOnly)
}
