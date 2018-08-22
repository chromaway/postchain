package net.postchain

import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.ParameterException
import net.postchain.cli.Cli


fun main(args: Array<String>) {
    with(Cli()) {
        try {
            parse(args)
        } catch (e: MissingCommandException) {
            println(e.message)
            usage()
        } catch (e: ParameterException) {
            println(e.message)
            usage(e.jCommander.parsedCommand)
        } catch (e: Exception) {
            println(e.message)
        }
    }
}