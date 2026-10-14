package com.jarrlyyy.guessthenumber.domain.model

import com.jarrlyyy.guessthenumber.BuildConfig
import com.jarrlyyy.guessthenumber.shared.domain.AchievementDef as SharedAchievementDef
import com.jarrlyyy.guessthenumber.shared.domain.GameSettings as SharedGameSettings
import com.jarrlyyy.guessthenumber.shared.domain.GameState as SharedGameState
import com.jarrlyyy.guessthenumber.shared.domain.GameStatistics as SharedGameStatistics
import com.jarrlyyy.guessthenumber.shared.domain.ShopItemDef as SharedShopItemDef
import com.jarrlyyy.guessthenumber.shared.domain.UpgradeDef as SharedUpgradeDef

/** Compatibility aliases keep existing Android UI and persistence call sites stable. */
typealias GameState = SharedGameState
typealias GameStatistics = SharedGameStatistics
typealias GameSettings = SharedGameSettings
typealias UpgradeDef = SharedUpgradeDef
typealias ShopItemDef = SharedShopItemDef
typealias AchievementDef = SharedAchievementDef

val DEFAULT_MORE_SCREEN_ORDER = buildList {
    val sharedOrder = com.jarrlyyy.guessthenumber.shared.domain.DEFAULT_MORE_SCREEN_ORDER
    addAll(sharedOrder.take(15))
    if (BuildConfig.DEBUG) addAll(listOf("dev_settings", "lua_testing_hub"))
    addAll(sharedOrder.drop(15))
}
