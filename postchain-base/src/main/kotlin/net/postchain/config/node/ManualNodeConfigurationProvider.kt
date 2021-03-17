// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.app.AppConfig
import net.postchain.network.x.XPeerID

/**
 *
 */
open class ManualNodeConfigurationProvider(
        protected val appConfig: AppConfig,
        createStorage: (AppConfig) -> Storage
) : NodeConfigurationProvider {

    private val storage = createStorage(appConfig)

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = getPeerInfoCollection(appConfig)
                    .associateBy(PeerInfo::peerId)
            override val blockchainReplicaNodes = getBlockchainReplicaCollection(appConfig)
            override val mustSyncUntilHeight: Map<Long, Long>? = getSyncUntilHeight(appConfig)
        }
    }

    override fun close() {
        storage.close()
    }

    /**
     *
     *
     * @param appConfig is the
     * @return the [PeerInfo] this node should know about
     */
    // TODO: [et]: Make it protected
    open fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        return storage.withReadConnection { ctx ->
            DatabaseAccess.of(ctx).getPeerInfoCollection(ctx)
        }
    }
    open fun getBlockchainReplicaCollection(appConfig: AppConfig): Map<BlockchainRid, List<XPeerID>> {
        return storage.withReadConnection { ctx ->
            DatabaseAccess.of(ctx).getBlockchainReplicaCollection(ctx)
        }
    }

    open fun getSyncUntilHeight(appConfig: AppConfig): Map<Long, Long> {
        return storage.withReadConnection { ctx ->
            DatabaseAccess.of(ctx).getMustSyncUntil(ctx)
        }
    }

    open fun getChainIDs(appConfig: AppConfig): Map<BlockchainRid, Long> {
        return storage.withReadConnection { ctx ->
            DatabaseAccess.of(ctx).getChainIds(ctx)
        }
    }
}