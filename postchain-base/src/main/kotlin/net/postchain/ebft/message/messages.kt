// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.message

import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import net.postchain.core.UserMistake
import net.postchain.gtv.*

class SignedMessage(val message: ByteArray, val pubKey: ByteArray, val signature: ByteArray) {

    companion object {
        fun decode(bytes: ByteArray): SignedMessage {
            try {
                val gtvArray = GtvFactory.decodeGtv(bytes) as GtvArray

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
    GETBLOCKSIG, COMPLETEBLOCK, GETBLOCKATHEIGHT, GETUNFINISHEDBLOCK
}

abstract class Message(val type: Int) {

    companion object {
        fun decode(bytes: ByteArray): Message {
            val data =  GtvFactory.decodeGtv(bytes) as GtvArray
            val type = data[0].asInteger().toInt()
            return when (type) {
                MessageType.ID.ordinal -> Identification(data[1].asByteArray(), data[2].asByteArray(), data[3].asInteger())
                MessageType.STATUS.ordinal -> Status(data[1].asByteArray(), data[2].asInteger(), data[3].asBoolean(), data[4].asInteger(), data[5].asInteger(), data[6].asInteger().toInt())
                MessageType.TX.ordinal -> Transaction(data[1].asByteArray())
                MessageType.SIG.ordinal -> Signature(data[1].asByteArray(), data[2].asByteArray())
                MessageType.BLOCKSIG.ordinal -> BlockSignature(data[1].asByteArray(), Signature(data[2].asByteArray(), data[3].asByteArray()))
                MessageType.GETBLOCKSIG.ordinal -> GetBlockSignature(data[1].asByteArray())
                MessageType.BLOCKDATA.ordinal -> BlockData(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() })
                MessageType.COMPLETEBLOCK.ordinal -> CompleteBlock(BlockData(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() }), data[3].asInteger(), data[4].asByteArray())
                MessageType.GETBLOCKATHEIGHT.ordinal -> GetBlockAtHeight(data[1].asInteger())
                MessageType.GETUNFINISHEDBLOCK.ordinal -> GetUnfinishedBlock(data[1].asByteArray())
                MessageType.UNFINISHEDBLOCK.ordinal -> UnfinishedBlock(data[1].asByteArray(), data[2].asArray().map { it.asByteArray() })
                else -> throw ProgrammerMistake("Message type $type is not handled")
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

class Identification(val pubKey: ByteArray, val blockchainRID: ByteArray, val timestamp: Long): Message(MessageType.ID.ordinal) {

    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(pubKey),
                GtvFactory.gtv(blockchainRID), GtvFactory.gtv(timestamp))
    }
}

class Status(val blockRID: ByteArray, val height: Long, val revolting: Boolean, val round: Long,
                  val serial: Long, val state: Int): Message(MessageType.STATUS.ordinal) {


    override fun toGtv(): Gtv {
        return GtvFactory.gtv(GtvFactory.gtv(type.toLong()), GtvFactory.gtv(blockRID), GtvFactory.gtv(height),
                GtvFactory.gtv(revolting), GtvFactory.gtv(round), GtvFactory.gtv(serial), GtvFactory.gtv(state.toLong()))
    }
}




