package net.postchain.base.merkle


/**
 * This construct can be used to validate blocks
 */
class HashBinaryTree(root: BinaryTreeElement) : BinaryTree<Hash>(root) {
}

/*
class HashFullBinaryTreeFactory : CompleteBinaryTreeFactory<Hash>() {

    fun buildFromArrayList(originalList: List<Hash>): HashBinaryTree {
        val result = buildSubTree(originalList)
        return HashBinaryTree(result)
    }

    /**
     * Needed to override this, to handle [Hash] leafs
     */
    override fun handleLeaf(leaf: Hash): BinaryTreeElement {
        return Leaf(leaf)
    }
}
        */