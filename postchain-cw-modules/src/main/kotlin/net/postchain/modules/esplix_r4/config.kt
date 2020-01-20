package net.postchain.modules.esplix_r4

import net.postchain.base.CryptoSystem
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.gtv.Gtv

open class EsplixConfig (
        val cryptoSystem: CryptoSystem,
        val blockchainRID: ByteArray
)

fun makeBaseEsplixConfig(data: Gtv, blockchainRID: ByteArray): EsplixConfig {
    val blockchainRid = blockchainRID

    val cs = SECP256K1CryptoSystem()
    return EsplixConfig(
            cs,
            blockchainRid
    )
}
