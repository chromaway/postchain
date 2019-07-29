package net.postchain.modules.certificate

import net.postchain.base.CryptoSystem
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.gtv.Gtv

class CertificateConfig (
        val cryptoSystem: CryptoSystem,
        val blockchainRID: ByteArray
)

fun makeBaseCertificateConfig(data: Gtv, blockchainRID: ByteArray): CertificateConfig {
    val cs = SECP256K1CryptoSystem()
    return CertificateConfig(
        cs,
        blockchainRID
    )
}