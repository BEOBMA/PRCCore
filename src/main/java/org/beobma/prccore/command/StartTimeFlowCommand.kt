package org.beobma.prccore.command

import org.beobma.prccore.manager.DataManager.saveAll
import org.beobma.prccore.manager.TimeManager.hasStartedTimeFlow
import org.beobma.prccore.manager.TimeManager.startTimeFlow
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class StartTimeFlowCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (hasStartedTimeFlow()) {
            sender.sendMessage("[PrcCore] 시간은 이미 흐르고 있습니다.")
            return true
        }

        startTimeFlow()
        saveAll()

        sender.server.broadcastMessage("[PrcCore] 튜토리얼 단계가 종료되어 시간이 흐르기 시작합니다.")

        return true
    }
}
