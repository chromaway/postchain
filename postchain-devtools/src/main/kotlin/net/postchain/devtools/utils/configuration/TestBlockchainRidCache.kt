package net.postchain.devtools.utils.configuration

    /**
     * We put all the chains' RIDs in here, and re-use them in all tests
     *
     * Note1: This may seem a bit crazy, but we are allowed to do this since this is just for testing.
     * Note2: If you need more chains for your test, just add more
     */
    object TestBlockchainRidCache {

        const val BC1 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
        const val BC2 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a4"
        const val BC3 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a5"
        const val BC4 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a6"
        const val BC5 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a7"
        const val BC6 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a8"
        const val BC7 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a9"
        const val BC8 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577b0"
        const val BC9 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577b1"
        const val BC10= "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577b2"


        val cacheRid = mutableMapOf<Int, String>()
        val cacheChainId = mutableMapOf<String, Int>()

        fun init() {
            cacheRid[1] = BC1
            cacheRid[2] = BC2
            cacheRid[3] = BC3
            cacheRid[4] = BC4
            cacheRid[5] = BC5
            cacheRid[6] = BC6
            cacheRid[7] = BC7
            cacheRid[8] = BC8
            cacheRid[9] = BC9
            cacheRid[10]= BC10

            cacheChainId[BC1] = 1
            cacheChainId[BC2] = 2
            cacheChainId[BC3] = 3
            cacheChainId[BC4] = 4
            cacheChainId[BC5] = 5
            cacheChainId[BC6] = 6
            cacheChainId[BC7] = 7
            cacheChainId[BC8] = 8
            cacheChainId[BC9] = 9
            cacheChainId[BC10]= 10

        }

        fun getChainId(rid: String): Int {
            if (cacheChainId.isEmpty()) {
                init()
            }
            return cacheChainId[rid]!!
        }


        fun getRid(chainId: Int): String {
            if (cacheRid.isEmpty()) {
                init()
            }
            return cacheRid[chainId]!!
        }


    }
