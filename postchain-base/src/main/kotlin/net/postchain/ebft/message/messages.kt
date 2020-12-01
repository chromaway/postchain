// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.message

import net.postchain.base.BlockchainRid
import net.postchain.common.toHex
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.core.UserMistake
import net.postchain.gtv.*

class SignedMessage(val message: ByteArray, val pubKey: ByteArray, val signature: ByteArray) {

    companion object {
        fun decode(bytes: ByteArray): SignedMessage {
            try {
                val gtvArray = GtvDecoder.decodeGtv(bytes) as GtvArray

                return SignedMessage(gtvArray[0].asByteArray(), gtvArray[1].asByteArray(), gtvArray[2].asByteArray())
            } catch (e: Exception) {
                throw UserMistake("bytes ${bytes.toHex()} cannot be decoded", e)
            }
        }
    }

    fun encode(): ByteArray {
        return GtvEncoder.encodeGtv(toGtv())
    }

    fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(message), GtvFactory.gtv(pubKey), GtvFactory.gtv(signature))
    }
}

enum class MessageType {
    ID, STATUS, TX, SIG, BLOCKSIG, BLOCKDATA, UNFINISHEDBLOCK,
    GETBLOCKSIG, COMPLETEBLOCK, GETBLOCKATHEIGHT, GETUNFINISHEDBLOCK, GETBLOCKHEADERANDBLOCK, BLOCKHEADER
}

abstract class Message(val type: Int) {

    companion object {
        inline fun <reified T:  Message> decode(bytes: ByteArray): T {
            val data =  GtvDecoder.decodeGtv(bytes) as GtvArray
            val type = data[0].asInteger().toInt()
            return when (type) {
                MessageType.ID.ordinal -> Identification(data[1].asByteArray(), BlockchainRid(data[2].asByteArray()), data[3].asInteger()) as T
                MessageType.STATUS.ordinal -> Status(nullableBytearray(data[1]), data[2].asInteger(), data[3].asBoolean(), data[4].asInteger(), data[5].asInteger(), data[6].asInteger().toInt()) as T
                MessageType.TX.ordinal -> Transaction(data[1].asByteArray()) as T
                MessageType.SIG.ordinal -> Signature(data[1].asByteArray(), data[2].asByteArray()) as T
                MessageType.BLOCKSIG.ordinal -> BlockSignature(data[1].asByteArray(), Signature(data[2].asByteArray(), data[3].asByteArray())) as T
                MessageType.GETBLOCKSIG.ordinal -> GetBlockSignature(data[1].asByteArray()) as T
                MessageType.BLOCKDATA.ordinal -> BlockData(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() }) as T
                MessageType.COMPLETEBLOCK.ordinal -> CompleteBlock(BlockData(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() }), data[3].asInteger(), data[4].asByteArray()) as T
                MessageType.GETBLOCKATHEIGHT.ordinal -> GetBlockAtHeight(data[1].asInteger()) as T
                MessageType.GETUNFINISHEDBLOCK.ordinal -> GetUnfinishedBlock(data[1].asByteArray()) as T
                MessageType.UNFINISHEDBLOCK.ordinal -> UnfinishedBlock(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() }) as T
                MessageType.GETBLOCKHEADERANDBLOCK.ordinal -> GetBlockHeaderAndBlock(data[1].asInteger()) as T
                MessageType.BLOCKHEADER.ordinal -> BlockHeader(data[1].asByteArray(), data[2].asByteArray(), data[3].asInteger()) as T
                else -> throw BadDataMistake(BadDataType.BAD_MESSAGE,"Message type $type is not handled")
            }
        }

        fun nullableBytearray(tmpGtv: Gtv): ByteArray? {
            return when (tmpGtv) {
                is GtvNull -> null
                is GtvByteArray -> tmpGtv.asByteArray()
                else -> throw BadDataMistake(BadDataType.BAD_MESSAGE,"Incorrect EBFT status ${tmpGtv.type}")
            }
        }
    }

    abstract fun toGtv(): Gtv

    fun encode(): ByteArray {
        return GtvEncoder.encodeGtv(toGtv())
    }

    override fun toString(): String {
        return this::class.simpleName!!
    }
}

class Transaction(val data: ByteArray): Message(MessageType.TX.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(data))
    }
}

class Signature(val subjectID: ByteArray, val data: ByteArray): Message(MessageType.SIG.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(subjectID), GtvFactory.gtv(data))
    }
}

class BlockSignature(val blockRID: ByteArray, val sig: Signature): Message(MessageType.BLOCKSIG.ordinal) {

    override fun toGtv(): GtvArray {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(blockRID),
                GtvFactory.gtv(sig.subjectID), GtvFactory.gtv(sig.data))
    }
}

class GetBlockSignature(val blockRID: ByteArray): Message(MessageType.GETBLOCKSIG.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(blockRID))
    }
}

class BlockData(val header: ByteArray, val transactions: List<ByteArray>): Message(MessageType.BLOCKDATA.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(header),
                GtvFactory.gtv(transactions.map { GtvFactory.gtv(it) }))
    }
}

class CompleteBlock(val data: BlockData, val height: Long, val witness: ByteArray): Message(MessageType.COMPLETEBLOCK.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()),
                GtvFactory.gtv(data.header), GtvFactory.gtv(data.transactions.map { GtvFactory.gtv(it) }),
                GtvFactory.gtv(height), GtvFactory.gtv(witness))
    }
}

class GetBlockAtHeight(val height: Long): Message(MessageType.GETBLOCKATHEIGHT.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(height))
    }
}

class GetUnfinishedBlock(val blockRID: ByteArray): Message(MessageType.GETUNFINISHEDBLOCK.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(blockRID))
    }
}

class UnfinishedBlock(val header: ByteArray, val transactions: List<ByteArray>): Message(MessageType.UNFINISHEDBLOCK.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(header),
                GtvFactory.gtv(transactions.map { GtvFactory.gtv(it) }))
    }
}

class Identification(val pubKey: ByteArray, val blockchainRID: BlockchainRid, val timestamp: Long): Message(MessageType.ID.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(pubKey),
                GtvFactory.gtv(blockchainRID), GtvFactory.gtv(timestamp))
    }
}

class Status(val blockRID: ByteArray?, val height: Long, val revolting: Boolean, val round: Long,
                  val serial: Long, val state: Int): Message(MessageType.STATUS.ordinal) {


    override fun toGtv(): Gtv {
        val currentBlockRid: Gtv = if (blockRID != null) {
            GtvFactory.gtv(blockRID)
        } else {
            GtvNull
        }
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), currentBlockRid, GtvFactory.gtv(height),
                GtvFactory.gtv(revolting), GtvFactory.gtv(round), GtvFactory.gtv(serial), GtvFactory.gtv(state.toLong()))
    }
}

/**
 * Requests that the peer reply with two messages
 *
 * 1. The BlockHeader at height
 * 2. The UnfinishedBlock at height
 *
 * If the peer doesn't have that block, it will reply with the BlockHeader of its tip
 */
class GetBlockHeaderAndBlock(val height: Long): Message(MessageType.GETBLOCKHEADERANDBLOCK.ordinal) {
    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(height))
    }
}

/**
 * This represent a block header at a requested height or the highest blockheader
 * if requestedHeight didn't exist. A BlockHeader with empty header and witness byte
 * arrays represents that the node has no blocks at all.
 */
class BlockHeader(val header: ByteArray, val witness: ByteArray, val requestedHeight: Long)
    : Message(MessageType.BLOCKHEADER.ordinal) {
    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(header), GtvFactory.gtv(witness),
                GtvFactory.gtv(requestedHeight))
    }
}



