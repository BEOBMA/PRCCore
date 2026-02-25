package org.beobma.prccore.manager

import kr.eme.prcMission.api.events.MissionEvent
import kr.eme.prcMission.enums.MissionVersion
import net.kyori.adventure.text.minimessage.MiniMessage
import org.beobma.prccore.PrcCore
import org.beobma.prccore.entity.Enemy
import org.beobma.prccore.manager.AdvancementManager.grantAdvancement
import org.beobma.prccore.manager.CustomModelDataManager.hasCustomModelData
import org.beobma.prccore.manager.DataManager.gameData
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.ToolManager.HEAVYDRILL_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.LIGHTDRILL_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.PICKAXE_MODEL_DATA
import org.beobma.prccore.manager.ToolManager.PICKAXE_MODEL_DATAS
import org.beobma.prccore.manager.ToolManager.decreaseCustomDurability
import org.beobma.prccore.mine.Mine
import org.beobma.prccore.mine.MineTemplate
import org.beobma.prccore.mine.MineType
import org.beobma.prccore.resource.Resource
import org.beobma.prccore.resource.ResourceType
import org.beobma.prccore.resource.ResourceType.*
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Transformation
import org.example.hoon.coreframe.api.CoreFrameAPI
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random

object MineManager {
    private const val MAX_MINE_FLOOR = 60
    private const val CALCULATE_OFFSET = 480.0
    private const val TICKINTERVAL = 10L
    private const val MAX_GATHERING_DISTANCE_SQUARED = 40.0

    private val world = Bukkit.getWorlds().first()
    private val miniMessage = MiniMessage.miniMessage()

    val gatheringPlayers = mutableSetOf<UUID>()
    private val resourceInteractingPlayers = mutableMapOf<String, UUID>()

    /** 드랍/생성 가중치 단위 */
    private data class WeightedResource(val type: ResourceType?, val weight: Int)

    /** 층수별 가중치 룰 */
    private data class FloorResourceRule(val floors: IntRange, val entries: List<WeightedResource>)

    /**
     * 층수별 광산 자원 가중치.
     */
    private val floorResourceRules = listOf(
        FloorResourceRule(1..8, listOf(
            WeightedResource(Magnesium, 33),
            WeightedResource(Aluminum, 67)
        )),
        FloorResourceRule(9..16, listOf(
            WeightedResource(Magnesium, 31),
            WeightedResource(Aluminum, 30),
            WeightedResource(Iron, 40)
        )),
        FloorResourceRule(17..24, listOf(
            WeightedResource(Magnesium, 21),
            WeightedResource(Aluminum, 20),
            WeightedResource(Iron, 30),
            WeightedResource(Copper, 30)
        )),
        FloorResourceRule(25..32, listOf(
            WeightedResource(Magnesium, 1),
            WeightedResource(Aluminum, 1),
            WeightedResource(Iron, 29),
            WeightedResource(Copper, 20),
            WeightedResource(Lithium, 50)
        )),
        FloorResourceRule(33..40, listOf(
            WeightedResource(Iron, 31),
            WeightedResource(Copper, 30),
            WeightedResource(Lithium, 40)
        )),
        FloorResourceRule(41..46, listOf(
            WeightedResource(Magnesium, 16),
            WeightedResource(Aluminum, 15),
            WeightedResource(Iron, 30),
            WeightedResource(Gold, 20),
            WeightedResource(Platinum, 20)
        )),
        FloorResourceRule(47..52, listOf(
            WeightedResource(Magnesium, 16),
            WeightedResource(Aluminum, 15),
            WeightedResource(Iron, 30),
            WeightedResource(Platinum, 20),
            WeightedResource(Nickel, 20)
        )),
        FloorResourceRule(53..MAX_MINE_FLOOR, listOf(
            WeightedResource(Magnesium, 16),
            WeightedResource(Aluminum, 15),
            WeightedResource(Iron, 30),
            WeightedResource(Nickel, 20),
            WeightedResource(Titanium, 20)
        ))
    )

