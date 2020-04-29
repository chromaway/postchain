package net.postchain.ebft.syncmanager.common

import net.postchain.ebft.NodeStatus
import org.junit.Test
import kotlin.test.assertEquals

class EBFTNodesConditionTest {

    @Test
    fun emptyNodesList_with_TrueCondition() {
        val sut = EBFTNodesCondition(arrayOf()) { true }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun emptyNodesList_with_FalseCondition() {
        val sut = EBFTNodesCondition(arrayOf()) { false }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun singleNode_with_Quorum() {
        val sut = EBFTNodesCondition(
                arrayOf(NodeStatus(10, 0))
        ) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun singleNode_with_NoQuorum() {
        val sut = EBFTNodesCondition(
                arrayOf(NodeStatus(10, 0))
        ) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun twoNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun twoNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(30, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun threeNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(13, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun threeNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(30, 0),
                NodeStatus(12, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun fourNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(13, 0),
                NodeStatus(1, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun fourNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(30, 0),
                NodeStatus(30, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun fiveNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(13, 0),
                NodeStatus(6, 0),
                NodeStatus(1, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun fiveNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(30, 0),
                NodeStatus(32, 0),
                NodeStatus(31, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun sixNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(13, 0),
                NodeStatus(6, 0),
                NodeStatus(7, 0),
                NodeStatus(1, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun sixNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(30, 0),
                NodeStatus(32, 0),
                NodeStatus(31, 0),
                NodeStatus(33, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun sevenNodes_with_Quorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(12, 0),
                NodeStatus(13, 0),
                NodeStatus(6, 0),
                NodeStatus(7, 0),
                NodeStatus(1, 0),
                NodeStatus(1, 0)
        )) { status -> status.height > 5 }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun sevenNodes_with_NoQuorum() {
        val sut = EBFTNodesCondition(arrayOf(
                NodeStatus(10, 0),
                NodeStatus(11, 0),
                NodeStatus(12, 0),
                NodeStatus(30, 0),
                NodeStatus(32, 0),
                NodeStatus(31, 0),
                NodeStatus(33, 0)
        )) { status -> status.height > 20 }

        assertEquals(false, sut.satisfied())
    }

    @Test
    fun manyNodes_with_Quorum() {
        val nodesCount = 1000
        val quorum = nodesCount - (nodesCount - 1) / 3 // 667
        val rest = nodesCount - quorum // 333
        val statuses = mutableListOf<NodeStatus>()
        repeat(quorum) { statuses.add(NodeStatus(10, 0)) }
        repeat(rest) { statuses.add(NodeStatus(1, 0)) }

        val sut = EBFTNodesCondition(statuses.apply { shuffle() }.toTypedArray()) {
            status -> status.height > 5
        }

        assertEquals(true, sut.satisfied())
    }

    @Test
    fun manyNodes_with_NoQuorum() {
        val nodesCount = 1000
        val quorum = nodesCount - (nodesCount - 1) / 3 // 667
        val rest = nodesCount - quorum // 333
        val statuses = mutableListOf<NodeStatus>()
        repeat(quorum - 1) { statuses.add(NodeStatus(10, 0)) }
        repeat(rest + 1) { statuses.add(NodeStatus(1, 0)) }

        val sut = EBFTNodesCondition(statuses.apply { shuffle() }.toTypedArray()) {
            status -> status.height > 5
        }

        assertEquals(false, sut.satisfied())
    }
}