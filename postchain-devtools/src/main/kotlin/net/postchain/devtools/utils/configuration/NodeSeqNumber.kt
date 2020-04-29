package net.postchain.devtools.utils.configuration

/**
 * Just a wrapper around the sequence number of the node (0, 1, 2, 3..).
 * (so that we don't accidentally confuse it with the blockchain number)
 */
data class NodeSeqNumber(val nodeNumber: Int) {

}