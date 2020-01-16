package net.postchain.devtools.utils.configuration

/**
 * NOTE: Don't use this!
 *
 * We put all the nodes keys in here, and re-use them in all tests
 */

@Deprecated("Use KeyPairHelper")
object TestNodeKeyCache{

    const val NODE0_PrivK = "0000000000000000000000000000000001000000000000000000000000000000"
    const val NODE0_PubK = "03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
    const val NODE1_PrivK = "0101010101010101010101010101010101010101010101010101010101010101"
    const val NODE1_PubK = "031B84C5567B126440995D3ED5AABA0565D71E1834604819FF9C17F5E9D5DD078F"
    const val NODE2_PrivK = "0202020202020202020202020202020201020202020202020202020202020202"
    const val NODE2_PubK = "03B2EF623E7EC933C478135D1763853CBB91FC31BA909AEC1411CA253FDCC1AC94"
    const val NODE3_PrivK = "0303030303030303030303030303030301030303030303030303030303030303"
    const val NODE3_PubK = "0203C6150397F7E4197FF784A8D74357EF20DAF1D09D823FFF8D3FC9150CBAE85D"
    const val NODE4_PrivK = "0404040404040404040404040404040401040404040404040404040404040404"
    const val NODE4_PubK = "031A7C8706F34316DF7154595FA798F672968C3E44EF65E4695E89F0965E4141D1"


    val cachePub = mutableMapOf<NodeSeqNumber, String>()
    val cachePriv = mutableMapOf<NodeSeqNumber, String>()

    fun init() {
        cachePub[NodeSeqNumber(0)] = NODE0_PubK
        cachePub[NodeSeqNumber(1)] = NODE1_PubK
        cachePub[NodeSeqNumber(2)] = NODE2_PubK
        cachePub[NodeSeqNumber(3)] = NODE3_PubK
        cachePub[NodeSeqNumber(4)] = NODE4_PubK

        cachePriv[NodeSeqNumber(0)] = NODE0_PrivK
        cachePriv[NodeSeqNumber(1)] = NODE1_PrivK
        cachePriv[NodeSeqNumber(2)] = NODE2_PrivK
        cachePriv[NodeSeqNumber(3)] = NODE3_PrivK
        cachePriv[NodeSeqNumber(4)] = NODE4_PrivK
    }

    fun getPubKey(nodeNr: NodeSeqNumber): String {
        if (cachePub.isEmpty()) {
            init()
        }
        return cachePub[nodeNr]!!
    }

    fun getPrivKey(nodeNr: NodeSeqNumber): String {
        if (cachePriv.isEmpty()) {
            init()
        }
        return cachePriv[nodeNr]!!
    }
}