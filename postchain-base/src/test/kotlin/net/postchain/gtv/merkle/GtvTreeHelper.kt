// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvInteger

object GtvTreeHelper {
    /**
     * Transforms (readable) integers into [GtvArray]
     */
    fun transformIntListToGtvArray(ints: List<Int>): GtvArray {
        return transformGtvsToGtvArray(transformIntToGtv(ints))
    }

    /**
     * Transforms (readable) integers into [GtvInteger] list
     */
    fun transformIntToGtv(ints: List<Int>): MutableList<Gtv> {
        val retList = arrayListOf<Gtv>()
        for (i in ints) {
            retList.add(GtvInteger(i.toLong()))
        }
        return retList
    }

    /**
     * Packs a list of [Gtv] into an [GtvArray]
     */
    fun transformGtvsToGtvArray(gtvList: List<Gtv>): GtvArray {
        val gtvArr: Array<Gtv> = gtvList.toTypedArray()
        return GtvArray(gtvArr)
    }

    /**
     * Transforms (readable) strings and integers into a [GtvDictionary]
     */
    fun transformStringAndIntToGtvDictionary(strings: List<String>, ints: List<Int>): GtvDictionary {
        if (strings.size != ints.size) {
            throw IllegalArgumentException("Cannot make a Dict if we don't have equal amount of keys and content")
        }
        val dict = HashMap<String, Gtv>()

        for (i in 0..(strings.size - 1)) {
            val key = strings[i]
            val content = GtvInteger(ints[i].toLong())
            dict.set(key, content)
        }
        return GtvDictionary.build(dict)

    }
}