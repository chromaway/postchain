package net.postchain.gtv

import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.core.UserMistake
import java.math.BigInteger

/**
 * Just a base class for all GTVs.
 */
abstract class AbstractGtv : Gtv {

    // This a locally cached object. If it's null it will be calculated.
    private var cachedMerkleHashSummary: MerkleHashSummary? = null

    override operator fun get(index: Int): Gtv {
        throw UserMistake("Type error: array expected")
    }

    override operator fun get(key: String): Gtv? {
        throw UserMistake("Type error: dict expected")
    }

    override fun asString(): String {
        throw UserMistake("Type error: string expected")
    }

    override fun asArray(): Array<out Gtv> {
        throw UserMistake("Type error: array expected")
    }

    override fun isNull(): Boolean {
        return false
    }

    override fun asDict(): Map<String, Gtv> {
        throw UserMistake("Type error: dict expected")
    }

    override fun asInteger(): Long {
        throw UserMistake("Type error: integer expected")
    }

    override fun asBigInteger(): BigInteger {
        throw UserMistake("Type error: big integer expected")
    }

    override fun asBoolean(): Boolean {
        throw UserMistake("Type error: boolean expected")
    }

    override fun asByteArray(convert: Boolean): ByteArray {
        throw UserMistake("Type error: byte array expected")
    }

    override fun nrOfBytes(): Int {
        throw UserMistake("Implementation expected")
    }

    override fun getCachedMerkleHash(): MerkleHashSummary? {
        return cachedMerkleHashSummary
    }

    override fun setCachedMerkleHash(summary: MerkleHashSummary) {
        cachedMerkleHashSummary =  summary
    }

}
