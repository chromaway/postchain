// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client.core

import net.postchain.base.BlockchainRid
import net.postchain.client.core.ConcretePostchainClient
import net.postchain.client.core.DefaultSigner
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainNodeResolver

object PostchainClientFactory {

    fun makeSimpleNodeResolver(serverURL: String): PostchainNodeResolver {
        return object : PostchainNodeResolver {
            override fun getNodeURL(blockchainRID: BlockchainRid): String {
                return serverURL
            }
        }
    }

    fun getClient(resolver: PostchainNodeResolver, blockchainRID: BlockchainRid, defaultSigner: DefaultSigner?): PostchainClient {
        return ConcretePostchainClient(resolver, blockchainRID, defaultSigner)
    }
}