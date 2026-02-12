package org.beobma.prccore.plant.list

import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.util.Season
import org.bukkit.potion.PotionEffect

class CabbagePlant : Plant("양배추", 7, 7, 1, 1, listOf(Season.Spring)), EatablePlants {
    override val silverNutrition: Int = 10
    override val goldNutrition: Int = 12
    override val titaniumNutrition: Int = 15

    override val silverSaturation: Float = 10f
    override val goldSaturation: Float = 12f
    override val titaniumSaturation: Float = 14f

    override val silverConsumeSeconds: Float = 4.0f
    override val goldConsumeSeconds: Float = 3.5f
    override val titaniumConsumeSeconds: Float = 3.0f

    override val silverEffects: List<PotionEffect> = listOf()
    override val goldEffects: List<PotionEffect> = listOf()
    override val titaniumEffects: List<PotionEffect> = listOf()
}