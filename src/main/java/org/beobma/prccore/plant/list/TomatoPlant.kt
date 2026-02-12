package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.potion.PotionEffect

class TomatoPlant : Plant("토마토", 3, 3, 3, 5, listOf(Season.Summer)), EatablePlants {
    override val silverNutrition: Int = 4
    override val goldNutrition: Int = 5
    override val titaniumNutrition: Int = 6

    override val silverSaturation: Float = 2f
    override val goldSaturation: Float = 4f
    override val titaniumSaturation: Float = 6f

    override val silverConsumeSeconds: Float = 1.6f
    override val goldConsumeSeconds: Float = 1.4f
    override val titaniumConsumeSeconds: Float = 1.25f

    override val silverEffects: List<PotionEffect> = listOf()
    override val goldEffects: List<PotionEffect> = listOf()
    override val titaniumEffects: List<PotionEffect> = listOf()
}