package net.postchain.gtv.gtvml

import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.Test
import net.postchain.gtv.*

class GtvMLParserArraysTest {

    @Test
    fun parseGtv_array_empty_successfully() {
        val xml = "<array></array>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvArray(arrayOf())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_array_of_scalars_successfully() {
        val xml = "<array><string>hello</string><int>42</int></array>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvArray(arrayOf(
                GtvString("hello"),
                GtvInteger(42)
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_array_with_params_successfully() {
        val xml = "<array><string>hello</string><param type='int' key='num'/></array>"
        val actual = GtvMLParser.parseGtvML(
                xml,
                mapOf("num" to GtvInteger(42)))

        val expected = GtvArray(arrayOf(
                GtvString("hello"),
                GtvInteger(42)
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_array_with_not_found_param_throws_exception() {
        val xml = "<array><string>hello</string><param type='int' key='UNKNOWN_KEY'/></array>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("num" to GtvInteger(42)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_array_with_CASE_SENSITIVE_not_found_param_throws_exception() {
        val xml = "<array><string>hello</string><param type='int' key='CASE_SENSITIVE_KEY'/></array>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("case_sensitive_key" to GtvInteger(42)))
    }

    @Test
    fun parseGtv_array_of_arrays_successfully() {
        val xml = """
            <array>
                <array>
                    <string>foo</string>
                    <string>bar</string>
                </array>
                <array>
                    <int>42</int>
                    <int>43</int>
                </array>
            </array>
        """.trimIndent()

        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvArray(arrayOf(
                GtvArray(arrayOf(
                        GtvString("foo"),
                        GtvString("bar")
                )),
                GtvArray(arrayOf(
                        GtvInteger(42),
                        GtvInteger(43)
                ))
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_array_of_all_types_successfully() {
        val xml = """
            <array>
                <string>foo</string>
                <int>42</int>
                <array>
                    <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <string>foo</string>
                    <null/>
                    <string>bar</string>
                    <array>
                        <int>42</int>
                        <int>43</int>
                        <array>
                            <int>44</int>
                        </array>
                        <dict>
                            <entry key="hello"><string>world</string></entry>
                            <entry key="123"><int>123</int></entry>
                        </dict>
                    </array>
                    <dict>
                        <entry key="hello">
                            <array>
                                <string>world</string>
                                <string>world</string>
                            </array>
                        </entry>
                        <entry key="123"><int>123</int></entry>
                    </dict>
                </array>
                <array>
                    <int>42</int>
                    <dict>
                        <entry key="hello"><string>world</string></entry>
                        <entry key="123">
                            <param type='int' key='param_int_123'/>
                        </entry>
                    </dict>
                </array>
                <dict>
                    <entry key="hello"><string>world</string></entry>
                    <entry key="dict123">
                        <dict>
                            <entry key="hello">
                                <param type='string' key='param_string_foo'/>
                            </entry>
                            <entry key="123"><int>123</int></entry>
                        </dict>
                    </entry>
                    <entry key="array123">
                        <array>
                            <int>42</int>
                            <int>43</int>
                        </array>
                    </entry>
                </dict>
            </array>
        """.trimIndent()

        val actual = GtvMLParser.parseGtvML(
                xml,
                mapOf(
                        "param_int_123" to GtvInteger(123),
                        "param_string_foo" to GtvString("foo"))
        )

        val expected = GtvArray(arrayOf(
                GtvString("foo"),
                GtvInteger(42),
                GtvArray(arrayOf(
                        GtvNull,
                        GtvString("foo"),
                        GtvNull,
                        GtvString("bar"),
                        GtvArray(arrayOf(
                                GtvInteger(42),
                                GtvInteger(43),
                                GtvArray(arrayOf(
                                        GtvInteger(44)
                                )),
                                GtvDictionary(mapOf(
                                        "hello" to GtvString("world"),
                                        "123" to GtvInteger(123)
                                ))
                        )),
                        GtvDictionary(mapOf(
                                "hello" to GtvArray(arrayOf(
                                        GtvString("world"),
                                        GtvString("world")
                                )),
                                "123" to GtvInteger(123)
                        ))
                )),
                GtvArray(arrayOf(
                        GtvInteger(42),
                        GtvDictionary(mapOf(
                                "hello" to GtvString("world"),
                                "123" to GtvInteger(123)
                        ))
                )),
                GtvDictionary(mapOf(
                        "hello" to GtvString("world"),
                        "dict123" to GtvDictionary(mapOf(
                                "hello" to GtvString("foo"),
                                "123" to GtvInteger(123)
                        )),
                        "array123" to GtvArray(arrayOf(
                                GtvInteger(42),
                                GtvInteger(43)
                        ))
                ))
        ))

        assert(actual).isEqualTo(expected)
    }
}