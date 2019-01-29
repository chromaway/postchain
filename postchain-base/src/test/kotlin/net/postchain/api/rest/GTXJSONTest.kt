// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api.rest

import net.postchain.gtx.make_gtx_gson
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.Assert
import org.junit.Test

class GTXJSONTest {

    @Test
    fun testSerialization() {
        val gson = make_gtx_gson()
        val jsonValue = JsonObject()
        jsonValue.add("foo", JsonPrimitive("bar"))
        jsonValue.add("bar", JsonPrimitive("1234"))
        val Gtv = gson.fromJson<Gtv>(jsonValue, Gtv::class.java)!!
        Assert.assertEquals("bar", Gtv["foo"]!!.asString())
        Assert.assertEquals("1234", Gtv["bar"]!!.asString())
        Assert.assertTrue(Gtv["bar"]!!.asByteArray(true).size == 2)
    }

    @Test
    fun testBoth() {
        val gson = make_gtx_gson()
        val Gtv = gtv("foo" to gtv("bar"), "bar" to gtv("1234".hexStringToByteArray()))
        val jsonValue = gson.toJson(Gtv, Gtv::class.java)
        println(jsonValue)
        val Gtv2 = gson.fromJson<Gtv>(jsonValue, Gtv::class.java)!!
        Assert.assertEquals("bar", Gtv2["foo"]!!.asString())
        Assert.assertEquals("1234", Gtv2["bar"]!!.asString())
        Assert.assertTrue(Gtv2["bar"]!!.asByteArray(true).size == 2)
    }


}