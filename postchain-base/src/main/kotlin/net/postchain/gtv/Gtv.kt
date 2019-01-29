package net.postchain.gtv

import net.postchain.gtv.messages.Gtv as RawGtv

/**
 * Enum class of GTXML types
 * Note: order is same as in ASN.1, thus we can use same integer ids.
 */
enum class GtvType {
    NULL, BYTEARRAY, STRING, INTEGER, DICT, ARRAY
}

/**
 * GTV stands for Generic Transfer Value, and is a (home made) format for data transfer.
 */
interface Gtv {
    val type: GtvType
    operator fun get(i: Int): Gtv
    operator fun get(s: String): Gtv?
    fun asString(): String
    fun asArray(): Array<out Gtv>
    fun getSize(): Int
    fun isNull(): Boolean
    fun asDict(): Map<String, Gtv>
    fun asInteger(): Long
    fun asByteArray(convert: Boolean = false): ByteArray
    fun asPrimitive(): Any?
    fun getRawGtv(): RawGtv
    fun isContainerType(): Boolean  // Returns true if the Gtv can hold other Gtvs
}