    /** 초기화 or 로드 */
    fun reset() {
        resourceInteractingPlayers.clear()
        if (mines.isNotEmpty()) {
            loadData()
            return
        }
        mines.forEach { mine ->
            mine.resources.forEach { resource -> resource.getItemDisplay()?.remove() }
            removeItemDisplays(mine)
        }
        mines.clear()
        mines.addAll(generateMines())
    }

    /** 다음날 */
    fun nextDay() {
        resourceInteractingPlayers.clear()
        val start = System.currentTimeMillis()

        mines.toList().forEach { mine ->
            if (mine.players.isEmpty()) return@forEach
            mine.players.toList().forEach { player ->
                player.leaveMine(mine)
            }
        }
        val newMines = generateMines()

        mines.clear()
        mines.addAll(newMines)

        val elapsed = System.currentTimeMillis() - start
        PrcCore.instance.loggerMessage("nextDay completed in ${elapsed}ms")
    }

    /** 저장 데이터 적용 */
    fun loadData() {
        resourceInteractingPlayers.clear()
        mines.forEach { mine ->
            mine.startBlockLocation?.block?.type = Material.BARRIER
            mine.exitBlockLocation?.block?.type = Material.BARRIER
        }
    }

    /** 특정 층으로 이동 처리 */
    fun Player.approach(currentMine: Mine?, floor: Int, isExit: Boolean = false): Boolean {
        val home = DataManager.mineExitLocation ?: return false

        fun teleportHome(): Boolean {
            this.teleport(home)
            return false
        }

        // 0층
        if (floor == 0) {
            currentMine?.let { leaveMine(it) }
            return teleportHome()
        }

        val nextMine = mines.firstOrNull { it.floor == floor } ?: return teleportHome()

        if (currentMine != null && currentMine !== nextMine) {
            leaveMine(currentMine)
        }

        val base = if (isExit) nextMine.exitBlockLocation else nextMine.startBlockLocation
        val target = base?.clone()?.add(0.0, 1.0, 0.0) ?: return teleportHome()

        if (this !in nextMine.players) {
            nextMine.players.add(this)
        }

        // 미션 트리거
        Bukkit.getPluginManager().callEvent(
            MissionEvent(this, MissionVersion.V1, "PLAYER_PROGRESS", "mine_module", nextMine.floor)
        )

        // 60층까지 도달하세요
        if (nextMine.floor >= 60) {
            grantAdvancement(this, "module/normal/diglett")
        }



        this.teleport(target)
        nextMine.spawnVisuals()
        return true
    }

    /** 자원 배치 */
    private fun Mine.addResource() {
        val zOffset = calculateOffset(floor)
        mineType.resourcesLocations.forEach { (x, y, z) ->
            val location = Location(world, x, y, z - zOffset)
            if (location.block.getRelative(BlockFace.DOWN).type == Material.AIR) return@forEach
            val type = getRandomResourceTypeForFloor(floor)
            val resource = Resource(type, location)
            resources.add(resource)
        }
    }

    /** 층 자원 확률 */
    private fun getRandomResourceTypeForFloor(floor: Int): ResourceType {
        return getRandomWeightedResourceForFloor(floor)
            ?: error("Floor $floor has no resource drop rule")
    }

    private fun getRandomWeightedResourceForFloor(floor: Int): ResourceType? {
        val rule = floorResourceRules.firstOrNull { floor in it.floors }
            ?: floorResourceRules.last()
        return pickByWeight(rule.entries)
    }

    private fun pickByWeight(entries: List<WeightedResource>): ResourceType? {
        val totalWeight = entries.sumOf { it.weight }
        if (totalWeight <= 0) return null

        var roll = Random.nextInt(totalWeight)
        for (entry in entries) {
            roll -= entry.weight
            if (roll < 0) return entry.type
        }
        return entries.lastOrNull()?.type
    }

    /** 몬스터 처치 시 드랍할 자원 아이템 */
    fun createMonsterDropItemForFloor(floor: Int): ItemStack? {
        val resourceType = getRandomWeightedResourceForFloor(floor) ?: return null
        return resourceType.prcItem.create()
    }

