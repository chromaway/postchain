package net.postchain.managed

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
                .map { it.asDict() }
                .filter { it.containsKey("host") && it.containsKey("port") && it.containsKey("pubkey") }
                .map { pid ->
                    PeerInfo(
                            pid.getValue("host").asString(),
                            pid.getValue("port").asInteger().toInt(),
                            pid.getValue("pubkey").asByteArray(),
                            Instant.ofEpochMilli(if (!pid.containsKey("timestamp")) 0L else pid.getValue("timestamp").asInteger())
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

    override fun getConfiguration(blockchainRID: ByteArray, height: Long): ByteArray? {
        val res = queries.query(
                "nm_get_blockchain_configuration",
                buildArgs(
                        "blockchain_rid" to GtvFactory.gtv(blockchainRID),
                        "height" to GtvFactory.gtv(height))
        ).get()

        return if (res.isNull()) null else res.asByteArray()
    }

    override fun findNextConfigurationHeight(blockchainRID: ByteArray, height: Long): Long? {
        val res = queries.query(
                "nm_find_next_configuration_height",
                buildArgs(
                        "blockchain_rid" to GtvFactory.gtv(blockchainRID),
                        "height" to GtvFactory.gtv(height))
        ).get()

        return if (res.isNull()) null else res.asInteger()
    }

    private fun buildArgs(vararg args: Pair<String, Gtv>): Gtv {
        val currentHeightArg: Pair<String, Gtv> =
                "current_height" to GtvFactory.gtv(queries.getBestHeight().get())

        return GtvFactory.gtv(
                *arrayOf(currentHeightArg).plus(args)
        )
    }
}