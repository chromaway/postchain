package net.postchain

import net.postchain.cli.Cli
import net.postchain.cli.CliError
import net.postchain.cli.Ok
import java.io.File
import java.lang.management.ManagementFactory

fun main(args: Array<String>) {
    dumpPid()
    val cliResult = Cli().run { parse(args) }
    when(cliResult){
        is CliError -> {
            when(cliResult) {
                is CliError.MissingCommand -> {
                    Cli().usage()
                }
                is CliError.CommandNotFound -> {
                    Cli().usage(cliResult.command)
                }
                else -> cliResult.message?.let { println(it) }
            }
        }
        is Ok -> {
            cliResult.info?.also { println(it) }
        }
    }
    System.exit(cliResult.code)
}

fun dumpPid() {
    val processName = ManagementFactory.getRuntimeMXBean().name
    val pid = processName.split("@")[0]
    File("postchain.pid").writeText(pid)
}
