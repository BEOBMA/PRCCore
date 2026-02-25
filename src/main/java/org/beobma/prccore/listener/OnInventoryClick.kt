package org.beobma.prccore.listener

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.beobma.prccore.manager.DataManager.gameData
import org.beobma.prccore.manager.MineManager.approach
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class OnInventoryClick : Listener {
    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem ?: return
        val displayName = clickedItem.itemMeta?.displayName()
        val view = event.view


        if (view.title() == miniMessage.deserialize("<white>\u340F\u3442")) {
            event.isCancelled = true
            if (event.clickedInventory != view.topInventory) return
            if (displayName == null) return
            val displayName = LegacyComponentSerializer.legacySection().serialize(displayName)
            val floor = Regex("""(\d+)층""").find(displayName)?.groupValues?.get(1)?.toIntOrNull() ?: return
            if (gameData.maxMineFloor < floor) return

            player.approach(null, floor)
            player.closeInventory()
        }
    }
}