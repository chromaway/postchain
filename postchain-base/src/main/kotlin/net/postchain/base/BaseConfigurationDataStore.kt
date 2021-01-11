// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.toHex
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.core.ConfigurationDataStore
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvEncoder

object BaseConfigurationDataStore : KLogging(), ConfigurationDataStore {

    override fun findConfigurationHeightForBlock(context: EContext, height: Long): Long? {
        return DatabaseAccess.of(context).findConfigurationHeightForBlock(context, height)
    }

    override fun getConfigurationData(context: EContext, height: Long): ByteArray? {
        return DatabaseAccess.of(context).getConfigurationData(context, height)
    }

    override fun addConfigurationData(context: EContext, height: Long, binData: ByteArray) {
        return DatabaseAccess.of(context).addConfigurationData(
                context, height, binData)
    }


    override fun addConfigurationData(eContext: EContext, height: Long, gtvData: Gtv, allowUnknownSigners: Boolean) {
        val db = DatabaseAccess.of(eContext)
        db.addConfigurationData(eContext, height, GtvEncoder.encodeGtv(gtvData))

        /* When a new (height > 0) configuration is added, we automatically add signers in that config to table
        blockchainReplicaNodes (for current blockchain). Useful for synchronization. */
        if (height > 0) {
            val brid = db.getBlockchainRid(eContext)!!
            val confdict = gtvData as GtvDictionary
            val signers = confdict[BaseBlockchainConfigurationData.KEY_SIGNERS]!!.asArray().map { it.asByteArray() }
            for (sig in signers) {
                val nodePubkey = sig.toHex()
                // Node must be in PeerInfo, or else it cannot be a blockchain replica.
                val foundInPeerInfo = db.findPeerInfo(eContext, null, null, nodePubkey)
                if (foundInPeerInfo.isNotEmpty()) {
                    db.addBlockchainReplica(eContext, brid.toHex(), nodePubkey)
                    // If the node is not in the peerinfo table and we do not allow unknown signers in a configuration,
                    // throw error
                } else if (!allowUnknownSigners) {
                    throw BadDataMistake(BadDataType.MISSING_PEERINFO,
                            "Signer ${nodePubkey} does not exist in peerinfos.")
                }
            }
        }
    }
}