package net.postchain.test

import com.github.ajalt.clikt.core.subcommands
import net.postchain.test.cli.Cli
import net.postchain.test.cli.RunTestCommand

fun main(args: Array<String>) = Cli()
        .subcommands(RunTestCommand())
        .main(args)
