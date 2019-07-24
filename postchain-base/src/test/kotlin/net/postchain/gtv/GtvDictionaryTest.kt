package net.postchain.gtv

import net.postchain.gtv.GtvFactory.gtv
import org.junit.Test
import kotlin.test.assertEquals

class GtvDictionaryTest {

    @Test
    fun testSortingOfKeys() {

        val myMap = hashMapOf(
                "x" to gtv(100),
                "ccc" to gtv(3),
                "1" to gtv(-1),
                "b" to gtv(2),
                "a" to gtv(1))

        val dict = GtvDictionary.build(myMap)
        val resList = mutableListOf<String>()
        dict.dict.forEach{
            resList.add(it.key)
        }

        val expected = listOf("1", "a", "b", "ccc", "x") // Sorted
        assertEquals(expected, resList.toList())

    }

}