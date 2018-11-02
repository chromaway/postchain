package net.postchain.test

import java.util.*

fun List<*>.asString(): String {
    return Arrays.toString(this.toTypedArray())
}
