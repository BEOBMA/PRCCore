package org.beobma.prccore.manager

import org.beobma.prccore.PrcCore
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

object ToolManager {
    private val instance = PrcCore.instance

    /** 괭이 */
    val HOE_CUSTOM_MODEL_DATAS: IntArray = intArrayOf(
        DURABLE_HOE_CUSTOM_MODEL_DATA,
        LIGHT_AND_STURDY_HOE_CUSTOM_MODEL_DATA,
        AUTO_HOE_CUSTOM_MODEL_DATA
    )

    /** 물뿌리개 */
    val WATERINGCAN_CUSTOM_MODEL_DATAS: IntArray = intArrayOf(
        WATERINGCAN_CUSTOM_MODEL_DATA,
        PUMP_WATERINGCAN_CUSTOM_MODEL_DATA
    )

    /** 캡슐 */
    val CAPSULE_MODEL_DATAS: IntArray = intArrayOf(
        WEED_KILLER_CAPSULE_MODEL_DATA,
        GROWTH_CAPSULE_MODEL_DATA,
        NUTRIENT_CAPSULE_MODEL_DATA
    )

    /** 곡괭이/드릴 */
    val PICKAXE_MODEL_DATAS: IntArray = intArrayOf(
        PICKAXE_MODEL_DATA,
        LIGHTDRILL_MODEL_DATA,
        HEAVYDRILL_MODEL_DATA
    )

    /** 도구 커스텀 모델 데이터 */
    const val DURABLE_HOE_CUSTOM_MODEL_DATA: Int = 4
    const val LIGHT_AND_STURDY_HOE_CUSTOM_MODEL_DATA: Int = 5
    const val AUTO_HOE_CUSTOM_MODEL_DATA: Int = 6

    const val WATERINGCAN_CUSTOM_MODEL_DATA: Int = 7
    const val PUMP_WATERINGCAN_CUSTOM_MODEL_DATA: Int = 8

    const val CAPSULEGUN_CUSTOM_MODEL_DATA: Int = 9
    const val WEED_KILLER_CAPSULE_MODEL_DATA: Int = 2
    const val GROWTH_CAPSULE_MODEL_DATA: Int = 3
    const val NUTRIENT_CAPSULE_MODEL_DATA: Int = 4

    const val PICKAXE_MODEL_DATA: Int = 1
    const val LIGHTDRILL_MODEL_DATA: Int = 2
    const val HEAVYDRILL_MODEL_DATA: Int = 3

    /** PDC 키 정의 */
    private val KEY_MAX = NamespacedKey(instance, "custom_durability_max")
    private val KEY_CUR = NamespacedKey(instance, "custom_durability_cur")

    /** 최대 내구도 설정 / 내구도 초기화 */
    fun ItemStack.setMaxCustomDurability(max: Int) {
        val meta = itemMeta ?: return
        if (meta !is Damageable) return
        val container = meta.persistentDataContainer
        container.set(KEY_MAX, PersistentDataType.INTEGER, max)
        container.set(KEY_CUR, PersistentDataType.INTEGER, max)
        itemMeta = meta
    }

    /** 최대 내구도 반환 */
    fun ItemStack.getMaxCustomDurability(): Int? {
        val meta = itemMeta ?: return null
        val container = meta.persistentDataContainer
        return container.get(KEY_MAX, PersistentDataType.INTEGER)
    }

    /** 현재 내구도 반환 */
    fun ItemStack.getCurrentCustomDurability(): Int? {
        val meta = itemMeta ?: return null
        val container = meta.persistentDataContainer
        return container.get(KEY_CUR, PersistentDataType.INTEGER)
    }

    /** 내구도 감소 처리 */
    fun ItemStack.decreaseCustomDurability(amount: Int, player: Player) {
        val meta = itemMeta ?: return
        if (meta !is Damageable) return
        val container = meta.persistentDataContainer

        val max = container.get(KEY_MAX, PersistentDataType.INTEGER) ?: return
        val current = container.get(KEY_CUR, PersistentDataType.INTEGER) ?: return

        val newDurability = (current - amount).coerceAtLeast(0)
        container.set(KEY_CUR, PersistentDataType.INTEGER, newDurability)

        val vanillaMax = type.maxDurability.toInt()
        val visualDamage = ((1.0 - newDurability.toDouble() / max.toDouble()) * vanillaMax).toInt()
        meta.damage = visualDamage.coerceAtMost(vanillaMax)

        itemMeta = meta

        if (newDurability <= 0) {
            destruction(player)
        }
    }

    /** 파괴 처리 */
    private fun ItemStack.destruction(player: Player) {
        player.inventory.remove(this)
    }
}