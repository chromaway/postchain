package net.postchain.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException

class Cli {

    private val jCommander: JCommander
    private val commands: Map<String, Command> = listOf(
            CommandKeygen()

            , CommandAddBlockchain()
            , CommandAddConfiguration()

            , CommandConfigureNode()
            , CommandRunNode()
//            , CommandStopNode()

    ).map { it.key() to it }.toMap()

    init {
        jCommander = with(JCommander.newBuilder()) {
            commands.forEach { key, command -> addCommand(key, command) }
            build()
        }
    }

    fun parse(args: Array<String>) {
        jCommander.parse(*args)

        if (jCommander.parsedCommand == null) {
            throw MissingCommandException("Expected a command, got <no-command>", "<no-command>")
        } else {
            commands[jCommander.parsedCommand]?.execute()
        }
    }

    fun parse(input: String) {
        jCommander.parse(*input.split(Regex("\\s+")).toTypedArray())
        commands[jCommander.parsedCommand]?.execute()
    }

    fun usage() {
        jCommander.usage()
    }

    fun usage(command: String) {
        jCommander.usage(command)
    }
}