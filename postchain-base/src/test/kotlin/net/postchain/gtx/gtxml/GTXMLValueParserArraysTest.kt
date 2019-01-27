package net.postchain.gtx.gtxml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.gtx.*
import org.junit.Test

class GTXMLValueParserArraysTest {

    @Test
    fun parseGTXValue_array_empty_successfully() {
        val xml = "<args></args>"
        val actual = GTXMLValueParser.parseGTXMLValue(xml)
        val expected = ArrayGTXValue(arrayOf())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGTXValue_array_of_scalars_successfully() {
        val xml = "<args><string>hello</string><int>42</int></args>"
        val actual = GTXMLValueParser.parseGTXMLValue(xml)
        val expected = ArrayGTXValue(arrayOf(
                StringGTXValue("hello"),
                IntegerGTXValue(42)
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGTXValue_array_with_params_successfully() {
        val xml = "<args><string>hello</string><param type='int' key='num'/></args>"
        val actual = GTXMLValueParser.parseGTXMLValue(
                xml,
                mapOf("num" to IntegerGTXValue(42)))

        val expected = ArrayGTXValue(arrayOf(
                StringGTXValue("hello"),
                IntegerGTXValue(42)
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXValue_array_with_not_found_param_throws_exception() {
        val xml = "<args><string>hello</string><param type='int' key='UNKNOWN_KEY'/></args>"
        GTXMLValueParser.parseGTXMLValue(
                xml,
                mapOf("num" to IntegerGTXValue(42)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGTXValue_array_with_CASE_SENSITIVE_not_found_param_throws_exception() {
        val xml = "<args><string>hello</string><param type='int' key='CASE_SENSITIVE_KEY'/></args>"
        GTXMLValueParser.parseGTXMLValue(
                xml,
                mapOf("case_sensitive_key" to IntegerGTXValue(42)))
    }

    @Test
    fun parseGTXValue_array_of_arrays_successfully() {
        val xml = """
            <args>
                <args>
                    <string>foo</string>
                    <string>bar</string>
                </args>
                <args>
                    <int>42</int>
                    <int>43</int>
                </args>
            </args>
        """.trimIndent()

        val actual = GTXMLValueParser.parseGTXMLValue(xml)
        val expected = ArrayGTXValue(arrayOf(
                ArrayGTXValue(arrayOf(
                        StringGTXValue("foo"),
                        StringGTXValue("bar")
                )),
                ArrayGTXValue(arrayOf(
                        IntegerGTXValue(42),
                        IntegerGTXValue(43)
                ))
        ))

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGTXValue_array_of_all_types_successfully() {
        val xml = """
            <args>
                <string>foo</string>
                <int>42</int>
                <args>
                    <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                    <string>foo</string>
                    <null/>
                    <string>bar</string>
                    <args>
                        <int>42</int>
                        <int>43</int>
                        <args>
                            <int>44</int>
                        </args>
                        <dict>
                            <entry key="hello"><string>world</string></entry>
                            <entry key="123"><int>123</int></entry>
                        </dict>
                    </args>
                    <dict>
                        <entry key="hello">
                            <args>
                                <string>world</string>
                                <string>world</string>
                            </args>
                        </entry>
                        <entry key="123"><int>123</int></entry>
                    </dict>
                </args>
                <args>
                    <int>42</int>
                    <dict>
                        <entry key="hello"><string>world</string></entry>
                        <entry key="123">
                            <param type='int' key='param_int_123'/>
                        </entry>
                    </dict>
                </args>
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
                        <args>
                            <int>42</int>
                            <int>43</int>
                        </args>
                    </entry>
                </dict>
            </args>
        """.trimIndent()

        val actual = GTXMLValueParser.parseGTXMLValue(
                xml,
                mapOf(
                        "param_int_123" to IntegerGTXValue(123),
                        "param_string_foo" to StringGTXValue("foo"))
        )

        val expected = ArrayGTXValue(arrayOf(
                StringGTXValue("foo"),
                IntegerGTXValue(42),
                ArrayGTXValue(arrayOf(
                        GTXNull,
                        StringGTXValue("foo"),
                        GTXNull,
                        StringGTXValue("bar"),
                        ArrayGTXValue(arrayOf(
                                IntegerGTXValue(42),
                                IntegerGTXValue(43),
                                ArrayGTXValue(arrayOf(
                                        IntegerGTXValue(44)
                                )),
                                DictGTXValue(mapOf(
                                        "hello" to StringGTXValue("world"),
                                        "123" to IntegerGTXValue(123)
                                ))
                        )),
                        DictGTXValue(mapOf(
                                "hello" to ArrayGTXValue(arrayOf(
                                        StringGTXValue("world"),
                                        StringGTXValue("world")
                                )),
                                "123" to IntegerGTXValue(123)
                        ))
                )),
                ArrayGTXValue(arrayOf(
                        IntegerGTXValue(42),
                        DictGTXValue(mapOf(
                                "hello" to StringGTXValue("world"),
                                "123" to IntegerGTXValue(123)
                        ))
                )),
                DictGTXValue(mapOf(
                        "hello" to StringGTXValue("world"),
                        "dict123" to DictGTXValue(mapOf(
                                "hello" to StringGTXValue("foo"),
                                "123" to IntegerGTXValue(123)
                        )),
                        "array123" to ArrayGTXValue(arrayOf(
                                IntegerGTXValue(42),
                                IntegerGTXValue(43)
                        ))
                ))
        ))

        assert(actual).isEqualTo(expected)
    }
}