package net.postchain.util

import kotlin.random.Random

object NodesTestHelper {

    fun selectAnotherRandNode(nodeId: Int, nodesCount: Int): Int {
        val randNode = Random.nextInt(nodesCount)
        // Cannot be connected to itself, so pic new value
        return if (randNode == nodeId) (randNode + 1) % nodesCount else randNode
    }

}