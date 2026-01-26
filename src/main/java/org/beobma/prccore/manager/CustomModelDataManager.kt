package org.beobma.prccore.manager

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object CustomModelDataManager {
    fun ItemStack.getCustomModelData(): Int {
        val meta = itemMeta ?: return 0
        if (!meta.hasCustomModelData()) return 0
        return meta.customModelData
    }

    fun ItemStack.hasCustomModelData(customModelData: Int, expectedType: Material): Boolean {
        if (type != expectedType) return false
        return getCustomModelData() == customModelData
    }

    fun ItemStack.hasCustomModelData(customModelDatas: IntArray, expectedType: Material): Boolean {
        if (type != expectedType) return false
        return getCustomModelData() in customModelDatas
    }

    fun ItemStack.matchesItemModel(other: ItemStack): Boolean {
        if (type != other.type) return false
        return getCustomModelData() == other.getCustomModelData()
    }
}