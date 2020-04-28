package net.postchain.client

import com.github.ajalt.clikt.core.subcommands
import net.postchain.client.cli.Cli
import net.postchain.client.cli.PostTxCommand

fun main(args: Array<String>) = Cli()
        .subcommands(PostTxCommand())
        .main(args)
