package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.core.ProgrammerMistake
import net.postchain.gtx.*

/**
 * This should be the serialization we use in production
 *
 * @param gtxValue to serialize
 * @return the byte array containing serialized data
 */
fun serializeGTXValueToByteArary(gtxValue: GTXValue): ByteArray {
    return when (gtxValue) {
        is GTXNull ->           encodeGTXValue(gtxValue)
        is IntegerGTXValue ->   encodeGTXValue(gtxValue)
        is StringGTXValue ->    encodeGTXValue(gtxValue)
        is ByteArrayGTXValue -> encodeGTXValue(gtxValue)
        is ArrayGTXValue ->     throw ProgrammerMistake("GTXValue is an array (We should have transformed all collection-types to trees by now)")
        is DictGTXValue ->      throw ProgrammerMistake("GTXValue is an dict (We should have transformed all collection-types to trees by now)")
        else -> {
            // TODO: Log a warning here? We don't know what this is!
            encodeGTXValue(gtxValue) // Hope for the best
        }
    }
}

/**
 * The calculator intended to be used is production for trees that hold [GTXValue]
 */
class GtxMerkleHashCalculator(cryptoSystem: CryptoSystem): MerkleHashCalculator<GTXValue>(cryptoSystem) {

    var baseCalc: BinaryNodeHashCalculator
    init {
        baseCalc = BinaryNodeHashCalculatorBase(cryptoSystem)
    }

    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        return baseCalc.calculateNodeHash(prefix, hashLeft, hashRight)
    }

    /**
     * Leafs hashes are prefixed to tell them apart from internal nodes
     *
     * @param value The leaf
     * @return Returns the hash of the leaf.
     */
    override fun calculateLeafHash(value: GTXValue): Hash {
        return calculateHashOfValueInternal(value, ::serializeGTXValueToByteArary, MerkleBasics::hashingFun)
    }



}