package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.gtx.*
import org.junit.Test

class GTXMLValueEncodeScalarsTest {

    @Test
    fun encodeXMLGTXValue_null_successfully() {
        val gtxValue = GTXNull
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("<null xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>")

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXValue_string_successfully() {
        val gtxValue = StringGTXValue("hello")
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("<string>hello</string>")

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXValue_int_successfully() {
        val gtxValue = IntegerGTXValue(42)
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("<int>42</int>")

        assert(actual).isEqualTo(expected)
    }

    /**
     * According [1-3] XmlAdapter will not be applied to the root element of xml
     * and should be called manually.
     *
     * 1. https://stackoverflow.com/questions/21640566/
     * 2. https://jcp.org/en/jsr/detail?id=222
     * 3. https://coderanch.com/t/505457/
     * 4. [ObjectFactory.createBytearrayElement]
     */
    @Test
    fun encodeXMLGTXValue_bytea_as_root_element_and_xmladapter_not_called_successfully() {
        val gtxValue = ByteArrayGTXValue(
                byteArrayOf(0x01, 0x02, 0x03, 0x0A, 0x0B, 0x0C))
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("<bytea>AQIDCgsM</bytea>") // 0102030A0B0C

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXValue_bytea_as_nested_element_successfully() {
        val gtxValue = ByteArrayGTXValue(
                byteArrayOf(0x01, 0x02, 0x03, 0x0A, 0x0B, 0x0C))
        val array = ArrayGTXValue(arrayOf(gtxValue))

        val actual = GTXMLValueEncoder.encodeXMLGTXValue(array)
        val expected = expected("""
            <args>
                <bytea>0102030A0B0C</bytea>
            </args>
        """.trimIndent())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXValue_bytea_empty_successfully() {
        val gtxValue = ByteArrayGTXValue(byteArrayOf())
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("<bytea></bytea>")

        assert(actual).isEqualTo(expected)
    }
}