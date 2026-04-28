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
            return true
        }

        startTimeFlow()
        saveAll()

        return true
    }
}
