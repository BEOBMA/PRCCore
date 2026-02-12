package org.beobma.prccore.manager

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import kr.eme.prcMission.api.events.MissionEvent
import kr.eme.prcMission.enums.MissionVersion
import org.beobma.prccore.manager.AdvancementManager.addAdvancementInt
import org.beobma.prccore.manager.CustomModelDataManager.getCustomModelData
import org.beobma.prccore.manager.CustomModelDataManager.hasCustomModelData
import org.beobma.prccore.manager.CustomModelDataManager.matchesItemModel
import org.beobma.prccore.manager.DataManager.interactionFarmlands
import org.beobma.prccore.manager.DataManager.plantList
import org.beobma.prccore.manager.PlantManager.PLANT_STAR_ICON_OFFSET
import org.beobma.prccore.manager.PlantManager.getHarvestItem
import org.beobma.prccore.manager.PlantManager.getItemDisplay
import org.beobma.prccore.manager.PlantManager.getPlantInstance
import org.beobma.prccore.manager.PlantManager.getRegisterPlants
import org.beobma.prccore.manager.PlantManager.getSeedItem
import org.beobma.prccore.manager.PlantManager.plantAgIcons
import org.beobma.prccore.manager.PlantManager.plantModels
import org.beobma.prccore.manager.PlantManager.plantSeedIcons
import org.beobma.prccore.manager.ToolManager.AUTO_HOE_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.CAPSULEGUN_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.CAPSULE_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.DURABLE_HOE_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.GROWTH_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.HOE_CUSTOM_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.LIGHT_AND_STURDY_HOE_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.NUTRIENT_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.PUMP_WATERINGCAN_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.WATERINGCAN_CUSTOM_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.WATERINGCAN_CUSTOM_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.WEED_KILLER_CAPSULE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.decreaseCustomDurability
import org.beobma.prccore.plant.EatablePlants
import org.beobma.prccore.plant.Plant
import org.beobma.prccore.plant.list.WeedPlant
import org.beobma.prccore.tool.CapsuleType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Farmland
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

