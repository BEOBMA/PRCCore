package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.potion.PotionEffect

class PumpkinPlant : Plant("호박", 8, 8, 1, 1, listOf(Season.Autumn)), EatablePlants {
    override val silverNutrition: Int = 12
    override val goldNutrition: Int = 16
    override val titaniumNutrition: Int = 20

    override val silverSaturation: Float = 15f
    override val goldSaturation: Float = 15f
    override val titaniumSaturation: Float = 15f

    override val silverConsumeSeconds: Float = 4f
    override val goldConsumeSeconds: Float = 3.5f
    override val titaniumConsumeSeconds: Float = 3f

    override val silverEffects: List<PotionEffect> = listOf()
    override val goldEffects: List<PotionEffect> = listOf()
    override val titaniumEffects: List<PotionEffect> = listOf()
}