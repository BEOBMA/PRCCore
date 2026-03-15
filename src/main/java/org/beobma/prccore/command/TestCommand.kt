package org.beobma.prccore.command
import net.kyori.adventure.text.minimessage.MiniMessage
import org.beobma.prccore.manager.DataManager.mines
import org.beobma.prccore.manager.DataManager.saveAll
import org.beobma.prccore.manager.MineManager.approach
import org.beobma.prccore.manager.MineManager.clearAllResourcesOnCurrentFloor
import org.beobma.prccore.manager.MineManager.killAllMonstersOnCurrentFloor
import org.beobma.prccore.manager.TimeManager.endOfDay
import org.beobma.prccore.manager.TimeManager.toggleTimeFlow
import org.beobma.prccore.resource.Resource
import org.beobma.prccore.tool.Pickaxe
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TestCommand: CommandExecutor {
    private val mm = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("/prctest <timeflow|nextday|clearores|killmobs|setmineprogress>")
            return true
        }

        when (args[0].lowercase()) {
            "timeflow" -> {
                val running = toggleTimeFlow()
                sender.sendMessage(if (running) "[PrcCore] 시간 흐름을 시작했습니다." else "[PrcCore] 시간 흐름을 멈췄습니다.")
            }
            "nextday" -> {
                endOfDay()
                sender.sendMessage("[PrcCore] 하루를 강제로 종료했습니다.")
            }
            "clearores" -> {
                val player = sender as? Player ?: run {
                    sender.sendMessage("[PrcCore] 플레이어만 사용할 수 있습니다.")
                    return true
                }
                val cleared = clearAllResourcesOnCurrentFloor(player)
                sender.sendMessage("[PrcCore] 현재 광산층 광물 ${cleared}개를 채굴 완료 처리했습니다.")
            }
            "killmobs" -> {
                val player = sender as? Player ?: run {
                    sender.sendMessage("[PrcCore] 플레이어만 사용할 수 있습니다.")
                    return true
                }
                val killed = killAllMonstersOnCurrentFloor(player)
                sender.sendMessage("[PrcCore] 현재 광산층 몬스터 ${killed}마리를 제거했습니다.")
            }
            "setmineprogress" -> {
                val player = sender as? Player ?: run {
                    sender.sendMessage("[PrcCore] 플레이어만 사용할 수 있습니다.")
                    return true
                }
                val floor = args.getOrNull(1)?.toIntOrNull()
                if (floor == null || floor < 1) {
                    sender.sendMessage("[PrcCore] /prctest setmineprogress <floor>")
                    return true
                }

                val currentMine = mines.find { it.players.contains(player) }
                val moved = player.approach(currentMine, floor)
                if (!moved) {
                    sender.sendMessage("[PrcCore] ${floor}층으로 이동하지 못했습니다. 광산 출구 좌표/층 데이터를 확인해주세요.")
                    return true
                }

                sender.sendMessage("[PrcCore] ${floor}층으로 이동했습니다.")
            }
            "item" -> {
                val player = sender as? Player ?: run {
                    sender.sendMessage("[PrcCore] 플레이어만 사용할 수 있습니다.")
                    return true
                }
                player.inventory.addItem(Pickaxe().heavyDrill)
            }
            else -> sender.sendMessage(mm.deserialize("<red>알 수 없는 서브 명령어입니다.</red>"))
        }

        saveAll()
        return true
    }
}