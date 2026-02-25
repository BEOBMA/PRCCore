package org.beobma.prccore

import kr.eme.prcShop.api.PRCItems
import org.beobma.prccore.command.StartTimeFlowCommand
import org.beobma.prccore.listener.*
import org.beobma.prccore.manager.DataManager.gameData
import org.beobma.prccore.manager.DataManager.loadAll
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.DataManager.playerList
import org.beobma.prccore.manager.DataManager.saveAll
import org.beobma.prccore.manager.MineManager
import org.beobma.prccore.manager.MineManager.leaveMine
import org.beobma.prccore.manager.PlantManager.register
import org.beobma.prccore.manager.TimeManager.showTimeBossBar
import org.beobma.prccore.manager.TimeManager.timePause
import org.beobma.prccore.manager.TimeManager.timePlay
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
        registerCommands()
        registerPlants()
        loadAll()
        MineManager.reset()
        playerList.addAll(Bukkit.getOnlinePlayers())
        playerList.forEach { showTimeBossBar(it) }

        if (gameData.hasStartedTimeFlow && playerList.isNotEmpty()) {
            timePlay()
        }
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

    private fun registerCommands() {
        getCommand("starttimeflow")?.setExecutor(StartTimeFlowCommand())
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
        DeadGrassPlant().register(DeadGrassPlant::class.java, 0, 0, 0, PRCItems.TOMATO_SEED, PRCItems.TOMATO_SEED, PRCItems.TOMATO_SEED, PRCItems.TOMATO_SEED)
        WeedPlant().register(WeedPlant::class.java, 1, 2, 41, PRCItems.CRANBERRY_SEED, PRCItems.CRANBERRY_G1, PRCItems.CRANBERRY_G2, PRCItems.CRANBERRY_G3)

        PotatoPlant().register(PotatoPlant::class.java, 31, 1, 1, PRCItems.POTATO_SEED, PRCItems.POTATO_G1, PRCItems.POTATO_G2, PRCItems.POTATO_G3)
        CabbagePlant().register(CabbagePlant::class.java, 32, 2, 5, PRCItems.CABBAGE_SEED, PRCItems.CABBAGE_G1, PRCItems.CABBAGE_G2, PRCItems.CABBAGE_G3)
        CucumberPlant().register(CucumberPlant::class.java, 33, 3, 9, PRCItems.CUCUMBER_SEED, PRCItems.CUCUMBER_G1, PRCItems.CUCUMBER_G2, PRCItems.CUCUMBER_G3)
        CoffeeBeansPlant().register(CoffeeBeansPlant::class.java, 34, 4, 13, PRCItems.COFFEE_BEAN_SEED, PRCItems.COFFEE_BEAN_G1, PRCItems.COFFEE_BEAN_G2, PRCItems.COFFEE_BEAN_G3)
        TomatoPlant().register(TomatoPlant::class.java, 35, 5, 17, PRCItems.TOMATO_SEED, PRCItems.TOMATO_G1, PRCItems.TOMATO_G2, PRCItems.TOMATO_G3)
        CornerPlant().register(CornerPlant::class.java, 36, 6, 21, PRCItems.CORN_SEED, PRCItems.CORN_G1, PRCItems.CORN_G2, PRCItems.CORN_G3)
        WheatPlant().register(WheatPlant::class.java, 37, 7, 25, PRCItems.WHEAT_SEED, PRCItems.WHEAT_G1, PRCItems.WHEAT_G2, PRCItems.WHEAT_G3)
        CranberryPlant().register(CranberryPlant::class.java, 38, 8, 29, PRCItems.CRANBERRY_SEED, PRCItems.CRANBERRY_G1, PRCItems.CRANBERRY_G2, PRCItems.CRANBERRY_G3)
        BitPlant().register(BitPlant::class.java, 39, 9, 33, PRCItems.BEET_SEED, PRCItems.BEET_G1, PRCItems.BEET_G2, PRCItems.BEET_G3)
        PumpkinPlant().register(PumpkinPlant::class.java, 40, 10, 37, PRCItems.PUMPKIN_SEED, PRCItems.PUMPKIN_G1, PRCItems.PUMPKIN_G2, PRCItems.PUMPKIN_G3)
    }


    fun loggerMessage(msg: String) {
        logger.info("[PrcCore] $msg")
    }
}
