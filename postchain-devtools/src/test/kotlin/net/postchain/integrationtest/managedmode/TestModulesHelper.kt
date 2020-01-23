// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.managedmode

import net.postchain.base.PeerInfo
import net.postchain.common.toHex
import net.postchain.devtools.PeerNameHelper
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory

object TestModulesHelper {

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
}