object FarmingManager {
    private val NEIGHBOR_OFFSETS: List<Pair<Int, Int>> = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1,  0 to 0,  0 to 1,
        1 to -1,  1 to 0,  1 to 1
    )
    private val NEIGHBOR_OFFSETS_2X2: List<Pair<Int, Int>> = listOf(
        0 to 0, 1 to 0,
        0 to 1, 1 to 1
    )
    private const val WEED_CHANCE_PERCENT = 4
    private const val MODEL_WEED = 41
    private const val MODEL_DEAD_GRASS = 42
    private const val WATER_PARTICLE = 10

    /** 3×3 */
    private inline fun forEach3x3(origin: Block, crossinline action: (Block) -> Unit) {
        val base = origin.location
        for ((dx, dz) in NEIGHBOR_OFFSETS) {
            action(base.clone().add(dx.toDouble(), 0.0, dz.toDouble()).block)
        }
    }

    /** 2×2 */
    private inline fun forEach2x2(origin: Block, crossinline action: (Block) -> Unit) {
        val base = origin.location
        for ((dx, dz) in NEIGHBOR_OFFSETS_2X2) {
            action(base.clone().add(dx.toDouble(), 0.0, dz.toDouble()).block)
        }
        }

    /** 경작지 변환 / 하부 경작지 반환 */
    private fun asFarmlandOrBelow(block: Block): Farmland? {
        (block.blockData as? Farmland)?.let { return it }
        return (block.getRelative(BlockFace.DOWN).blockData as? Farmland)
    }

    /** 경작지 수분 최대 갱신 및 상호작용 등록 */
    private fun moistenFarmland(block: Block) {
        val (farmlandBlock, farmland) = when (val self = block.blockData as? Farmland) {
            null -> {
                val below = block.getRelative(BlockFace.DOWN)
                val belowFarmland = below.blockData as? Farmland ?: return
                below to belowFarmland
            }
            else -> block to self
        }

        if (farmland.moisture < farmland.maximumMoisture) {
            farmland.moisture = farmland.maximumMoisture
            farmlandBlock.blockData = farmland
            interactionFarmlands.add(farmlandBlock.location)
        }
    }

    /** 아이템 디스플레이 갱신 */
    private fun updateDisplayStage(plant: Plant, registered: Any?, isHarvestComplete: Boolean, progress: Double) {
        val baseModel = registered?.let { plantModels[it] } ?: return
        val display = plant.getItemDisplay() ?: return
        val stage = when {
            isHarvestComplete -> 3
            progress >= 0.66  -> 2
            progress >= 0.33  -> 1
            else              -> 0
        }
        display.setItemStack(
            display.itemStack.apply {
                itemMeta = itemMeta.apply { setCustomModelData(baseModel + stage) }
            }
        )
    }

    /** 수확물 품질(일반/금/이리듐) */
    private fun rollQualityCmd(base: Int, iridiumChance: Int, goldChance: Int, plant: Plant): Int {
        val r = Random.nextInt(1, 101)
        return when {
            r <= iridiumChance              -> {
                plant.quality = 2
                base + (PLANT_STAR_ICON_OFFSET * 2)
            }
            r <= iridiumChance + goldChance -> {
                plant.quality = 1
                base + (PLANT_STAR_ICON_OFFSET)
            }
            else                            -> {
                plant.quality = 0
                base
            }
        }
    }

    /** 미션 */
    private fun Player.fireMission(missionVersion: MissionVersion, type: String, module: String, value: Int = 1) {
        Bukkit.getPluginManager().callEvent(
            MissionEvent(this, missionVersion, type, module, value)
        )
    }

    /** 밭 갈기 */
    fun Player.tillage(block: Block) {
        val main = inventory.itemInMainHand
        val cmd = main.getCustomModelData()
        if (block.type != Material.DIRT) return
        if (!main.hasCustomModelData(HOE_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL)) return

        when (cmd) {
            DURABLE_HOE_CUSTOM_MODEL_DATA -> convertToFarmland(block)
            LIGHT_AND_STURDY_HOE_CUSTOM_MODEL_DATA -> {
                forEach2x2(block) { b -> if (b.type == Material.DIRT) convertToFarmland(b) }
            }
            AUTO_HOE_CUSTOM_MODEL_DATA -> autoHoeHandler(block, this)
        }

        main.decreaseCustomDurability(1, this)
    }

    /** 흙 → 경작지 변환 + 상호작용 등록 */
    private fun Player.convertToFarmland(block: Block) {
        block.type = Material.FARMLAND
        interactionFarmlands.add(block.location)
        fireMission(MissionVersion.V2, "PLAYER_PROGRESS", "farming_module", 1)
        // 300회 경작 미션 문제
        // 300회 카운트를 어느 플러그인에서 해야할지.
    }

    /** 자동화 괭이 */
    private fun Player.autoHoeHandler(block: Block, player: Player) {
        val registry = getRegisterPlants()
        val off = player.inventory.itemInOffHand
        val plantType = registry.find { off.matchesItemModel(it.getSeedItem()) }

        forEach3x3(block) { target ->
            if (target.type != Material.DIRT) return@forEach3x3
            convertToFarmland(target)
            if (plantType != null && off.amount > 0) {
                val instance = getPlantInstance(plantType)
                player.plant(target, instance)
            }
        }
    }

    /** 물 주기 */
    fun Player.watering(block: Block) {
        val hand = inventory.itemInMainHand
        val cmd = hand.getCustomModelData()
        if (!hand.hasCustomModelData(WATERINGCAN_CUSTOM_MODEL_DATAS, Material.WOODEN_SHOVEL)) return

        when (cmd) {
            WATERINGCAN_CUSTOM_MODEL_DATA -> {
                world.spawnParticle(Particle.FALLING_WATER, block.location.clone().add(0.5, 1.0, 0.5), WATER_PARTICLE, 0.1, 0.1, 0.1, 1.0)
                fireMission(MissionVersion.V1,"FARMING", "farming_module", 1)
                moistenFarmland(block)
            }
            PUMP_WATERINGCAN_CUSTOM_MODEL_DATA -> {
                forEach3x3(block) { b ->
                    world.spawnParticle(Particle.FALLING_WATER, b.location.clone().add(0.5, 1.0, 0.5), WATER_PARTICLE, 0.1, 0.1, 0.1, 1.0)
                    fireMission(MissionVersion.V1,"FARMING", "farming_module", 1)
                    moistenFarmland(b)
                }
            }
        }

        world.playSound(block.location, Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f)
        hand.decreaseCustomDurability(1, this)
    }

    /** 심기 */
    fun Player.plant(block: Block, plant: Plant) {
        val registry = getRegisterPlants()

        // 손/보조손 씨앗 판별
        var item = inventory.itemInMainHand
        var cmd = item.getCustomModelData()
        var registered = registry.find { item.matchesItemModel(it.getSeedItem()) }
        if (registered == null) {
            item = inventory.itemInOffHand
            cmd = item.getCustomModelData()
            registered = registry.find { item.matchesItemModel(it.getSeedItem()) }
        }
        if (registered == null) return

        // 중복 방지
        if (plantList.any { it.farmlandLocation == block.location }) return

        // 잡초
        if (registered is WeedPlant) {
            plant.plantStatus.isWeeds = true
        } else if (Random.nextInt(100) < WEED_CHANCE_PERCENT) {
            plant.plantStatus.isWeeds = true
        }

        // 상태
        plant.farmlandLocation = block.location
        plant.plantStatus.isPlant = true

        // 씨앗 소모
        if (cmd == plantSeedIcons[registered]) item.amount--

        // 디스플레이
        plantList.add(plant)
        val display = world.spawn(block.location.add(0.5, 1.4, 0.5), ItemDisplay::class.java)
        display.isInvulnerable = true
        plant.uuidString = display.uniqueId.toString()

        val model = if (plant.plantStatus.isWeeds) MODEL_WEED else (plantModels[registered] ?: MODEL_WEED)
        val stack = ItemStack(Material.BLUE_DYE).apply {
            itemMeta = itemMeta.apply { setCustomModelData(model) }
        }
        display.setItemStack(stack)

        playSound(block.location, Sound.ITEM_HOE_TILL, 1.0f, 1.0f)
    }

    /** 식물 제거 + 디스플레이 삭제 */
    fun Player.removePlant(plant: Plant) {
        plant.farmlandLocation?.let { playSound(it, Sound.ITEM_HOE_TILL, 1.0f, 1.0f) }
        plantList.remove(plant)
        plant.getItemDisplay()?.remove()
    }

    /** 시듬 */
    private fun Plant.wither() {
        plantStatus.isDeadGrass = true
        val display = getItemDisplay() ?: return
        val stack = ItemStack(Material.BLUE_DYE).apply {
            itemMeta = itemMeta.apply { setCustomModelData(MODEL_DEAD_GRASS) }
        }
        display.setItemStack(stack)
    }

    /** 수확 */
    fun Player.harvesting(plant: Plant) {
        val status = plant.plantStatus
        if (status.isDeadGrass) { removePlant(plant); return }
        if (!status.isHarvestComplete || !status.isPlant) return

        val registered = getRegisterPlants()
            .find { it.getSeedItem().matchesItemModel(plant.getSeedItem()) }
        val baseCmd = plantAgIcons[registered] ?: return

        // 등급 확률 계산
        val isNutrient = (status.capsuleType == CapsuleType.Nutrient)
        val iridiumChance = if (isNutrient) 30 else 0
        val goldChance = if (isNutrient) 70 else 30

        fun addOneHarvest() {
            val item = plant.getHarvestItem()
            val finalCmd = rollQualityCmd(baseCmd, iridiumChance, goldChance, plant)
            item.itemMeta = item.itemMeta.apply { setCustomModelData(finalCmd) }
            val eatablePlants = registered as? EatablePlants ?: return
            val quality = plant.quality ?: EatablePlants.QUALITY_SILVER

            val food = FoodProperties.food()
                .nutrition(eatablePlants.getNutrition(quality))
                .saturation(eatablePlants.getSaturation(quality))
                .build()
            val effects = eatablePlants.getEffects(quality)
            val consumable = Consumable.consumable()
                .consumeSeconds(eatablePlants.getConsumeSeconds(quality))
                .animation(ItemUseAnimation.EAT)
                .addEffect(ConsumeEffect.applyStatusEffects(effects, 1.0f))
                .build()

            item.setData(DataComponentTypes.FOOD, food)
            item.setData(DataComponentTypes.CONSUMABLE, consumable)

            inventory.addItem(item)

            // 작물 200개 수확
            addAdvancementInt(this, "module/normal/frederick_the_great", 200)
        }

        if (plant.harvestAmountMin != plant.harvestAmountMax) {
            repeat(Random.nextInt(plant.harvestAmountMin, plant.harvestAmountMax + 1)) { addOneHarvest() }
            removePlant(plant)
            return
        }

        repeat(plant.harvestAmountMax) { addOneHarvest() }
        removePlant(plant)
    }

    /** 캡슐 */
    fun Player.capsule(plant: Plant) {
        val status = plant.plantStatus
        val hand = inventory.itemInMainHand
        val off = inventory.itemInOffHand
        val offCmd = off.getCustomModelData()

        if (plant.plantStatus.isDeadGrass) return
        if (!hand.hasCustomModelData(CAPSULEGUN_CUSTOM_MODEL_DATA, Material.WOODEN_SHOVEL)) return
        if (!off.hasCustomModelData(CAPSULE_MODEL_DATAS, Material.ORANGE_DYE)) return
        if (status.isHarvestComplete || !status.isPlant) return
        if (status.capsuleType != CapsuleType.None) return
        if (offCmd == WEED_KILLER_CAPSULE_MODEL_DATA && plant.plantStatus.isWeeds) {
            removePlant(plant)
        }

        status.capsuleType = when (offCmd) {
            GROWTH_CAPSULE_MODEL_DATA      -> CapsuleType.Growth
            NUTRIENT_CAPSULE_MODEL_DATA    -> CapsuleType.Nutrient
            WEED_KILLER_CAPSULE_MODEL_DATA -> CapsuleType.None
            else                           -> CapsuleType.None
        }

        plant.farmlandLocation?.let {
            spawnParticle(Particle.END_ROD, it.clone().add(0.5, 1.0, 0.5), 10, 0.05, 0.0, 0.05, 0.01)
            playSound(it, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
        }
        if (off.amount > 0) off.amount -= 1

        // 미션
        fireMission(MissionVersion.V2, "FARMING", "farming_module", 1)
    }

    /** 물 주기 여부 */
    fun Plant.isWatering(): Boolean {
        val f = farmlandLocation?.block?.blockData as? Farmland ?: return false
        return f.moisture == f.maximumMoisture
    }

    /** 성장 */
    fun Plant.growth() {
        val status = plantStatus
        if (!status.isPlant) return

        val fLoc = farmlandLocation ?: return
        val farmland = fLoc.block.blockData as? Farmland ?: return

        // 계절 시스템(보류)

        if (status.isHarvestComplete) return

        val world = fLoc.world
        val baseX = fLoc.blockX
        val baseY = fLoc.blockY
        val baseZ = fLoc.blockZ

        fun advance(accelerated: Boolean) {
            if (!isWatering()) return
            val done = if (accelerated) { --remainingGrowthDays <= 1 } else { --remainingGrowthDays <= 0 }
            if (done) status.isHarvestComplete = true

            val progress = (growthDays - remainingGrowthDays).toDouble() / growthDays
            val registered = getRegisterPlants()
                .find { it.getSeedItem().matchesItemModel(getSeedItem()) }

            updateDisplayStage(this, registered, status.isHarvestComplete, progress)
            status.weedsCount = 0
        }

        var weedFound = false
        var nonWeedFound = false

        loop@ for (x in (baseX - 1)..(baseX + 1)) {
            for (z in (baseZ - 1)..(baseZ + 1)) {
                val cur = world.getBlockAt(x, baseY, z)
                if (cur.type != Material.FARMLAND) continue
                val adj = plantList.find { it.farmlandLocation?.block == cur } ?: continue

                if (adj.plantStatus.isWeeds) {
                    weedFound = true
                    break@loop
                } else {
                    nonWeedFound = true
                }
            }
        }

        when {
            weedFound -> {
                status.weedsCount++
                if (status.weedsCount > 2) wither()
            }

            nonWeedFound -> {
                // Growth 캡슐 가속 반영
                advance(accelerated = (status.capsuleType == CapsuleType.Growth))
            }

            else -> { /* 인접 식물 없음 */
            }
        }


        // 수분 소모
        farmland.moisture = 0
        fLoc.block.blockData = farmland
    }
}