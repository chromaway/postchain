package net.postchain.devtools

fun List<*>.asString(): String {
    return this.toTypedArray().contentToString()
}
