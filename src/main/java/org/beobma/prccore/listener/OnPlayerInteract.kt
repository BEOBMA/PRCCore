package org.beobma.prccore.listener

import io.papermc.paper.datacomponent.DataComponentTypes
import org.beobma.prccore.PrcCore
import org.beobma.prccore.manager.CustomModelDataManager.getCustomModelData
import org.beobma.prccore.manager.CustomModelDataManager.hasCustomModelData
import org.beobma.prccore.manager.CustomModelDataManager.matchesItemModel
import org.beobma.prccore.manager.DataManager
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.DataManager.plantList
import org.beobma.prccore.manager.FarmingManager.capsule
import org.beobma.prccore.manager.FarmingManager.harvesting
import org.beobma.prccore.manager.FarmingManager.isWatering
import org.beobma.prccore.manager.FarmingManager.plant
import org.beobma.prccore.manager.FarmingManager.removePlant
import org.beobma.prccore.manager.FarmingManager.tillage
import org.beobma.prccore.manager.FarmingManager.watering
import org.beobma.prccore.manager.MineManager.approach
import org.beobma.prccore.manager.MineManager.gathering
import org.beobma.prccore.manager.MineManager.showMineFloorSelector
import org.beobma.prccore.manager.PlantManager.getItemDisplay
import org.beobma.prccore.manager.PlantManager.getPlantInstance
import org.beobma.prccore.manager.PlantManager.getRegisterPlants
import org.beobma.prccore.manager.PlantManager.getSeedItem
import org.beobma.prccore.manager.PlantManager.plantAgIcons
import org.beobma.prccore.manager.PlantManager.plantModels
import org.beobma.prccore.manager.TimeManager
import org.beobma.prccore.manager.ToolManager.CAPSULEGUN_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.CAPSULE_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.GROWTH_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.HOE_CUSTOM_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.NUTRIENT_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.WATERINGCAN_CUSTOM_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.WEED_KILLER_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.decreaseCustomDurability
import org.beobma.prccore.manager.ToolManager.getCurrentCustomDurability
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.tool.Capsule
import org.beobma.prccore.tool.CapsuleType
import org.beobma.prccore.tool.Hoe
import org.beobma.prccore.tool.Pickaxe
import org.beobma.prccore.tool.WateringCan
import org.bukkit.Bukkit.getEntity
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class OnPlayerInteract : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val item = event.item
        val cmd = item?.getCustomModelData()
        val plantAtBlock = plantList.find { it.farmlandLocation == block.location }
        val isHoe = item?.hasCustomModelData(HOE_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL) ?: false

        if (!isHoe && block.type == Material.DIRT) {
            event.isCancelled = true
            return
        }

        // 디버그 아이템
        if (item?.type == Material.CLOCK) {
            TimeManager.endOfDay()
            event.isCancelled = true
            return
        }
        // 디버그 아이템
        if (item?.type == Material.BUCKET) {
            plantList.toList().forEach { player.removePlant(it) }
            event.isCancelled = true
            return
        }
        // 디버그 아이템
        if (item?.type == Material.TOTEM_OF_UNDYING) {
            showMineFloorSelector(player)
            event.isCancelled = true
            return
        }
        // 디버그 아이템
        if (item?.type == Material.BUNDLE) {
            val inventory = event.player.inventory
            Hoe().hoes.forEach {
                inventory.addItem(it)
            }

            val capsule = Capsule()
            inventory.addItem(capsule.capsuleGun)
            capsule.capsules.forEach {
                inventory.addItem(it)
            }

            WateringCan().wateringCans.forEach {
                inventory.addItem(it)
            }

            Pickaxe().pickaxes.forEach {
                inventory.addItem(it)
            }

            getRegisterPlants().forEach {
                inventory.addItem(it.getSeedItem())
            }
            event.isCancelled = true
            return
        }

        // 광산
        if (mines.any { it.players.contains(player) }) {
            handleMine(player, block)
            event.isCancelled = true
            return
        }

        // 경작
        if (cmd != null && isHoe && block.type == Material.DIRT) {
            player.tillage(block)
            event.isCancelled = true
            return
        }

        // 물 주기
        if (cmd != null && item.hasCustomModelData(WATERINGCAN_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL) &&
            (block.type == Material.FARMLAND || block.type == Material.WHEAT)
        ) {
            player.watering(block)
            event.isCancelled = true
            return
        }

        // 식물 상호작용 처리
        plantAtBlock?.let {
            handlePlantInteraction(player, it, item, block)
            event.isCancelled = true
            return
        }

        // 심기

        if (item == null) return
        if (item.type != Material.BLACK_DYE) return

        val registered = getRegisterPlants()
            .find { item.matchesItemModel(it.getSeedItem()) } ?: run {
            event.isCancelled = true; return
        }

        if (block.type != Material.FARMLAND) {
            event.isCancelled = true; return
        }
        if (plantList.any { it.farmlandLocation == block.location }) {
            event.isCancelled = true; return
        }

        val newPlant = getPlantInstance(registered)
        player.plant(block, newPlant)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val entity = event.rightClicked
        val main = player.inventory.itemInMainHand
        val cmd = main.getCustomModelData()

        // 광산 선택 인터렉션
        if (entity is Interaction) {
            val key = NamespacedKey("module", "mine_interaction")
            val pdc = entity.persistentDataContainer
            DataManager.mineExitLocation = entity.location
            if (pdc.getOrDefault(key, PersistentDataType.BOOLEAN, false)) {
                showMineFloorSelector(player)
                return
            }
        }

        if (entity !is ItemDisplay) return
        val plant = plantList.find { it.uuidString == entity.uniqueId.toString() } ?: return
        val farmland = plant.farmlandLocation ?: return

        // 물 주기
        if (main.hasCustomModelData(WATERINGCAN_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL)) {
            player.watering(farmland.block)
            event.isCancelled = true
            return
        }

        // 식물 상호작용 처리
        handlePlantInteraction(player, plant, main, farmland.block)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val tool = damager.inventory.itemInMainHand
        val durability = tool.getCurrentCustomDurability() ?: return
        if (durability <= 0) return
        tool.decreaseCustomDurability(1, damager)
    }

    /** 광산 상호작용 처리 */
    private fun handleMine(player: Player, block: Block) {
        val mine = mines.find { it.players.contains(player) } ?: return

        // 디버그 아이템
        if (player.inventory.itemInMainHand.type == Material.NETHERITE_INGOT) {
            player.approach(mine, mine.floor + 1)
            return
        }
        // 디버그 아이템
        if (player.inventory.itemInMainHand.type == Material.NETHERITE_SWORD) {
            mine.enemys.forEach { enemy ->
                val uuid = UUID.fromString(enemy.enemyUUID)
                val entity = getEntity(uuid) as? LivingEntity
                entity?.damage(9999.9, player)
            }
            return
        }
        when (block) {
            mine.exitBlockLocation?.block -> player.approach(mine, mine.floor + 1)
            mine.startBlockLocation?.block -> player.approach(mine, 0, true)
            else -> {
                if (player.inventory.itemInMainHand.type != Material.WOODEN_SHOVEL) return
                mines.find { it == mine }?.resources
                    ?.find { it.location.block == block }
                    ?.let { player.gathering(it) }
            }
        }
    }

    /** 식물 상호작용 처리 */
    private fun handlePlantInteraction(player: Player, plant: Plant, item: ItemStack?, block: Block) {
        val status = plant.plantStatus
        if (!status.isPlant) return

        val main = item
        val off = player.inventory.itemInOffHand
        val mainCmd = main?.getCustomModelData()
        val offCmd = off.getCustomModelData()

        // 빈손/무관 도구 상호작용
        if (mainCmd == null || main.isFoodLikeItem()) {
            when {
                status.isHarvestComplete -> player.harvesting(plant)
                status.isDeadGrass       -> player.removePlant(plant)
            }
            return
        }

        // 캡슐 상호작용
        if (off.hasCustomModelData(CAPSULE_MODEL_DATAS, Material.ORANGE_DYE)) {
            val canShoot = main.hasCustomModelData(CAPSULEGUN_CUSTOM_MODEL_DATA, Material.WOODEN_SHOVEL) &&
                    status.capsuleType == CapsuleType.None && (offCmd in CAPSULE_MODEL_DATAS)
            if (canShoot) player.capsule(plant)
            return
        }

        // 관개 상호작용
        if (main.hasCustomModelData(WATERINGCAN_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL)) {
            if (!plant.isWatering()) {
                player.watering(block)
            }
            return
        }
    }

    private fun ItemStack?.isFoodLikeItem(): Boolean {
        val stack = this ?: return false
        return stack.type.isEdible ||
                stack.hasData(DataComponentTypes.FOOD) ||
                stack.hasData(DataComponentTypes.CONSUMABLE)
    }
}