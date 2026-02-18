package org.beobma.prccore.tool

import net.kyori.adventure.text.minimessage.MiniMessage
import org.beobma.prccore.manager.ToolManager.setMaxCustomDurability
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class Weapon {
    val pipe = ItemStack(Material.WOODEN_SHOVEL, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("파이프"))
        meta.lore(
            listOf(MiniMessage.miniMessage().deserialize("<gray>무기로는 형편없지만, 쓸만하다."))
        )
        meta.setCustomModelData(10)
        itemMeta = meta
    }

    val knife = ItemStack(Material.WOODEN_SHOVEL, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("나이프"))
        meta.lore(
            listOf(MiniMessage.miniMessage().deserialize("<gray>적을 쓰러트리기에 적절한 무기."))
        )
        meta.setCustomModelData(11)
        itemMeta = meta
    }

    val longsword = ItemStack(Material.WOODEN_SHOVEL, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("장도"))
        meta.lore(
            listOf(MiniMessage.miniMessage().deserialize("<gray>긴 사거리를 가진 장도."))
        )
        meta.setCustomModelData(12)
        itemMeta = meta
    }

    init {
        pipe.setMaxCustomDurability(150)
        knife.setMaxCustomDurability(250)
        longsword.setMaxCustomDurability(750)
    }

    val weapons = hashSetOf(pipe, knife, longsword)
}