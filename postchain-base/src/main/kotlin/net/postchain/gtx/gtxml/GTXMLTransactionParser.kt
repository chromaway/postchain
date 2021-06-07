// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.gtxml

import net.postchain.base.BlockchainRid
import net.postchain.base.CryptoSystem
import net.postchain.base.SigMaker
import net.postchain.base.gtxml.OperationsType
import net.postchain.base.gtxml.ParamType
import net.postchain.base.gtxml.SignersType
import net.postchain.base.gtxml.TransactionType
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.common.hexStringToByteArray
import net.postchain.core.ByteArrayKey
import net.postchain.core.byteArrayKeyOf
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import net.postchain.gtx.OpData
import java.io.StringReader
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBElement

class TransactionContext(val blockchainRID: BlockchainRid?,
                         val params: Map<String, Gtv> = mapOf(),
                         val autoSign: Boolean = false,
                         val signers: Map<ByteArrayKey, SigMaker> = mapOf()) {

    companion object {
        fun empty() = TransactionContext(null)
    }
}


object GTXMLTransactionParser {

    /**
     * Parses XML represented as string into [GTXTransactionData] within the [TransactionContext]
     */
    fun parseGTXMLTransaction(xml: String, context: TransactionContext, cs: CryptoSystem): GTXTransactionData {
        return parseGTXMLTransaction(
                JAXB.unmarshal(StringReader(xml), TransactionType::class.java),
                context,
                cs)
    }

    /**
     * Parses XML represented as string into [GTXData] within the jaxbContext of [params] ('<param />') and [signers]
    fun parseGTXMLTransaction(xml: String,
                              params: Map<String, Gtv> = mapOf(),
                              signers: Map<ByteArrayKey, Signer> = mapOf()): GTXData {

        return parseGTXMLTransaction(
                xml,
                TransactionContext(null, params, true, signers))
    }
     */

    /**
     * TODO: [et]: Parses XML represented as string into [GTXData] within the [TransactionContext]
     */
    fun parseGTXMLTransaction(transaction: TransactionType, context: TransactionContext, cs: CryptoSystem): GTXTransactionData {
        // Asserting count(signers) == count(signatures)
        requireSignaturesCorrespondsSigners(transaction)

        val rid= parseBlockchainRID(transaction.blockchainRID, context.blockchainRID)
        val signers = parseSigners(transaction.signers, context.params)
        val signatures = parseSignatures(transaction, context.params)
        val ops = parseOperations(transaction.operations, context.params)

        val txBody = GTXTransactionBodyData(rid, ops, signers)
        val tx = GTXTransactionData(txBody, signatures)

        if (context.autoSign) {
            val calculator = GtvMerkleHashCalculator(cs)
            signTransaction(tx, context.signers, calculator)
        }

        return tx
    }

    private fun requireSignaturesCorrespondsSigners(tx: TransactionType) {
        if (tx.signatures != null && tx.signers.signers.size != tx.signatures.signatures.size) {
            throw IllegalArgumentException("Number of signers (${tx.signers.signers.size}) is not equal to " +
                    "the number of signatures (${tx.signatures.signatures.size})\n")
        }
    }

    private fun parseBlockchainRID(blockchainRID: String?, contextBlockchainRID: BlockchainRid?): BlockchainRid {
        return if (blockchainRID.isNullOrEmpty()) {
            contextBlockchainRID ?: BlockchainRid.ZERO_RID
        } else {
            val data = blockchainRID.hexStringToByteArray()
                    .takeIf { contextBlockchainRID == null || it.contentEquals(contextBlockchainRID.data) }
                    ?: throw IllegalArgumentException(
                            "BlockchainRID = '$blockchainRID' of parsed xml transaction is not equal to " +
                                    "TransactionContext.blockchainRID = '${contextBlockchainRID!!.toHex()}'"
                    )
            BlockchainRid(data)
        }
    }

    private fun parseSigners(signers: SignersType, params: Map<String, Gtv>): Array<ByteArray> {
        return signers.signers
                .map { parseJAXBElementToByteArrayOrParam(it, params) }
                .toTypedArray()
    }

    private fun parseSignatures(transaction: TransactionType, params: Map<String, Gtv>): Array<ByteArray> {
        return if (transaction.signatures != null) {
            transaction.signatures.signatures
                    .map { parseJAXBElementToByteArrayOrParam(it, params) }
                    .toTypedArray()
        } else {
            Array(transaction.signers.signers.size) { byteArrayOf() }
        }
    }

    private fun parseJAXBElementToByteArrayOrParam(jaxbElement: JAXBElement<*>, params: Map<String, Gtv>): ByteArray {
        // TODO: [et]: Add better error handling
        return if (jaxbElement.value is ParamType) {
            params[(jaxbElement.value as ParamType).key]
                    ?.asByteArray()
                    ?: throw IllegalArgumentException("Unknown type of GTXMLValue")
        } else {
            jaxbElement.value as? ByteArray ?: byteArrayOf()
        }
    }

    private fun parseOperations(operations: OperationsType, params: Map<String, Gtv>): Array<OpData> {
        return operations.operation.map {
            OpData(
                    it.name,
                    it.parameters.map {
                        GtvMLParser.parseJAXBElementToGtvML(it, params)
                    }.toTypedArray())
        }.toTypedArray()
    }

    /**
     * Will provide all missing signatures
     *
     * @param tx is the transaction to sign
     * @param signersMap is a map that tells us what [SigMaker] should be usd for each signer
     */
    private fun signTransaction(tx: GTXTransactionData, signersMap: Map<ByteArrayKey, SigMaker>, calculator: MerkleHashCalculator<Gtv> ) {
        val txSigners = tx.transactionBodyData.signers
        val txBodyMerkleRoot = tx.transactionBodyData.calculateRID(calculator)
        for (i in 0 until txSigners.size) {
            if (tx.signatures[i].isEmpty()) {
                val key = txSigners[i].byteArrayKeyOf()
                val sigMaker = signersMap[key] ?: throw IllegalArgumentException("Signer $key is absent")
                tx.signatures[i] = sigMaker.signDigest(txBodyMerkleRoot).data
            }
        }
    }
}