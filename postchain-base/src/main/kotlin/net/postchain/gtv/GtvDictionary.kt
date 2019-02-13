package net.postchain.gtv

import net.postchain.gtv.messages.DictPair
import net.postchain.gtv.messages.Gtv as RawGtv
import java.util.*

data class GtvDictionary(val dict: Map<String, Gtv>) : GtvCollection() {

    override val type = GtvType.DICT

    override operator fun get(key: String): Gtv? {
        return dict[key]
    }

    override fun getSize(): Int {
        return dict.keys.size
    }

    override fun asDict(): Map<String, Gtv> {
        return dict
    }

    override fun getRawGtv(): net.postchain.gtv.messages.Gtv {
        return RawGtv.dict(
                Vector<DictPair>(
                        dict.entries.map { GtvFactory.makeDictPair(it.key, it.value.getRawGtv()) }
                ))
    }

    override fun asPrimitive(): Any? {
        return dict.mapValues {
            it.value.asPrimitive()
        }
    }

    override fun nrOfBytes(): Int {
        // This could be expensive since it will go all the way down to the leaf
        var sumNrOfBytes =0
        for (key in dict.keys) {
            sumNrOfBytes += (key.length * 2)
            sumNrOfBytes += dict[key]!!.nrOfBytes()
        }
        return sumNrOfBytes
    }
}