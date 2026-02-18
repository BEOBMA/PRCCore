package org.beobma.prccore.manager

import kr.eme.prcShop.api.PRCItem
import org.beobma.prccore.manager.UtilManager.miniMessage
import org.beobma.prccore.plant.Plant
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import java.util.*

object PlantManager {
    const val PLANT_STAR_ICON_OFFSET: Int = 10

    private val plantFactories: MutableMap<Plant, () -> Plant> = mutableMapOf()
    val plantPRCSeedItemMap: MutableMap<Plant, PRCItem> = mutableMapOf()
    val plantPRCG1ItemMap: MutableMap<Plant, PRCItem> = mutableMapOf()
    val plantPRCG2ItemMap: MutableMap<Plant, PRCItem> = mutableMapOf()
    val plantPRCG3ItemMap: MutableMap<Plant, PRCItem> = mutableMapOf()
    val plantSeedIcons: MutableMap<Plant, Int> = mutableMapOf()
    val plantAgIcons: MutableMap<Plant, Int> = mutableMapOf()
    val plantModels: MutableMap<Plant, Int> = mutableMapOf()

    /** 식물 등록 */
    fun Plant.register(clazz: Class<out Plant>, seedIconCustomModelData: Int, agIconCustomModelData: Int, modelData: Int, prcSeedItem: PRCItem, prcG1Item: PRCItem, prcG2Item: PRCItem, prcG3Item: PRCItem) {
        plantFactories[this] = { clazz.getDeclaredConstructor().newInstance() }
        plantSeedIcons[this] = seedIconCustomModelData
        plantAgIcons[this] = agIconCustomModelData
        plantModels[this] = modelData
        plantPRCSeedItemMap[this] = prcSeedItem
        plantPRCG1ItemMap[this] = prcG1Item
        plantPRCG2ItemMap[this] = prcG2Item
        plantPRCG3ItemMap[this] = prcG3Item
    }

    /** 씨앗 아이템 생성 */
    fun Plant.getSeedItem(): ItemStack {
        val registeredPlant = getRegisteredPlant(this) ?: return ItemStack(Material.AIR)
        val prcItem = plantPRCSeedItemMap[registeredPlant] ?: return ItemStack(Material.AIR)
        val itemStack = prcItem.create()
        return itemStack
    }

    /** 수확물 아이템 생성 */
    fun Plant.getHarvestItem(): ItemStack {
        val registeredPlant = getRegisteredPlant(this) ?: return ItemStack(Material.AIR)
        val prcItem = plantPRCG1ItemMap[registeredPlant] ?: return ItemStack(Material.AIR)
        val itemStack = prcItem.create()
        return itemStack
    }

    /** 인스턴스 생성 */
    fun getPlantInstance(plant: Plant): Plant {
        return plantFactories[plant]!!.invoke()
    }

    /** 등록된 식물 원형 조회 (클래스 기준) */
    fun getRegisteredPlant(plant: Plant): Plant? {
        return plantFactories.keys.find { it::class.java == plant::class.java }
    }

    /** 등록 식물 목록 */
    fun getRegisterPlants(): List<Plant> {
        return plantFactories.map { it.key }
    }

    /** 아이템 디스플레이 조회 */
    fun Plant.getItemDisplay(): ItemDisplay? {
        val uuid = UUID.fromString(uuidString)
        val entity = Bukkit.getEntity(uuid)
        if (entity !is ItemDisplay) return null
        return entity
    }
}