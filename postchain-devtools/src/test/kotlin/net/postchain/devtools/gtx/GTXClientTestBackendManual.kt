// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.gtx

import net.postchain.devtools.IntegrationTest
import org.junit.Test

class GTXClientTestBackendManual : IntegrationTest() {

    @Test
    fun testBuildBlock() {
        val nodeCount = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        createNodes(nodeCount, "/net/postchain/devtools/manual/blockchain_config.xml")
        Thread.sleep(6000/*00*/)
    }
}