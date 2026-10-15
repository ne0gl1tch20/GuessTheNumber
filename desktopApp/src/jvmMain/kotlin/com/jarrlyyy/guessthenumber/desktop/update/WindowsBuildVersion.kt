package com.jarrlyyy.guessthenumber.desktop.update

import java.util.Properties

internal object WindowsBuildVersion {
    val current: String by lazy {
        val properties = Properties()
        WindowsBuildVersion::class.java.getResourceAsStream("/windows-build.properties")?.use {
            properties.load(it)
        }
        properties.getProperty("version", "1.0.0")
    }
}
