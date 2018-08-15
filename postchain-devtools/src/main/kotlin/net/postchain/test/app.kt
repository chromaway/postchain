package net.postchain.test

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

/**
 * Root Cli object
 */
class Cli : CliktCommand(name = "postchain-devtools") {

    val config by option(
            names = *arrayOf("--config", "-c"),
            help = "Path to config file")

    override fun run() {}
}

/**
 * Cli test command
 */
class TestCommand : CliktCommand(name = "test", help = "Tests gtxml file") {

    private val filename by option(
            names = *arrayOf("--filename", "-f"),
            help = "Path to gtxml file")

    private val blockchainRID by option(
            names = *arrayOf("--blockchain-rid", "-rid"),
            help = "BlockchainRID Hexadecimal string")

    override fun run() {
        println("Gtxml file will be processed: $filename\n")

        val result = TestLauncher().runXMLGTXTests(
                File(filename).readText(),
                blockchainRID,
                (context.parent?.command as? Cli)?.config)

        println("\nTest ${if (result) "passed" else "failed"}")
    }
}

fun main(args: Array<String>) = Cli()
        .subcommands(TestCommand())
        .main(args)
