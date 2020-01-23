// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.gtv.merkle.proof.GtvMerkleHashSummaryFactory
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.gtv.merkle.virtual.GtvVirtualFactory

object GtvMerkleBasics {
    const val HASH_PREFIX_NODE_GTV_ARRAY: Byte = 7
    const val HASH_PREFIX_NODE_GTV_DICT: Byte = 8

    const val UNKNOWN_COLLECTION_POSITION =-10L
    // --------------------------------------------------------------
    // Fetch factory instances from here (no need to create new instances over and over)
    //
    // Note: Probably ok to use the (forbidden) singleton pattern here to get to instances, since we (at least for now)
    // have no plans for replacing these factories in tests.
    // --------------------------------------------------------------
    private val treeFactory = GtvBinaryTreeFactory()
    private val proofFactory = GtvMerkleProofTreeFactory()
    private val summaryFactory = GtvMerkleHashSummaryFactory(treeFactory, proofFactory)
    private val virtualFactory = GtvVirtualFactory

    fun getGtvBinaryTreeFactory() = treeFactory
    fun getGtvMerkleProofTreeFactory() = proofFactory
    fun getGtvMerkleHashSummaryFactory() = summaryFactory
    fun getGtvVirtualFactory() = virtualFactory

}