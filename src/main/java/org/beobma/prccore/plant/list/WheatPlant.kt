package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.potion.PotionEffect

class WheatPlant : Plant("밀", 9, 9, 3, 4, listOf(Season.Summer, Season.Autumn)), EatablePlants {
    override val silverNutrition: Int = 2
    override val goldNutrition: Int = 2
    override val titaniumNutrition: Int = 3

    override val silverSaturation: Float = 0.3f
    override val goldSaturation: Float = 0.5f
    override val titaniumSaturation: Float = 0.7f

    override val silverConsumeSeconds: Float = 4f
    override val goldConsumeSeconds: Float = 4f
    override val titaniumConsumeSeconds: Float = 4f

    override val silverEffects: List<PotionEffect> = listOf()
    override val goldEffects: List<PotionEffect> = listOf()
    override val titaniumEffects: List<PotionEffect> = listOf()
}