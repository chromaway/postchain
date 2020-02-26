package net.postchain.e2e.tools

import org.awaitility.Duration.ONE_SECOND
import org.awaitility.Duration.TEN_SECONDS

internal val SECONDS_20 = TEN_SECONDS.multiply(2)
internal val SECONDS_21 = SECONDS_20 + ONE_SECOND

internal fun postgresUrl(host: String, port: Int): String = "jdbc:postgresql://$host:$port/postchain"

internal fun parseLogLastHeight(log: String): Int? {
    val pattern = "(height: (?<h>[0-9]+))".toPattern()
    val matcher = pattern.matcher(log)

    var res: Int? = null
    while (matcher.find()) {
        res = matcher.group("h").toInt()
    }

    return res
}
