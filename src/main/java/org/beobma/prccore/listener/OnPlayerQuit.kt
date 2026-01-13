package org.beobma.prccore.listener

import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.DataManager.playerList
import org.beobma.prccore.manager.MineManager.leaveMine
import org.beobma.prccore.manager.TimeManager.timePause
import org.beobma.prccore.manager.TimeManager.unShowTimeBossBar
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffectType

class OnPlayerQuit : Listener {
    private val world = Bukkit.getWorlds().first()

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val currentMine = mines.find { it.players.contains(player) }
        unShowTimeBossBar(player)
        // TODO(테스트)
//        player.removePotionEffect(PotionEffectType.SLOWNESS)
//        player.removePotionEffect(PotionEffectType.BLINDNESS)
        playerList.remove(player)
        if (currentMine != null) {
            currentMine.players.remove(player)
            player.leaveMine(currentMine)
        }

        if (playerList.isEmpty()) {
            timePause()
        }
    }
}