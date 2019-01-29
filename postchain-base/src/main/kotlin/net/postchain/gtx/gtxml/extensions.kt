package net.postchain.gtx.gtxml

import net.postchain.gtv.GtvType
import net.postchain.gtv.GtvType.*
import javax.xml.namespace.QName

/**
 * Returns [GtvType] object correspondent to [QName]
 */
fun GtvTypeOf(qname: QName): GtvType =
        GtvTypeOf(qname.localPart)

/**
 * Returns [GtvType] object correspondent to [String]
 */
fun GtvTypeOf(type: String): GtvType {
    return when (type) {
        "null" -> NULL
        "string" -> STRING
        "int" -> INTEGER
        "bytea" -> BYTEARRAY
        "args" -> ARRAY
        "dict" -> DICT
        else -> throw IllegalArgumentException("Unknown type of GtvType: $type")
    }
}

/**
 * Returns `true` if [QName] corresponds `<param />` tag and `false` otherwise
 */
fun isParam(qname: QName): Boolean =
        "param".equals(qname.localPart, true)