package net.postchain.gtv

import net.postchain.gtv.gtvml.GtvMLParser
import java.io.File

object GtvFileReader {

    /**
     * Gets the entire content of GtvML (*.xml) or Gtv (*.gtv) files as a Gtv.
     * @param filename file name to read
     * @return the entire content of this file as a Gtv.
     */
    fun readFile(filename: String): Gtv {
        return when (filename.takeLast(3)) {
            "xml" -> {
                GtvMLParser.parseGtvML(File(filename).readText())
            }
            "gtv" -> {
                GtvFactory.decodeGtv(File(filename).readBytes())
            }
            else -> throw IllegalArgumentException("Unknown file format of: $filename")
        }
    }
}