// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.gtvml

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.gtv.*
import org.junit.Test

class GtvMLParserScalarsTest {

    @Test
    fun parseGtv_null_successfully() {
        val xml = "<null />"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvNull

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_string_successfully() {
        val xml = "<string>hello</string>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvString("hello")

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_int_successfully() {
        val xml = "<int>42</int>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvInteger(42L)

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_bytea_successfully() {
        val xml = "<bytea>0102030A0B0C</bytea>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvByteArray(
                byteArrayOf(0x01, 0x02, 0x03, 0x0A, 0x0B, 0x0C))

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_bytea_empty_successfully() {
        val xml = "<bytea></bytea>"
        val actual = GtvMLParser.parseGtvML(xml)
        val expected = GtvByteArray(
                byteArrayOf())

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_param_successfully() {
        val xml = "<param key='param_key'/>"

        val actual = GtvMLParser.parseGtvML(
                xml,
                mapOf("param_key" to GtvInteger(123)))

        val expected = GtvInteger(123)

        assert(actual).isEqualTo(expected)
    }

    @Test
    fun parseGtv_param_compatible_type_successfully() {
        val xml = "<param key='param_key' type='int'/>"

        val actual = GtvMLParser.parseGtvML(
                xml,
                mapOf("param_key" to GtvInteger(123)))

        val expected = GtvInteger(123)

        assert(actual).isEqualTo(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_param_incompatible_type_successfully() {
        val xml = "<param key='param_key' type='string'/>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("param_key" to GtvArray(arrayOf(GtvInteger(123)))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_param_unknown_type_successfully() {
        val xml = "<param key='param_key' type='UNKNOWN_TYPE'/>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("param_key" to GtvInteger(123)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_param_not_found_throws_exception() {
        val xml = "<param key='param_key_not_found' type='int'/>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("param_key" to GtvInteger(123)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseGtv_array_with_CASE_SENSITIVE_not_found_param_throws_exception() {
        val xml = "<param key='CASE_SENSITIVE_KEY' type='int'/>"
        GtvMLParser.parseGtvML(
                xml,
                mapOf("case_sensitive_key" to GtvInteger(123)))
    }
}