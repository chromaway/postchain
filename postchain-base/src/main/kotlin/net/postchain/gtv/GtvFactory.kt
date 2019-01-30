package net.postchain.gtv

import net.postchain.core.ProgrammerMistake
import net.postchain.gtv.messages.DictPair
import java.io.ByteArrayInputStream
import net.postchain.gtv.messages.Gtv as RawGtv

/**
 * Responsible for creating various forms of GTV objects.
 */
object GtvFactory {

    fun wrapValue(r: RawGtv): Gtv {
        when (r.choiceID) {
            RawGtv.null_Chosen -> return GtvNull
            RawGtv.byteArrayChosen -> return GtvByteArray(r.byteArray)
            RawGtv.stringChosen -> return GtvString(r.string)
            RawGtv.integerChosen -> return GtvInteger(r.integer)
            RawGtv.dictChosen -> return GtvDictionary(r.dict.associateBy({ it.name }, { wrapValue(it.value) }))
            RawGtv.arrayChosen -> return GtvArray(r.array.map { wrapValue(it) }.toTypedArray())
        }
        throw ProgrammerMistake("Unknown type identifier")
    }

    fun decodeGtv(bytes: ByteArray): Gtv {
        return wrapValue(RawGtv.der_decode(ByteArrayInputStream(bytes)))
    }

    // helper methods:
    fun gtv(i: Long): Gtv {
        return GtvInteger(i)
    }

    fun gtv(s: String): Gtv {
        return GtvString(s)
    }

    fun gtv(ba: ByteArray): Gtv {
        return GtvByteArray(ba)
    }

    fun gtv(vararg a: Gtv): Gtv {
        return GtvArray(a)
    }

    fun gtv(a: List<Gtv>): Gtv {
        return GtvArray(a.toTypedArray())
    }

    fun gtv(vararg pairs: Pair<String, Gtv>): Gtv {
        return GtvDictionary(mapOf(*pairs))
    }

    fun gtv(dict: Map<String, Gtv>): Gtv {
        return GtvDictionary(dict)
    }


    fun makeDictPair(name: String, value: RawGtv): DictPair {
        val dp = DictPair()
        dp.name = name
        dp.value = value
        return dp
    }
}