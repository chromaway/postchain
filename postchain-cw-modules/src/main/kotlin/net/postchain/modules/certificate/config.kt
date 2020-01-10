package net.postchain.modules.certificate

import net.postchain.base.BlockchainRid
import net.postchain.base.CryptoSystem
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.gtv.Gtv

class CertificateConfig (
        val cryptoSystem: CryptoSystem,
        val blockchainRID: BlockchainRid
)

fun makeBaseCertificateConfig(data: Gtv, blockchainRID: BlockchainRid): CertificateConfig {
    val cs = SECP256K1CryptoSystem()
    return CertificateConfig(
        cs,
        blockchainRID
    )
}