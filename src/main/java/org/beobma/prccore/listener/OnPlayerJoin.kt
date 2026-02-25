package org.beobma.prccore.listener

import org.beobma.prccore.manager.DataManager.playerList
import org.beobma.prccore.manager.TimeManager.handleEndOfDayWarnings
import org.beobma.prccore.manager.TimeManager.sendLateNightReminder
import org.beobma.prccore.manager.TimeManager.showTimeBossBar
import org.beobma.prccore.manager.TimeManager.hasStartedTimeFlow
import org.beobma.prccore.manager.TimeManager.timePlay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent


class OnPlayerJoin : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        playerList.add(player)
        showTimeBossBar(player)
        sendLateNightReminder()
        handleEndOfDayWarnings()
        if (hasStartedTimeFlow()) {
            timePlay()
        }
    }
}