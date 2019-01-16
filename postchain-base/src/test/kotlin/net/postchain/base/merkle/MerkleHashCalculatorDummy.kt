package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.proof.GtxMerkleProofTree
import net.postchain.base.merkle.proof.GtxMerkleProofTreeFactory
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import net.postchain.gtx.StringGTXValue
import java.nio.charset.Charset


/**
 * We create a dummy serializer so that we can use for tests
 * It truncates an Int to one byte
 */
fun dummySerializatorFun(iGtx: GTXValue): ByteArray {
    when (iGtx) {
        is IntegerGTXValue -> {
            val i: Long = iGtx.integer
            if (i > 127 && i > -1) {
                throw IllegalArgumentException("Test integers should be positive and should not be bigger than 127: $i")
            } else {
                val b: Byte = i.toByte()
                return byteArrayOf(b)
            }
        }
        is StringGTXValue -> {
            val str = iGtx.string
            val byteArr = str.toByteArray(Charset.defaultCharset()) // TODO: Do we need to think about charset?
            println("leaf of string: $str becomes bytes: " + TreeHelper.convertToHex(byteArr))
            return byteArr
        }
        else -> {
            throw IllegalArgumentException("We don't use any other than Integers for these tests")
        }
    }
}

/**
 * A simple dummy hashing function that's easy to test
 * It only adds 1 to all bytes in the byte array.
 */
fun dummyAddOneHashFun(bArr: ByteArray, cryptoSystem: CryptoSystem?): Hash {
    val retArr = ByteArray(bArr.size)
    var pos = 0
    for (b: Byte in bArr.asIterable()) {
        retArr[pos] = b.inc()
        pos++
    }
    return retArr
}

/**
 * The "dummy" version is a real calculator, but it uses simplified versions of
 * serializations and hashing
 */
class MerkleHashCalculatorDummy: MerkleHashCalculator<GTXValue, GTXPath>(null) {
    val treeFactory = GtxBinaryTreeFactory()

    var proofTreeFactory: GtxMerkleProofTreeFactory
    var baseCalc: BinaryNodeHashCalculator
    init {
        proofTreeFactory = GtxMerkleProofTreeFactory(this)
        baseCalc = BinaryNodeHashCalculatorBase(cryptoSystem)
    }

    override fun calculateLeafHash(value: GTXValue): Hash {
        val hash = calculateHashOfValueInternal(value, ::dummySerializatorFun, ::dummyAddOneHashFun)
        //println("Hex: " + TreeHelper.convertToHex(hash))
        return hash
    }

    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        val prefixBA = byteArrayOf(prefix)
        return prefixBA + calculateNodeHashNoPrefixInternal(hashLeft, hashRight, ::dummyAddOneHashFun)
    }

    override fun isContainerProofValueLeaf(value: GTXValue): Boolean {
        return value.isContainerType()
    }

    override fun buildTreeFromContainerValue(value: GTXValue): GtxMerkleProofTree {
        val root: GtxBinaryTree = treeFactory.buildFromGtx(value)
        return proofTreeFactory.buildFromBinaryTree(root)
    }

}