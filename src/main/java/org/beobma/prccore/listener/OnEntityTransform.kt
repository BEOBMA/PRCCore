package org.beobma.prccore.listener

import org.bukkit.entity.Drowned
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTransformEvent

class OnEntityTransform : Listener {
    @EventHandler
    fun onEntityTransform(event: EntityTransformEvent) {
        val zombie = event.entity as? Zombie ?: return
        val transformedToDrowned = event.transformedEntity as? Drowned ?: return

        if (event.transformReason == EntityTransformEvent.TransformReason.DROWNED) {
            event.isCancelled = true
            transformedToDrowned.remove()
            zombie.remainingAir = zombie.maximumAir
        }
    }
}