package net.postchain.client.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

/**
 * Root Cli Object
 */
class Cli : CliktCommand(name = "postchain-client") {

    val configFile by option(
            names = *arrayOf("-c", "--config"),
            help = "Configuration *.properties of node and blockchain"
    ).required()

    /*
        val nodeConfig by option(
                names = *arrayOf("--node-config", "-nc"),
                help = "Path to node config file")

        val blockchainConfig by option(
                names = *arrayOf("--blockchain-config", "-bc"),
                help = "Path to blockchain config file")

    */
    override fun run() {}
}