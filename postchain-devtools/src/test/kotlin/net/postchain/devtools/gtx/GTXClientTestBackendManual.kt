// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.gtx

import net.postchain.devtools.IntegrationTestSetup
import org.junit.Test

class GTXClientTestBackendManual : IntegrationTestSetup() {

    @Test
    fun testBuildBlock() {
        val nodeCount = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        createNodes(nodeCount, "/net/postchain/devtools/manual/blockchain_config.xml")
        Thread.sleep(6000/*00*/)
    }
}