// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.gtvml

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import net.postchain.gtv.*
import org.junit.Test

class GtvMLEncodeArraysTest {

    @Test
    fun encodeXMLGtv_array_empty_successfully() {
        val gtv = GtvArray(arrayOf())
        val actual = GtvMLEncoder.encodeXMLGtv(gtv)
        val expected = arrayOf(
                expected("<array></array>"),
                expected("<array/>"))

        assert(actual).isIn(*expected)
    }

    @Test
    fun encodeXMLGtv_array_successfully() {
        val gtv = GtvArray(arrayOf(
                GtvString("hello"),
                GtvInteger(42)))
        val actual = GtvMLEncoder.encodeXMLGtv(gtv)
        val expected = expected("""
            <array>
                <string>hello</string>
                <int>42</int>
            </array>""".trimIndent())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun encodeXMLGtv_compound_array_successfully() {
        val gtv = GtvArray(arrayOf(
                GtvNull,
                GtvString("hello"),
                GtvInteger(42),
                GtvArray(arrayOf()),
                GtvArray(arrayOf(
                        GtvArray(arrayOf(
                                GtvDictionary.build(mapOf(
                                        "0" to GtvNull,
                                        "1" to GtvString("1"),
                                        "2" to GtvInteger(2)
                                ))
                        )),
                        GtvDictionary.build(mapOf(
                                "array" to GtvArray(arrayOf(
                                        GtvInteger(1),
                                        GtvString("2")
                                )),
                                "str" to GtvString("foo"),
                                "int" to GtvInteger(42)
                        ))
                )),
                GtvDictionary.build(mapOf()),
                GtvDictionary.build(mapOf(
                        "1" to GtvString("1"),
                        "2" to GtvInteger(42)
                ))
        ))

        val actual = GtvMLEncoder.encodeXMLGtv(gtv)

        val expected = expected("""
            <array>
                <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                <string>hello</string>
                <int>42</int>
                <array/>
                <array>
                    <array>
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
                    </array>
                    <dict>
                        <entry key="array">
                            <array>
                                <int>1</int>
                                <string>2</string>
                            </array>
                        </entry>
                        <entry key="int">
                            <int>42</int>
                        </entry>
                        <entry key="str">
                            <string>foo</string>
                        </entry>
                    </dict>
                </array>
                <dict/>
                <dict>
                    <entry key="1">
                        <string>1</string>
                    </entry>
                    <entry key="2">
                        <int>42</int>
                    </entry>
                </dict>
            </array>""".trimIndent())

        assert(actual).isEqualTo(expected)
    }
}
