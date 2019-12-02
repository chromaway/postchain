package net.postchain.devtools

import com.github.ajalt.clikt.core.subcommands
import net.postchain.devtools.cli.CalculateBlockchainRidCommand
import net.postchain.devtools.cli.Cli
import net.postchain.devtools.cli.EncodeBlockchainConfigurationCommand
import net.postchain.devtools.cli.RunTestCommand

fun main(args: Array<String>) = Cli()
        .subcommands(RunTestCommand())
        .subcommands(EncodeBlockchainConfigurationCommand())
        .subcommands(CalculateBlockchainRidCommand())
        .main(args)
