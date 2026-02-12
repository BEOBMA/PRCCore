package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.potion.PotionEffect

class CoffeeBeansPlant : Plant("커피콩", 10, 10, 10, 10, listOf(Season.Spring, Season.Summer)), EatablePlants {
    override val silverNutrition: Int = 1
    override val goldNutrition: Int = 1
    override val titaniumNutrition: Int = 2

    override val silverSaturation: Float = 0.2f
    override val goldSaturation: Float = 0.4f
    override val titaniumSaturation: Float = 0.5f

    override val silverConsumeSeconds: Float = 0.8f
    override val goldConsumeSeconds: Float = 0.8f
    override val titaniumConsumeSeconds: Float = 0.8f

    override val silverEffects: List<PotionEffect> = listOf()
    override val goldEffects: List<PotionEffect> = listOf()
    override val titaniumEffects: List<PotionEffect> = listOf()
}