    /** 적 배치 데이터 */
    private fun Mine.addEnemyData() {
        val zOffset = calculateOffset(floor)
        mineType.enemysLocations.forEach { (x, y, z) ->
            val spawnLocation = Location(world, x, y, z - zOffset)
            val enemy = Enemy(spawnLocation, getEntityTypeForFloor(floor))
            enemys.add(enemy)
        }
    }

    /** 층수 기반 엔티티 */
    private fun getEntityTypeForFloor(floor: Int): EntityType {
        return when ((floor - 1) % 15) {
            in 0..4 -> EntityType.DROWNED
            in 5..9 -> EntityType.HUSK
            else -> EntityType.ZOMBIE
        }
    }

    /** 퇴장 */
    fun Player.leaveMine(mine: Mine) {
        val exitLocation = DataManager.mineExitLocation ?: return
        mine.players.remove(this)
        resourceInteractingPlayers.entries.removeIf { it.value == uniqueId }
        mine.removeVisuals()
        mine.enemys.filter { it.isSpawn && !it.isDead && it.enemyUUID != null }.forEach {
            it.isSpawn = false
            Bukkit.getEntity(UUID.fromString(it.enemyUUID))?.remove()
        }
        teleport(exitLocation)
    }

    /** 자원 채굴 */
    fun Player.gathering(resource: Resource) {
        if (resource.isGathering || gatheringPlayers.contains(this.uniqueId)) return
        val resourceKey = resource.getInteractionKey()
        val interactingPlayer = resourceInteractingPlayers[resourceKey]
        if (interactingPlayer != null && interactingPlayer != uniqueId) return


        val mainHand = inventory.itemInMainHand
        if (!mainHand.hasCustomModelData(PICKAXE_MODEL_DATAS, Material.WOODEN_SHOVEL)) return

        val mine = mines.find { it.players.contains(this) } ?: return
        val delay = getGatheringDelay(mainHand, resource.resourcesType) ?: return
        val totalTicks = delay.toDouble().coerceAtLeast(1.0)
        var elapsedTicks = 0.0

        var cancelled = false

        fun cancelGathering() {
            if (cancelled) return
            cancelled = true
            gatheringPlayers.remove(uniqueId)
            resourceInteractingPlayers.remove(resourceKey)
            sendActionBar(miniMessage.deserialize(""))
        }

        fun canKeepGathering(): Boolean {
            val currentMine = mines.find { it.players.contains(this) } ?: return false
            if (currentMine != mine) return false
            if (!isOnline || isDead) return false
            if (world != resource.location.world) return false
            return location.distanceSquared(resource.location.clone().add(0.5, 0.5, 0.5)) <= MAX_GATHERING_DISTANCE_SQUARED
        }

        var soundTask: org.bukkit.scheduler.BukkitTask? = null

        gatheringPlayers.add(uniqueId)
        resourceInteractingPlayers[resourceKey] = uniqueId

        // 타격 사운드/파티클 반복
        soundTask = Bukkit.getScheduler().runTaskTimer(
            PrcCore.instance,
            Runnable {
                if (!canKeepGathering()) {
                    soundTask?.cancel()
                    cancelGathering()
                    return@Runnable
                }

                elapsedTicks = (elapsedTicks + TICKINTERVAL).coerceAtMost(totalTicks)
                sendActionBar(miniMessage.deserialize(getGatheringProgressBar(elapsedTicks / totalTicks)))
                world.playSound(location, Sound.BLOCK_STONE_HIT, 1f, 1f)
                world.spawnParticle(
                    Particle.BLOCK, resource.location.clone().add(0.5, 0.5, 0.5), 10,
                    0.2, 0.2, 0.2, Material.STONE.createBlockData()
                )
            },
            0L, TICKINTERVAL
        )

        // 지연 완료 후 채집 완료 처리
        Bukkit.getScheduler().runTaskLater(PrcCore.instance, Runnable {
            soundTask.cancel()

            if (!canKeepGathering()) {
                cancelGathering()
                return@Runnable
            }

            sendActionBar(miniMessage.deserialize(""))
            val itemStack = resource.resourcesType.prcItem.create()

            Bukkit.getPluginManager().callEvent(
                MissionEvent(this, MissionVersion.V2, "PLAYER_PROGRESS", "mine_module", 1)
            )

            resource.getItemDisplay()?.remove()
            resource.isGathering = true
            resourceInteractingPlayers.remove(resourceKey)
            resource.location.block.type = Material.AIR

            val dropLoc = resource.location.clone().add(0.5, 0.3, 0.5)
            val dropped = dropLoc.world?.dropItem(dropLoc, itemStack)
            dropped?.pickupDelay = 0
            resource.isGathering = true
            resource.location.block.type = Material.AIR

            playSound(location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
            world.spawnParticle(Particle.BLOCK, resource.location, 30, 0.3, 0.3, 0.3, Material.STONE.createBlockData())

            if (isHeavyDrill(mainHand)) {
                world.spawnParticle(Particle.EXPLOSION, resource.location.clone().add(0.5, 0.5, 0.5), 1, 0.1, 0.1, 0.1, 0.0)
                playSound(resource.location, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.1f)
            }


            val gathered = mine.resources.count { it.isGathering }
            // 출구가 등장할 비율
            if (mine.exitBlockLocation == null && gathered >= ceil(mine.resources.size * 0.7).toInt()) {
                if (mine.floor < MAX_MINE_FLOOR) {
                    if (resource.location.block.getRelative(BlockFace.DOWN).type != Material.AIR) {
                        resource.location.block.setExit(mine)
                        if (gameData.maxMineFloor < mine.floor) gameData.maxMineFloor = mine.floor
                    }
                    else {
                        mine.players.forEach { player ->
                            player.sendMessage(miniMessage.deserialize("광산 출구가 입구 주변에 나타났습니다."))
                        }
                        mine.startBlockLocation?.clone()?.add(1.0, 0.0, 0.0)?.block?.setExit(mine)
                    }
                }
            }

            gatheringPlayers.remove(uniqueId)
            cancelGathering()
            inventory.itemInMainHand.decreaseCustomDurability(1, this)
        }, delay)
    }

    private fun Resource.getInteractionKey(): String {
        val blockLocation = location.toBlockLocation()
        return "${blockLocation.world?.name}:${blockLocation.blockX}:${blockLocation.blockY}:${blockLocation.blockZ}"
    }

    /** 채집 지연 시간 */
    fun getGatheringDelay(pickaxe: ItemStack, type: ResourceType): Long? {
        val sec = when (type) {
            Magnesium, Aluminum -> when {
                isHardPickaxe(pickaxe) -> 4.0
                isLightDrill(pickaxe) -> 4.0
                isHeavyDrill(pickaxe) -> 2.5
                else -> return null
            }
            Iron, Copper, Lithium -> when {
                isHardPickaxe(pickaxe) -> 6.0
                isLightDrill(pickaxe) -> 6.0
                isHeavyDrill(pickaxe) -> 4.0
                else -> return null
            }
            Gold -> when {
                isHardPickaxe(pickaxe) -> 8.0
                isLightDrill(pickaxe) -> 8.0
                isHeavyDrill(pickaxe) -> 6.0
                else -> return null
            }
            Platinum -> when {
                isLightDrill(pickaxe) -> 8.0
                isHeavyDrill(pickaxe) -> 6.0
                else -> return null
            }
            Nickel, Titanium -> when {
                isLightDrill(pickaxe) -> 10.0
                isHeavyDrill(pickaxe) -> 6.0
                else -> return null
            }
        }
        return (sec * 20).toLong()
    }

    /** 단단한 곡괭이 판정 */
    fun isHardPickaxe(item: ItemStack): Boolean = item.hasCustomModelData(PICKAXE_MODEL_DATA, Material.WOODEN_SHOVEL)

    /** 경량 드릴 판정 */
    fun isLightDrill(item: ItemStack): Boolean = item.hasCustomModelData(LIGHTDRILL_MODEL_DATA, Material.WOODEN_SHOVEL)

    /** 무거운 드릴 판정 */
    fun isHeavyDrill(item: ItemStack): Boolean = item.hasCustomModelData(HEAVYDRILL_MODEL_DATA, Material.WOODEN_SHOVEL)

    /** 채굴 진행도 바 */
    private fun getGatheringProgressBar(progress: Double): String {
        val clamped = progress.coerceIn(0.0, 1.0)
        val total = 20
        val filled = (clamped * total).toInt()
        val percent = (clamped * 100).toInt()
        val bar = "<green>${"|".repeat(filled)}</green><dark_gray>${"|".repeat(total - filled)}</dark_gray>"
        return "<gray>채굴 진행도</gray> $bar <yellow>$percent%</yellow>"
    }


    /** 층 선택 인벤토리 */
    fun showMineFloorSelector(player: Player) {
        val accessibleFloors = mines
            .filter { it.floor == 1 || mines.any { m -> m.floor == it.floor - 1 && m.floor <= gameData.maxMineFloor } }
            .distinctBy { it.floor }
            .sortedBy { it.floor }

        val validFloors = accessibleFloors
            .map { it.floor }
            .filter { it % 5 == 1 }
            .filter { it <= gameData.maxMineFloor }
            .toMutableSet()

        validFloors.add(1)
        val sortedFloors = validFloors.sorted()

        val slotIndices = listOf(4, 5, 6, 7, 13, 14, 15, 16, 22, 23, 24, 25)
        val inventory = Bukkit.createInventory(null, 27, miniMessage.deserialize("<white>\u340F\u3442"))

        for ((i, floor) in sortedFloors.withIndex()) {
            if (i >= slotIndices.size) break
            val item = ItemStack(Material.GLASS_PANE).apply {
                itemMeta = itemMeta?.apply {
                    displayName(miniMessage.deserialize("${floor}층"))
                    setCustomModelData(1)
                }
            }
            inventory.setItem(slotIndices[i], item)
        }

        val item = ItemStack(Material.IRON_HORSE_ARMOR).also { stack ->
            stack.itemMeta = stack.itemMeta?.apply {
                isHideTooltip = true

                // ── 표시 이름/로어 완전 제거 ──
                displayName(null)  // 빈 문자열 대신 null 로 제거
                lore(null)

                // ── 필요 설정 ──
                setCustomModelData(10)
            }
        }
        inventory.setItem(10, item)

        player.openInventory(inventory)
    }

    /** 출구 */
    private fun Block.setExit(mine: Mine) {
        type = Material.BARRIER
        mine.exitBlockLocation = location

        val display = createItemDisplay(
            location, Material.LEATHER_HORSE_ARMOR, 6,
            Vector3f(1.5f, 1.5f, 1.5f), 0.5, 0.9, 0.5
        )
        mine.exitBlockUUID = display.uniqueId.toString()

        val marker = createExitItemDisplay(location.clone().add(0.5, 0.5, 0.5), 3)
        mine.exitBlockMarker = marker.uniqueId.toString()
    }

    /** 자원 디스플레이 조회 */
    fun Resource.getItemDisplay(): ItemDisplay? {
        if (uuidString == null) return null
        val uuid = UUID.fromString(uuidString)
        val entity = Bukkit.getEntity(uuid)
        if (entity !is ItemDisplay) return null
        return entity
    }

    /** 디스플레이 조회 */
    fun getItemDisplayToUUID(uuid: String): ItemDisplay? {
        val id = UUID.fromString(uuid)
        val entity = Bukkit.getEntity(id)
        if (entity !is ItemDisplay) return null
        return entity
    }

    /** 시각 요소 생성 */
    fun Mine.spawnVisuals() {
        val start = System.currentTimeMillis()
        if (players.size > 1) return

        startBlockLocation?.block?.type = Material.BARRIER
        if (floor != 1) {
            startBlockUUID = createItemDisplay(
                startBlockLocation!!, Material.LEATHER_HORSE_ARMOR, 5,
                Vector3f(3.0f, 3.0f, 3.0f), 0.25, 1.8, 0.25
            ).uniqueId.toString()
        }
        val marker = createExitItemDisplay(startBlockLocation!!.clone().add(0.5, 0.5, 0.5), 4)
        marker.billboard = Display.Billboard.VERTICAL
        startBlockMarker = marker.uniqueId.toString()

        exitBlockLocation?.block?.type = Material.BARRIER
        exitBlockUUID = exitBlockLocation?.let {
            val display = createItemDisplay(
                it, Material.LEATHER_HORSE_ARMOR, 6,
                Vector3f(1.5f, 1.5f, 1.5f), 0.5, 0.9, 0.5
            )
            display.uniqueId.toString()
        }

        exitBlockMarker = exitBlockLocation?.let {
            val markerDisplay = createExitItemDisplay(it.clone().add(0.5, 1.0, 0.5), 3)
            markerDisplay.uniqueId.toString()
        }

        addResourceDisplays()
        spawnEnemysMine()

        val elapsed = System.currentTimeMillis() - start
        PrcCore.instance.loggerMessage("[Mine] spawnVisuals() for floor $floor took ${elapsed}ms")
    }

    /** 시각 요소 제거 */
    fun Mine.removeVisuals() {
        val start = System.currentTimeMillis()
        if (players.isNotEmpty()) return

        startBlockLocation?.block?.type = Material.AIR
        startBlockUUID?.let { getItemDisplayToUUID(it)?.remove() }
        startBlockMarker?.let { getItemDisplayToUUID(it)?.remove() }
        exitBlockLocation?.block?.type = Material.AIR
        exitBlockUUID?.let { getItemDisplayToUUID(it)?.remove() }
        exitBlockMarker?.let { getItemDisplayToUUID(it)?.remove() }

        resources.forEach {
            it.location.block.type = Material.AIR
            it.getItemDisplay()?.remove()
        }

        val elapsed = System.currentTimeMillis() - start
        PrcCore.instance.loggerMessage("[Mine] removeVisuals() for floor $floor took ${elapsed}ms")
    }

    /** 자원 디스플레이 */
    fun Mine.addResourceDisplays() {
        resources.filter { !it.isGathering }.forEach { resource ->
            resource.location.block.type = Material.BARRIER
            val itemDisplay = world.spawn(resource.location.clone().add(0.5, 0.5, 0.5), ItemDisplay::class.java)
            resource.uuidString = itemDisplay.uniqueId.toString()

            val itemStack = ItemStack(Material.GRAY_DYE).apply {
                itemMeta = itemMeta.apply { setCustomModelData(resource.resourcesType.prcItem.customModelData) }
            }
            itemDisplay.setItemStack(itemStack)
        }
    }

    /** 적 스폰 실행 */
    fun Mine.spawnEnemysMine() {
        val cycleFloor = ((floor.coerceAtLeast(1) - 1) % 15) + 1

        val (normalModel, spaceModel) = when (cycleFloor) {
            in 1..5   -> "rock_zombie" to "sapce_rock"
            in 6..10  -> "rock_zombie_magma" to "sapce_magma"
            else      -> "rock_zombie_nature" to "sapce_nature"
        }

        enemys
            .filter { !it.isSpawn && !it.isDead }
            .forEach { enemy ->
                val entity = world.spawnEntity(enemy.location.clone().add(0.5, 0.5, 0.5), EntityType.ZOMBIE) as Zombie

                entity.setAdult()
                entity.canPickupItems = false
                entity.equipment.apply {
                    clear()
                    helmetDropChance = 0f
                    chestplateDropChance = 0f
                    leggingsDropChance = 0f
                    bootsDropChance = 0f
                    itemInMainHandDropChance = 0f
                    itemInOffHandDropChance = 0f
                }

            entity.addPotionEffect(
                PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    PotionEffect.INFINITE_DURATION,
                    0,
                    false,
                    false,
                    false
                )
            )

            val modelId = if (Random.nextBoolean()) spaceModel else normalModel
            CoreFrameAPI.Model.applyModel(entity, modelId, entity.location)

            enemy.isSpawn = true
            enemy.enemyUUID = entity.uniqueId.toString()
        }
    }

