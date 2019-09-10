package net.postchain.managed

import net.postchain.base.PeerInfo
import net.postchain.config.node.NodeConfig
import net.postchain.core.BlockQueries
import net.postchain.core.EContext
import net.postchain.gtv.GtvFactory

class GTXManagedNodeDataSource(val queries: BlockQueries, val nodeConfig: NodeConfig) : ManagedNodeDataSource {

    override fun getPeerInfos(): Array<PeerInfo> {
        val res = queries.query("nm_get_peer_infos", GtvFactory.gtv(mapOf()))
        return res.get().asArray()
                .map {
                    val pia = it.asArray()
                    PeerInfo(
                            pia[0].asString(),
                            pia[1].asInteger().toInt(),
                            pia[2].asByteArray()
                    )
                }
                .toTypedArray()
    }

    override fun getPeerListVersion(): Long {
        val res = queries.query("nm_get_peer_list_version", GtvFactory.gtv(mapOf()))
        return res.get().asInteger()
    }

    override fun computeBlockchainList(ctx: EContext): List<ByteArray> {
        val res = queries.query("nm_compute_blockchain_list",
                GtvFactory.gtv("node_id" to GtvFactory.gtv(nodeConfig.pubKeyByteArray))
        )
        return res.get().asArray().map { it.asByteArray() }
    }

    override fun getConfiguration(blockchainRID: ByteArray, height: Long): ByteArray? {
        val res = queries.query("nm_get_blockchain_configuration",
                GtvFactory.gtv("blockchain_rid" to GtvFactory.gtv(blockchainRID),
                        "height" to GtvFactory.gtv(height))).get()
        return if (res.isNull()) null else res.asByteArray()
    }

    override fun findNextConfigurationHeight(blockchainRID: ByteArray, height: Long): Long? {
        val res = queries.query("nm_find_next_configuration_height",
                GtvFactory.gtv("blockchain_rid" to GtvFactory.gtv(blockchainRID),
                        "height" to GtvFactory.gtv(height))).get()
        return if (res.isNull()) null else res.asInteger()
    }
}