package net.postchain.gtx

import org.junit.Test
import org.junit.Assert

class GTXPathTest {

    @Test
    fun testGTXPath_getTail() {

        val ints: Array<Any> = arrayOf(0,7)
        val org = GTXPathFactory.buildFromArrayOfPointers(ints)
        Assert.assertEquals(3, org.size())

        println("Path (size: ${org.size()} ) list: " + org.debugString())

        val firstElement  = firstArrayElement(org)
        Assert.assertEquals(0, firstElement.index)
        val tail1: GTXPath = GTXPath.getTailIfFirstElementIsArrayOfThisIndex(0, org)!!
        Assert.assertEquals(2, tail1.size())
        val tail1Fail: GTXPath? = GTXPath.getTailIfFirstElementIsArrayOfThisIndex(1, org) // Index = 1 won't find anything
        Assert.assertNull(tail1Fail)

        val secondElement = firstArrayElement(tail1)
        Assert.assertEquals(7, secondElement.index)
        val tail2: GTXPath = GTXPath.getTailIfFirstElementIsArrayOfThisIndex(7, tail1)!!
        Assert.assertEquals(1, tail2.size())

        val thirdElement = firstLeafElement(tail2)

    }

    fun firstArrayElement(gtxPath: GTXPath) : ArrayGTXPathElement {
       return gtxPath.pathElements[0] as ArrayGTXPathElement
    }
    fun firstLeafElement(gtxPath: GTXPath) : LeafGTXPathElement {
        return gtxPath.pathElements[0] as LeafGTXPathElement
    }
}