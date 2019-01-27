package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import net.postchain.gtx.*
import org.junit.Test

class GTXMLValueEncodeArraysTest {

    @Test
    fun encodeXMLGTXValue_array_empty_successfully() {
        val gtxValue = ArrayGTXValue(arrayOf())
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = arrayOf(
                expected("<args></args>"),
                expected("<args/>"))

        assert(actual).isIn(*expected)
    }

    @Test
    fun encodeXMLGTXValue_array_successfully() {
        val gtxValue = ArrayGTXValue(arrayOf(
                StringGTXValue("hello"),
                IntegerGTXValue(42)))
        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)
        val expected = expected("""
            <args>
                <string>hello</string>
                <int>42</int>
            </args>""".trimIndent())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGTXValue_compound_array_successfully() {
        val gtxValue = ArrayGTXValue(arrayOf(
                GTXNull,
                StringGTXValue("hello"),
                IntegerGTXValue(42),
                ArrayGTXValue(arrayOf()),
                ArrayGTXValue(arrayOf(
                        ArrayGTXValue(arrayOf(
                                DictGTXValue(mapOf(
                                        "0" to GTXNull,
                                        "1" to StringGTXValue("1"),
                                        "2" to IntegerGTXValue(2)
                                ))
                        )),
                        DictGTXValue(mapOf(
                                "args" to ArrayGTXValue(arrayOf(
                                        IntegerGTXValue(1),
                                        StringGTXValue("2")
                                )),
                                "str" to StringGTXValue("foo"),
                                "int" to IntegerGTXValue(42)
                        ))
                )),
                DictGTXValue(mapOf()),
                DictGTXValue(mapOf(
                        "1" to StringGTXValue("1"),
                        "2" to IntegerGTXValue(42)
                ))
        ))

        val actual = GTXMLValueEncoder.encodeXMLGTXValue(gtxValue)

        val expected = expected("""
            <args>
                <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                <string>hello</string>
                <int>42</int>
                <args/>
                <args>
                    <args>
                        <dict>
                            <entry key="0">
                                <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                            </entry>
                            <entry key="1">
                                <string>1</string>
                            </entry>
                            <entry key="2">
                                <int>2</int>
                            </entry>
                        </dict>
                    </args>
                    <dict>
                        <entry key="args">
                            <args>
                                <int>1</int>
                                <string>2</string>
                            </args>
                        </entry>
                        <entry key="str">
                            <string>foo</string>
                        </entry>
                        <entry key="int">
                            <int>42</int>
                        </entry>
                    </dict>
                </args>
                <dict/>
                <dict>
                    <entry key="1">
                        <string>1</string>
                    </entry>
                    <entry key="2">
                        <int>42</int>
                    </entry>
                </dict>
            </args>""".trimIndent())

        assert(actual).isEqualTo(expected)
    }
}
