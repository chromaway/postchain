package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash

object BlockchainRidFactory {

    val cryptoSystem = SECP256K1CryptoSystem()
    val merkleHashCalculator = GtvMerkleHashCalculator(cryptoSystem)

    /**
     * Check if there is a Blockchain RID for this chain already
     * If no Blockchain RID, we use this config to create a BC RID, since this MUST be the first config, and store to DB.
     *
     * TODO: Olle Can we check in any other way if the GTV config we are holding is NOT the first config?
     *
     * @param data is the [Gtv] data of the configuration
     * @param merkleHashCalculator needed if the BC RID does not exist
     * @param eContext
     * @return the blockchain's RID, either old or just created
     */
    fun resolveBlockchainRID(
            data: Gtv,
            eContext: EContext
    ): ByteArray {
        val dbBcRid = DatabaseAccess.of(eContext).getBlockchainRID(eContext)
        if (dbBcRid != null) {
            return dbBcRid // We have it in the DB so don't do anything (if it's in here it must be correct)
        } else {
            // Need to calculate it the RID, and we do it the usual way (same as merkle root of block)
            val bcRid = data.merkleHash(merkleHashCalculator)
            DatabaseAccess.of(eContext).checkBlockchainRID(eContext, bcRid)
            return bcRid
        }
    }
}