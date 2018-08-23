package net.postchain.base

import net.postchain.base.data.BaseTransactionQueue
import net.postchain.baseStorage
import net.postchain.common.hexStringToByteArray
import net.postchain.core.*
import net.postchain.gtx.decodeGTXValue
import org.apache.commons.configuration2.Configuration

class BaseBlockchainInfrastructure(val config: Configuration) : BlockchainInfrastructure {

    val cryptoSystem = SECP256K1CryptoSystem()
    val blockSigner : Signer
    val subjectID: ByteArray

    init {
        val privKey = config.getString("blocksigningprivkey").hexStringToByteArray()
        val pubKey = secp256k1_derivePubKey(privKey)
        blockSigner = cryptoSystem.makeSigner(pubKey, privKey)
        subjectID = pubKey
    }

    override fun parseConfigurationString(rawData: String, format: String): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration {
        val gtxData = decodeGTXValue(rawConfigurationData)
        val confData = BaseBlockchainConfigurationData(gtxData,
                context.blockchainRID,
                context.chainID,
                context.nodeID,
                blockSigner,
                subjectID
        )
        val bcfClass = Class.forName(confData.data["configurationfactory"]!!.asString())
        val factory = (bcfClass.newInstance() as BlockchainConfigurationFactory)

        return factory.makeBlockchainConfiguration(confData, context)
    }

    override fun makeBlockchainEngine(bc: BlockchainConfiguration): BaseBlockchainEngine
    {
        val storage = baseStorage(config, -1) // TODO: nodeID
        val tq = BaseTransactionQueue(config.getInt("queuecapacity", 2500))
        val engine = BaseBlockchainEngine(bc, storage, bc.chainID, tq)
        engine.initializeDB()
        return engine
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine): BlockchainProcess {
        TODO("Nope")
    }

}

class BaseBlockchainInfrastructureFactory : BlockchainInfrastructureFactory {
    override fun makeBlockchainInfrastructure(config: Configuration): BlockchainInfrastructure {
        return BaseBlockchainInfrastructure(config)
    }
}