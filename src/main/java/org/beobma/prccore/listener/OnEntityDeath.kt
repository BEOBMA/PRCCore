package org.beobma.prccore.listener

import kr.eme.prcMission.api.events.MissionEvent
import kr.eme.prcMission.enums.MissionVersion
import kr.eme.prcShop.api.PRCItems
import org.beobma.prccore.manager.AdvancementManager.grantAdvancement
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.FarmingManager.makeCoffeeBeanEatable
import org.beobma.prccore.manager.MineManager.createMonsterDropItemForFloor
import org.beobma.prccore.manager.MineManager.leaveMine
import org.beobma.prccore.manager.MineManager.markEnemyAsDead
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import kotlin.random.Random

class OnEntityDeath : Listener {
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer

        if (killer is Player) {
            onEntityKillerByPlayer(event, killer, entity)
        }
        else if (entity is Player) {
            onPlayerKillerByEntity(entity)
        }
        else {
            onEntityKillerByNull(entity)
        }
    }

    private fun onEntityKillerByPlayer(event: EntityDeathEvent, killer: Player, entity: LivingEntity) {
        val mine = mines.find { it.players.contains(killer) } ?: return
        val enemy = mine.enemys.find { it.enemyUUID == entity.uniqueId.toString() } ?: return

        Bukkit.getPluginManager().callEvent(
            MissionEvent(killer, MissionVersion.V2, "PLAYER_PROGRESS", "mine_module", 1)
        )

        markEnemyAsDead(entity)
        val dropItem = createMonsterDropItemForFloor(mine.floor)
        dropItem?.let { event.drops.add(it) }

        if (Random.nextDouble() < 0.15) {
            val coffeeItem = makeCoffeeBeanEatable(PRCItems.COFFEE_BEAN_G1.create())
            event.drops.add(coffeeItem)
        }
    }

    private fun onPlayerKillerByEntity(entity: Player) {
        val mine = mines.find { mine -> mine.players.any { it == entity } } ?: return
        mine.players.remove(entity)
        entity.leaveMine(mine)

        // 광산 1층에서 사망
        if (mine.floor == 1) {
            grantAdvancement(entity, "module/normal/noob");
        }
    }

    // 지형지물 등으로 인한 낙사, 추락사
    private fun onEntityKillerByNull(entity: LivingEntity) {
        markEnemyAsDead(entity)
    }
}