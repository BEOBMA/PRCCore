package org.beobma.prccore.listener

import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTransformEvent

class OnEntityTransform : Listener {
    @EventHandler
    fun onEntityTransform(event: EntityTransformEvent) {
        val zombie = event.entity as? Zombie ?: return
        if (event.transformReason == EntityTransformEvent.TransformReason.DROWNED) {
            event.isCancelled = true
            zombie.remove()
        }
    }
}