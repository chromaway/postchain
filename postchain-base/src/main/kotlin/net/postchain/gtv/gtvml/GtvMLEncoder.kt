package net.postchain.gtv.gtvml;

import net.postchain.base.gtxml.ArrayType
import net.postchain.base.gtxml.DictType
import net.postchain.base.gtxml.ObjectFactory
import net.postchain.gtv.*
import java.io.StringWriter
import java.math.BigInteger
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement


object GtvMLEncoder {

    private val objectFactory = ObjectFactory()

    /**
     * Encodes [Gtv] into XML format
     */
    fun encodeXMLGtv(Gtv: Gtv): String {
        return with(StringWriter()) {
            JAXB.marshal(encodeGTXMLValueToJAXBElement(Gtv), this)
            toString()
        }
    }

    fun encodeGTXMLValueToJAXBElement(Gtv: Gtv): JAXBElement<*> {
        return when (Gtv) {
            /**
             * Note: null element will be equal to:
             *      `<null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>`
             */
            is GtvNull -> objectFactory.createNull(null)
            is GtvString -> objectFactory.createString(Gtv.string)
            is GtvInteger -> objectFactory.createInt(BigInteger.valueOf(Gtv.integer))
            is GtvByteArray -> objectFactory.createBytea(Gtv.bytearray) // See comments in GTXMLValueEncodeScalarsTest
            is GtvArray -> createArrayElement(Gtv)
            is GtvDictionary -> createDictElement(Gtv)
            else -> throw IllegalArgumentException("Unknown type of Gtv")
        }
    }

    private fun createArrayElement(Gtv: GtvArray): JAXBElement<ArrayType> {
        return with(objectFactory.createArrayType()) {
            Gtv.array
                    .map(GtvMLEncoder::encodeGTXMLValueToJAXBElement)
                    .toCollection(this.elements)

            objectFactory.createArray(this)
        }
    }

    private fun createDictElement(Gtv: GtvDictionary): JAXBElement<DictType> {
        return with(objectFactory.createDictType()) {
            Gtv.dict.map { entry ->
                val entryType = objectFactory.createEntryType()
                entryType.key = entry.key
                entryType.value = encodeGTXMLValueToJAXBElement(entry.value)
                entryType
            }.toCollection(this.entry)

            objectFactory.createDict(this)
        }
    }
}
