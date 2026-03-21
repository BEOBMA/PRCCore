package org.beobma.prccore.listener

import org.bukkit.entity.Chicken
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

class OnCreatureSpawn : Listener {
    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val zombie = event.entity as? Zombie ?: return
        if (zombie.vehicle is Chicken) {
            event.isCancelled = true
        }
    }
}