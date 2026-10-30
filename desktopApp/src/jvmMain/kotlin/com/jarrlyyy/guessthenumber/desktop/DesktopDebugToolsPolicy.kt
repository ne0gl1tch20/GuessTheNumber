package com.jarrlyyy.guessthenumber.desktop

/**
 * Developer-only Windows destinations stay hidden and unreachable unless the
 * JVM is explicitly launched with -Dgtn.desktop.debugTools=true.
 * The default is deliberately off for packaged/release builds.
 */
internal object DesktopDebugToolsPolicy {
    const val ENABLE_PROPERTY = "gtn.desktop.debugTools"

    private val debugOnlyDestinations = setOf(
        DesktopDestination.DEVELOPER_SETTINGS,
        DesktopDestination.DEVELOPER_CONSOLE,
        DesktopDestination.PROCESS_INSPECTOR,
        DesktopDestination.LUA_TESTING_HUB
    )

    fun enabled(): Boolean = System.getProperty(ENABLE_PROPERTY) == "true"

    fun isDeveloperOnly(destination: DesktopDestination): Boolean =
        destination in debugOnlyDestinations

    fun visibleDestinations(destinations: List<DesktopDestination>): List<DesktopDestination> =
        if (enabled()) destinations else destinations.filterNot(::isDeveloperOnly)
}
