package net.postchain.base.gtv

import net.postchain.base.BlockchainDependencies
import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv

object BlockchainDependencyGtvSerializer {


    /**
     * When we turn the object to GTV we only keep the BC's RID and the Block's RID.
     */
    /*
    fun toGtv(dependecies: BlockchainDependencies): GtvDictionary {
        //val gtvDict = GtvDictionary()
        val gtvMap = mutableMapOf<String, Gtv>()
        for (dep in dependecies.all()) {
            val bcRidHex = dep.blockchainRid.toHex()
            val blockRid = dep.heightDependency ?: throw ProgrammerMistake("There is no block RID for dependency $dep")
            gtvMap[bcRidHex] = gtv(blockRid.lastBlockRid)
        }
        return GtvDictionary(gtvMap)
    }
    */
}