    /** 아이템 디스플레이 제거 */
    private fun removeItemDisplays(mine: Mine) {
        mine.startBlockLocation?.block?.type = Material.AIR
        mine.exitBlockLocation?.block?.type = Material.AIR
        listOf(mine.startBlockUUID, mine.exitBlockUUID, mine.startBlockMarker, mine.exitBlockMarker)
            .forEach { it?.let { uuid -> getItemDisplayToUUID(uuid)?.remove() } }
    }

    /** 층수 오프셋 계산 */
    private fun calculateOffset(floor: Int): Double = ((floor - 1) / 15) * CALCULATE_OFFSET

    /** 광산 일괄 생성 */
    private fun generateMines(): List<Mine> {
        val templates = arrayOf(MineTemplate.M, MineTemplate.N, MineTemplate.R)
        val types = arrayOf(MineType.A, MineType.B, MineType.C, MineType.D, MineType.E, MineType.F, MineType.G, MineType.H, MineType.I, MineType.J, MineType.K,
            MineType.L, MineType.M, MineType.N, MineType.O)

        return (1..MAX_MINE_FLOOR).map { floor ->
            val template = templates[(floor - 1) / 5 % templates.size]
            val type = types[(floor - 1) % types.size]
            val zOffset = calculateOffset(floor)

            val mine = Mine(floor, template, type).apply {
                val startX = type.startX
                val startY = type.startY
                val startZ = type.startZ
                startBlockLocation = Location(world, startX, startY, startZ - zOffset)

                addResource()
                addEnemyData()
            }
            mine
        }
    }

