// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

fun List<*>.asString(): String {
    return this.toTypedArray().contentToString()
}
