package net.postchain.client

class PostchainClientFactory {
    fun makeSimpleNodeResolver(serverURL: String): PostchainNodeResolver {
        return object: PostchainNodeResolver {
            override fun getNodeURL(blockchainRID: ByteArray): String {
                return serverURL
            }
        }
    }
    fun getClient(resolver: PostchainNodeResolver, blockchainRID: ByteArray, defaultSigner: DefaultSigner?): PostchainClient {
        return ConcretePostchainClient(resolver, blockchainRID, defaultSigner)
    }
}