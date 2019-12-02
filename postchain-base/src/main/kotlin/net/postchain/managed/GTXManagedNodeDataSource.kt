package net.postchain.managed

import net.postchain.base.BaseBlockHeader
import net.postchain.base.PeerInfo
import net.postchain.config.node.NodeConfig
import net.postchain.core.BlockQueries
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import java.time.Instant

class GTXManagedNodeDataSource(val queries: BlockQueries, val nodeConfig: NodeConfig) : ManagedNodeDataSource {

    override fun getPeerInfos(): Array<PeerInfo> {
        // TODO: [POS-90]: Implement correct error processing

        val res = queries.query("nm_get_peer_infos", buildArgs())
        return res.get().asArray()
                .map {
                    val pia = it.asArray()
                    PeerInfo(
                            pia[0].asString(),
                            pia[1].asInteger().toInt(),
                            pia[2].asByteArray(),
                            Instant.ofEpochMilli(if (pia.size < 4) 0L else pia[3].asInteger())
                    )
                }
                .toTypedArray()
    }

    override fun getPeerListVersion(): Long {
        val res = queries.query("nm_get_peer_list_version", buildArgs())
        return res.get().asInteger()
    }

    override fun computeBlockchainList(ctx: EContext): List<ByteArray> {
        val res = queries.query(
                "nm_compute_blockchain_list",
                buildArgs("node_id" to GtvFactory.gtv(nodeConfig.pubKeyByteArray))
        ).get()

        return res.asArray().map { it.asByteArray() }
    }

    override fun getConfiguration(blockchainRidRaw: ByteArray, height: Long): ByteArray? {
        val res = queries.query(
                "nm_get_blockchain_configuration",
                buildArgs(
                        "blockchain_rid" to GtvFactory.gtv(blockchainRidRaw),
                        "height" to GtvFactory.gtv(height))
        ).get()

        return if (res.isNull()) null else res.asByteArray()
    }

    override fun findNextConfigurationHeight(blockchainRidRaw: ByteArray, height: Long): Long? {
        val res = queries.query(
                "nm_find_next_configuration_height",
                buildArgs(
                        "blockchain_rid" to GtvFactory.gtv(blockchainRidRaw),
                        "height" to GtvFactory.gtv(height))
        ).get()

        return if (res.isNull()) null else res.asInteger()
    }

    private fun buildArgs(vararg args: Pair<String, Gtv>): Gtv {
        return GtvFactory.gtv(*args)
    }
}