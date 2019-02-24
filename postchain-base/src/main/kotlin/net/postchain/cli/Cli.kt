package net.postchain.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import java.sql.SQLException

class Cli {

    private val jCommander: JCommander
    private val commands: Map<String, Command> = listOf(
            CommandKeygen()

            , CommandWaitDb()
            , CommandCheckBlockchain()
            , CommandAddBlockchain()
            , CommandAddConfiguration()

            , CommandConfigureNode()
            , CommandRunNode()
            , CommandRunNodeAuto()
//            , CommandStopNode()

    ).map { it.key() to it }.toMap()

    init {
        jCommander = with(JCommander.newBuilder()) {
            commands.forEach { key, command -> addCommand(key, command) }
            build()
        }
    }

    fun parse(args: Array<String>): CliResult {
        return try {
            jCommander.parse(*args)
            if (jCommander.parsedCommand == null) {
                CliError.MissingCommand(message = "Expected a command, got <no-command>")
            } else {
                commands[jCommander.parsedCommand]?.execute()?: CliError.CommandNotFound(command = jCommander.parsedCommand)
            }
        } catch (e: ParameterException){
            CliError.CommandNotFound(command = jCommander.parsedCommand)
        } catch (e: SQLException){
            CliError.DatabaseOffline()
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