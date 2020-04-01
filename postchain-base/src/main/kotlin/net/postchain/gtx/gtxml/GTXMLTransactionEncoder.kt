// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.gtxml

import net.postchain.base.gtxml.ObjectFactory
import net.postchain.base.gtxml.OperationsType
import net.postchain.base.gtxml.SignaturesType
import net.postchain.base.gtxml.SignersType
import net.postchain.gtv.gtvml.GtvMLEncoder
import net.postchain.gtx.GTXTransactionData
import net.postchain.gtx.OpData
import java.io.StringWriter
import javax.xml.bind.JAXB


object GTXMLTransactionEncoder {

    private val objectFactory = ObjectFactory()

    /**
     * Encodes [GTXData] into XML format
     */
    fun encodeXMLGTXTransaction(gtxTxData: GTXTransactionData): String {
        val transactionType = objectFactory.createTransactionType()
        transactionType.blockchainRID = gtxTxData.transactionBodyData.blockchainRID.toHex()
        transactionType.signers = encodeSigners(gtxTxData.transactionBodyData.signers)
        transactionType.operations = encodeOperations(gtxTxData.transactionBodyData.operations)
        transactionType.signatures = encodeSignature(gtxTxData.signatures)

        val jaxbElement = objectFactory.createTransaction(transactionType)

        val xmlWriter = StringWriter()
        JAXB.marshal(jaxbElement, xmlWriter)

        return xmlWriter.toString()
    }

    private fun encodeSigners(signers: Array<ByteArray>): SignersType {
        return with(objectFactory.createSignersType()) {
            signers.map(objectFactory::createBytea) // See [ObjectFactory.createBytearrayElement]
                    .toCollection(this.signers)
            return this
        }
    }

    private fun encodeOperations(operations: Array<OpData>): OperationsType {
        return with(objectFactory.createOperationsType()) {
            operations.forEach {
                val operationType = objectFactory.createOperationType()
                operationType.name = it.opName
                it.args.map(GtvMLEncoder::encodeGTXMLValueToJAXBElement)
                        .toCollection(operationType.parameters)
                this.operation.add(operationType)
            }
            return this
        }
    }

    private fun encodeSignature(signatures: Array<ByteArray>): SignaturesType {
        return with(objectFactory.createSignaturesType()) {
            signatures.map(objectFactory::createBytea) // See [ObjectFactory.createBytearrayElement]
                    .toCollection(this.signatures)
            return this
        }
    }
}
