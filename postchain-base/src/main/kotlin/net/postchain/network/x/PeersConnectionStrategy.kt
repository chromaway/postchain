// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.core.Shutdownable

/**
 * This interface represents a strategy for making sure there is one connection per chain and peer. Its methods
 * will get called from a DefaultXConnectionManager when certain events happen.
 */
interface PeersConnectionStrategy: Shutdownable {
    fun connectAll(chainID: Long, peerIds: Set<XPeerID>)

    /**
     * Called when a connection has been closed for any reason. This method is responsible for
     * executing a strategy for how to handle this lost connection. It could be for example to
     * always immediately try to reconnect, or wait X ms before trying to reconnect, or do nothing.
     * When this method is called, the XConnectionManager has already removed the connection from
     * it's management.
     *
     * @param chainID The chain ID for which the connection was closed
     * @param peerId the remote peer of the closed connection
     * @param isOutgoing true if this is a client connection, ie we initiated the connection, false otherwise
     */
    fun connectionLost(chainID: Long, peerId: XPeerID, isOutgoing: Boolean)

    /**
     * Decides whether to swap the already used connection for a new connection.
     *
     * @param chainID the chain ID for which the duplicate was found
     * @param isOriginalOutgoing true if the already used connection is a client connection, ie outgoing. False otherwise
     * @param peerId the peer for which the duplicate was found
     * @return true if the original (already used) connection should be swapped for the new connection
     */
    fun duplicateConnectionDetected(chainID: Long, isOriginalOutgoing: Boolean, peerId: XPeerID): Boolean

    fun connectionEstablished(chainID: Long, isOutgoing: Boolean, peerId: XPeerID)
}