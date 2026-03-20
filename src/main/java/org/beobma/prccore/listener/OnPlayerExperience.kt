package org.beobma.prccore.listener

import org.beobma.prccore.manager.DataManager.mines
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent

class OnPlayerExperience : Listener {

    @EventHandler
    fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        val player = event.player
        if (mines.none { it.players.contains(player) }) return
        event.amount = 0
    }
}
