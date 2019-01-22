package net.postchain.devtools

import java.util.*

fun List<*>.asString(): String {
    return Arrays.toString(this.toTypedArray())
}