    /** 시작 지점 디스플레이 생성 */
    private fun Mine.createItemDisplays(floor: Int, startBlockLocation: Location?) {
        startBlockLocation?.let { loc ->
            val marker = createItemDisplay(
                loc, Material.LEATHER_HORSE_ARMOR, 4,
                Vector3f(1.75f, 1.75f, 1.75f), 0.2, 5.0, 0.2
            )
            marker.billboard = Display.Billboard.VERTICAL
            startBlockMarker = marker.uniqueId.toString()

            if (floor != 1) {
                startBlockUUID = createItemDisplay(
                    loc, Material.LEATHER_HORSE_ARMOR, 5,
                    Vector3f(3.0f, 3.0f, 3.0f), 0.25, 1.8, 0.25
                ).uniqueId.toString()
            }
        }
    }

    /** 아이템 디스플레이 생성 */
    private fun createItemDisplay(
        loc: Location,
        material: Material,
        customModelData: Int,
        scale: Vector3f,
        xOffset: Double,
        yOffset: Double,
        zOffset: Double
    ): ItemDisplay {
        val itemDisplay = world.spawn(loc.clone().add(xOffset, yOffset, zOffset), ItemDisplay::class.java)
        val itemStack = ItemStack(material).apply {
            itemMeta = itemMeta.apply { setCustomModelData(customModelData) }
        }
        itemDisplay.transformation = Transformation(
            Vector3f(0f, 0f, 0f),
            AxisAngle4f(0f, 0f, 0f, 0f),
            scale,
            AxisAngle4f(Math.PI.toFloat(), 0f, 1f, 0f)
        )
        itemDisplay.isInvulnerable = true
        itemDisplay.setItemStack(itemStack)
        return itemDisplay
    }

