// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
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
            , CommandWipeDb()
//            , CommandStopNode()

            , CommandPeerInfoList()
            , CommandPeerInfoFind()
            , CommandPeerInfoAdd()
            , CommandPeerInfoRemove()
            , CommandPeerInfoImport()

    ).map { it.key() to it }.toMap()

    init {
        jCommander = with(JCommander.newBuilder()) {
            commands.forEach { (key, command) -> addCommand(key, command) }
            build()
        }
    }

    fun parse(args: Array<String>): CliResult {
        return try {
            jCommander.parse(*args)
            if (jCommander.parsedCommand == null) {
                CliError.MissingCommand(message = "Expected a command, got <no-command>")
            } else {
                commands[jCommander.parsedCommand]?.execute()
                        ?: CliError.ArgumentNotFound(command = jCommander.parsedCommand)
            }
        } catch (e: MissingCommandException) {
            CliError.MissingCommand(e.unknownCommand)
        } catch (e: ParameterException) {
            CliError.ArgumentNotFound(command = jCommander.parsedCommand)
        } catch (e: SQLException) {
            CliError.DatabaseError(e)
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

    fun usageCommands() {
        val usage = jCommander.commands.keys
                .asSequence()
                .sorted()
                .map { cmd ->
                    "${cmd.padEnd(25, ' ')}${jCommander.getCommandDescription(cmd)}"
                }.joinToString(
                        separator = "\n  ",
                        prefix = "Commands:\n  "
                )

        println(usage)
    }
}