package net.postchain.gtv.path

import org.junit.Test
import org.junit.Assert

class GtvPathTest {

    @Test
    fun testGtvPath_equals() {
        val keys1: Array<Any> = arrayOf(0, 7, "myKey")
        val keys2: Array<Any> = arrayOf(0, 7, "myKey")
        val path1 = GtvPathFactory.buildFromArrayOfPointers(keys1)
        val path2 = GtvPathFactory.buildFromArrayOfPointers(keys2)
        Assert.assertEquals(path1, path2)
    }

    @Test
    fun testGtvPath_notEquals() {
        val keys1: Array<Any> = arrayOf(0, 7, "myKey")
        val keys2: Array<Any> = arrayOf(0, 8, "myKey")
        val path1 = GtvPathFactory.buildFromArrayOfPointers(keys1)
        val path2 = GtvPathFactory.buildFromArrayOfPointers(keys2)
        Assert.assertNotEquals(path1, path2)
    }

    @Test
    fun testGtvPath_getTail() {

        val ints: Array<Any> = arrayOf(0,7)
        val org = GtvPathFactory.buildFromArrayOfPointers(ints)
        Assert.assertEquals(3, org.size())

        println("Path (size: ${org.size()} ) list: " + org.debugString())

        val firstElement  = firstArrayElement(org)
        Assert.assertEquals(0, firstElement.index)
        val tail1: GtvPath = GtvPath.getTailIfFirstElementIsArrayOfThisIndex(0, org)!!
        Assert.assertEquals(2, tail1.size())
        val tail1Fail: GtvPath? = GtvPath.getTailIfFirstElementIsArrayOfThisIndex(1, org) // Index = 1 won't find anything
        Assert.assertNull(tail1Fail)

        val secondElement = firstArrayElement(tail1)
        Assert.assertEquals(7, secondElement.index)
        val tail2: GtvPath = GtvPath.getTailIfFirstElementIsArrayOfThisIndex(7, tail1)!!
        Assert.assertEquals(1, tail2.size())

        val thirdElement = firstLeafElement(tail2)

    }

    fun firstArrayElement(gtvPath: GtvPath) : ArrayGtvPathElement {
       return gtvPath.pathElements[0] as ArrayGtvPathElement
    }
    fun firstLeafElement(gtvPath: GtvPath) : GtvPathLeafElement {
        return gtvPath.pathElements[0] as GtvPathLeafElement
    }
}