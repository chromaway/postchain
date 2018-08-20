package net.postchain

import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.ParameterException
import net.postchain.cli.Cli


fun main(args: Array<String>) {
    while (true) {
        val cli = Cli()
        with(readLine()) {
            when {
                this == null -> {
                    cli.usage()
                }

                !this.isEmpty() -> {
                    try {
                        cli.parse(this)
                    } catch (e: MissingCommandException) {
                        println(e.message)
                        cli.usage()
                    } catch (e: ParameterException) {
                        println(e.message)
                        cli.usage(e.jCommander.parsedCommand)
                    } catch (e: Exception) {
                        println(e.message)
                    }
                }

            }
        }
    }
}