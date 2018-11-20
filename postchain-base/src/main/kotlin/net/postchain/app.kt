package net.postchain

import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.ParameterException
import net.postchain.cli.Cli
import java.io.File
import java.lang.management.ManagementFactory


fun main(args: Array<String>) {
    dumpPid()

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
            if (e.message != null) {
                println(e.message)
            } else {
                e.printStackTrace()
            }
        }
    }
}

fun dumpPid() {
    val processName = ManagementFactory.getRuntimeMXBean().name
    val pid = processName.split("@")[0]
    File("postchain.pid").writeText(pid)
}
