package org.beobma.prccore.tool

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class Capsule {
    val capsuleGun = ItemStack(Material.WOODEN_SHOVEL, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("캡슐건"))
        meta.lore(
            listOf(
                MiniMessage.miniMessage().deserialize("<gray>왼손에 든 캡슐을 소모하여 작물에 사용한다. 한 식물에 여러 캡슐을 사용할 수는 없다.")
            )
        )
        meta.setCustomModelData(9)
        itemMeta = meta
    }

    val weedKillerCapsule = ItemStack(Material.ORANGE_DYE, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("제초 캡슐"))
        meta.lore(
            listOf(
                MiniMessage.miniMessage().deserialize("<gray>제초제 성분이 담긴 캡슐.")
            )
        )
        meta.setCustomModelData(2)
        itemMeta = meta
    }

    val growthCapsule = ItemStack(Material.ORANGE_DYE, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("성장 캡슐"))
        meta.lore(
            listOf(
                MiniMessage.miniMessage().deserialize("<gray>성장 촉진제 성분이 담긴 캡슐, 식물에 사용하면 수확일 까지 시간을 줄여준다.")
            )
        )
        meta.setCustomModelData(3)
        itemMeta = meta
    }

    val nutrientCapsule = ItemStack(Material.ORANGE_DYE, 1).apply {
        val meta = itemMeta ?: return@apply
        meta.displayName(MiniMessage.miniMessage().deserialize("영양 캡슐"))
        meta.lore(
            listOf(
                MiniMessage.miniMessage().deserialize("<gray>비료 성분이 담긴 캡슐, 식물에 사용하면 더 높은 등급의 작물이 나올 확률이 증가한다.")
            )
        )
        meta.setCustomModelData(4)
        itemMeta = meta
    }

    val capsules = hashSetOf(weedKillerCapsule, nutrientCapsule, growthCapsule)
}