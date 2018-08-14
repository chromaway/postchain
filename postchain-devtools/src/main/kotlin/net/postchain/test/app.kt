package net.postchain.test

import java.io.File

/**
 * Main function, everything starts here
 *
 * @param args { -t | --test } <path_to_gtxml_file> [ <blockchainRID> ]
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        usage()

    } else {
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-h", "--help" -> {
                    usage()
                }

                "-t", "--test" -> {
                    var filename = args[++i]
                    var blockchainRID = if (i + 1 < args.size) args[++i] else null
                    println("gtxml file will be processed: $filename\n")

                    val result = TestLauncher().runXMLGTXTests(
                            File(filename).readText(),
                            blockchainRID)

                    println("\nTest ${if (result) "passed" else "failed"}")
                }
            }

            i++
        }
    }
}

private fun usage() {
    println("Usage: java -jar postchain-devtools.jar " +
            "{ -t | --test } <path_to_gtxml_file> [ <blockchainRID> ]")
}
