package org.beobma.prccore

import org.beobma.prccore.listener.*
import org.beobma.prccore.manager.DataManager.loadAll
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.DataManager.playerList
import org.beobma.prccore.manager.DataManager.saveAll
import org.beobma.prccore.manager.MineManager
import org.beobma.prccore.manager.MineManager.leaveMine
import org.beobma.prccore.manager.PlantManager.register
import org.beobma.prccore.manager.TimeManager.timePause
import org.beobma.prccore.plant.list.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class PrcCore : JavaPlugin() {

    companion object {
        lateinit var instance: PrcCore
    }

    override fun onEnable() {
        instance = this
        registerEvents()
        registerPlants()
        loadAll()
        MineManager.reset()
        playerList.addAll(Bukkit.getOnlinePlayers())
        loggerMessage("PrcCore Plugin Enable")
    }

    override fun onDisable() {
        playerList.toList().forEach { player ->
            val mine = mines.find { it.players.contains(player) } ?: return@forEach
            mine.players.remove(player)
            player.leaveMine(mine)
        }
        playerList.clear()
        timePause()
        saveAll()

        loggerMessage("PrcCore Plugin Disable")
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(OnInventoryOpen(), this)
        server.pluginManager.registerEvents(OnInventoryClose(), this)
        server.pluginManager.registerEvents(OnPlayerInteract(), this)
        server.pluginManager.registerEvents(OnPlayerJoin(), this)
        server.pluginManager.registerEvents(OnPlayerQuit(), this)
        server.pluginManager.registerEvents(OnInventoryClick(), this)
        server.pluginManager.registerEvents(OnEntityDeath(), this)
        server.pluginManager.registerEvents(OnPlayerMove(), this)
        server.pluginManager.registerEvents(OnPlayerItemHeld(), this)
        server.pluginManager.registerEvents(OnConsume(), this)
    }

    private fun registerPlants() {
        DeadGrassPlant().register(DeadGrassPlant::class.java, 0, 0, 0)

        PotatoPlant().register(PotatoPlant::class.java, 31, 1, 1)
        CabbagePlant().register(CabbagePlant::class.java, 32, 2, 5)
        CucumberPlant().register(CucumberPlant::class.java, 33, 3, 9)
        CoffeeBeansPlant().register(CoffeeBeansPlant::class.java, 34, 4, 13)
        TomatoPlant().register(TomatoPlant::class.java, 35, 5, 17)
        CornerPlant().register(CornerPlant::class.java, 36, 6, 21)
        WheatPlant().register(WheatPlant::class.java, 37, 7, 25)
        CranberryPlant().register(CranberryPlant::class.java, 38, 8, 29)
        BitPlant().register(BitPlant::class.java, 39, 9, 33)
        PumpkinPlant().register(PumpkinPlant::class.java, 40, 10, 37)
        WeedPlant().register(WeedPlant::class.java, 1, 2, 41)
    }


    fun loggerMessage(msg: String) {
        logger.info("[PrcCore] $msg")
    }
}
