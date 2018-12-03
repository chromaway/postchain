package net.postchain.devtools.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option

/**
 * Root Cli object
 */
class Cli : CliktCommand(name = "postchain-devtools") {

    val config by option(
            names = *arrayOf("--config", "-c"),
            help = "Path to config file")

    override fun run() {}
}