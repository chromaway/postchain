// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.gtvml.GtvMLParser
import java.io.File

class EncodeBlockchainConfigurationCommand : CliktCommand(
        name = "encode-blockchain-config",
        help = "Encodes blockchain configuration in GtxML format into binary format"
) {

    private val blockchainConfigFilename by option(
            names = *arrayOf("-bc", "--blockchain-config"),
            help = "Configuration file of blockchain (GtxML)"
    ).required()

    override fun run() {
        println("GtxML file will be encoded to binary: $blockchainConfigFilename")

        try {
            val binaryFilename = "$blockchainConfigFilename.bin"
            val gtv = GtvMLParser.parseGtvML(File(blockchainConfigFilename).readText())
            val binary = GtvEncoder.encodeGtv(gtv)
            File(binaryFilename).writeBytes(binary)

            println("Binary file has been created: $binaryFilename")

        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}
