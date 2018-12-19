package net.postchain.base.merkle

object MerkleRootCalculator {

    //val hashTreeFactory = HashFullBinaryTreeFactory()


    /**
     * @param cryptoSystem - needed to find the hash/digest function
     * @param hashes - all the hashes we will use as leaves in the tree
     *  @return the merkle root
    fun computeMerkleRootHash(cryptoSystem: CryptoSystem,
                              hashes: Array<ByteArray>): Hash {
        val treeWithHashesAsLeaves = hashTreeFactory.buildFromArrayList(hashes.toList())
        return computeSubTreeHash(treeWithHashesAsLeaves.root, MerkleHashCalculatorBase(cryptoSystem))
    }
     */

    /**
     * Inner recursion
     */
    fun computeSubTreeHash(element: BinaryTreeElement, calculator: MerkleHashCalculator): Hash {
        return when(element) {
            is Leaf<*> -> {
                val x = element.content
                x as Hash // No need to calculate the leaf hash. That's what we started out with
            }
            is Node -> {
                val left = computeSubTreeHash(element.left, calculator)
                val right = computeSubTreeHash(element.right, calculator)

                calculator.calculateNodeHash(element.getPrefixByte(), left, right)
            }
            else -> {
                throw IllegalStateException("TODO fix") // TODO fix
            }
        }
    }
}