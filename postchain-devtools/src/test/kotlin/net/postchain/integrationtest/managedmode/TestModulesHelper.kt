package net.postchain.integrationtest.managedmode

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.devtools.PeerNameHelper
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory

object TestModulesHelper {

    fun argCurrentHeight(args: Gtv): Long {
        return args["current_height"]!!.asInteger()
    }

    fun peerInfoToGtv(peerInfo: PeerInfo): Gtv {
        return GtvFactory.gtv(
                GtvFactory.gtv(peerInfo.host),
                GtvFactory.gtv(peerInfo.port.toLong()),
                GtvFactory.gtv(peerInfo.pubKey))
    }

    fun peerInfoAsString(peerInfos: Array<PeerInfo>): String {
        return peerInfos
                .map { PeerNameHelper.peerName(it.pubKey) }
                .toTypedArray()
                .contentToString()
    }

    fun gtvBlockchain0Rid(): Gtv {
        val blockchain0Rid = "0000000000000000000000000000000000000000000000000000000000000000"
        return GtvFactory.gtv(blockchain0Rid.hexStringToByteArray())
    }

}