    private fun createExitItemDisplay(loc: Location, customModelData: Int): ItemDisplay {
        val itemDisplay = world.spawn(loc, ItemDisplay::class.java)
        val itemStack = ItemStack(Material.LEATHER_HORSE_ARMOR).apply {
            itemMeta = itemMeta.apply { setCustomModelData(customModelData) }
        }
        itemDisplay.transformation = Transformation(
            Vector3f(0f, 1.75f, 0f),
            Quaternionf(0f, 1f, 0f, 0f),
            Vector3f(1.75f, 1.75f, 1.75f),
            Quaternionf(0f, 0f, 0f, 1f)
        )
        itemDisplay.displayWidth = 0f
        itemDisplay.displayHeight = 0f
        itemDisplay.interpolationDuration = 0
        itemDisplay.teleportDuration = 0
        itemDisplay.glowColorOverride = Color.fromARGB(255, 255, 255, 255)
        itemDisplay.billboard = Display.Billboard.VERTICAL
        itemDisplay.brightness = Display.Brightness(15, 15)
        itemDisplay.shadowRadius = 0f
        itemDisplay.shadowStrength = 1f
        itemDisplay.viewRange = 1f
        itemDisplay.isInvulnerable = true
        itemDisplay.setItemStack(itemStack)
        return itemDisplay
    }
}