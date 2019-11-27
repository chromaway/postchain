package net.postchain.integrationtest.managedmode

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.devtools.PeerNameHelper
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory

object TestModulesHelper {

    val BLOCKCHAIN_RIDS = mapOf(
            0L to "0000000000000000000000000000000000000000000000000000000000000000",
            1L to "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB1577A3",
            2L to "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB1577A4",
            100L to "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB000100",
            101L to "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB000101"
    )
    fun argCurrentHeight(args: Gtv): Long {
        return args["current_height"]?.asInteger() ?: -1
    }

    fun argHeight(args: Gtv): Long {
        return args["height"]?.asInteger() ?: -1
    }

    fun argBlockchainRid(args: Gtv): String {
        return args["blockchain_rid"]?.asByteArray()?.toHex() ?: ""
    }

    fun peerInfoToGtv(peerInfo: PeerInfo): Gtv {
        return GtvFactory.gtv(
                GtvFactory.gtv(peerInfo.host),
                GtvFactory.gtv(peerInfo.port.toLong()),
                GtvFactory.gtv(peerInfo.pubKey),
                GtvFactory.gtv(peerInfo.timestamp?.toEpochMilli() ?: 0))
    }

    fun peerInfoAsString(peerInfos: Array<PeerInfo>): String {
        return peerInfos
                .map { PeerNameHelper.peerName(it.pubKey) }
                .toTypedArray()
                .contentToString()
    }

    fun gtvBlockchain0Rid(): Gtv {
        return gtvBlockchainRid(0L)
    }

    fun gtvBlockchainRid(chainId: Long): Gtv {
        return GtvFactory.gtv(
                BLOCKCHAIN_RIDS[chainId]?.hexStringToByteArray() ?: byteArrayOf())
    }
}