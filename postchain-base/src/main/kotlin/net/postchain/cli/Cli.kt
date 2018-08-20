package net.postchain.cli

import com.beust.jcommander.JCommander

class Cli {

    private val jCommander: JCommander
    private val commands: Map<String, Command> = listOf(
            CommandKeygen()
            , CommandRunNode()
            , CommandStopNode()
            , CommandConfigureNode()
            , CommandAddBlockchain()
            , CommandAddConfiguration()
    ).map { it.key() to it }.toMap()

    init {
        jCommander = with(JCommander.newBuilder()) {
            commands.forEach { key, command -> addCommand(key, command) }
            build()
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