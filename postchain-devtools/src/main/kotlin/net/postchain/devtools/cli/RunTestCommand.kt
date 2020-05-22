// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.devtools.TestLauncher
import java.io.File

/**
 * Cli test command
 */
class RunTestCommand : CliktCommand(
        name = "run-test",
        help = "Tests gtxml file"
) {

    private val filename by option(
            names = *arrayOf("--filename", "-f"),
            help = "Path to gtxml file"
    ).required()

    private val blockchainRID by option(
            names = *arrayOf("--blockchain-rid", "-rid"),
            help = "BlockchainRID Hexadecimal string"
    ).required()

    private val testOutputFileName by option(
            names = *arrayOf("--output"),
            help = "Path to output file"
    )

    override fun run() {
        println("GtxML file will be processed: $filename\n")

        val result = TestLauncher().runXMLGTXTests(
                File(filename).readText(),
                blockchainRID,
                (context.parent?.command as? Cli)?.nodeConfig,
                (context.parent?.command as? Cli)?.blockchainConfig
        )

        if (testOutputFileName != null) {
            File(testOutputFileName).writeText(result.toJSON())
        }

        println("\nTest ${if (result.passed) "passed" else "failed"}")
    }
}
