package net.postchain.api.rest.controller

interface Modellable {

    /**
     * Attaches a [model] associated with key [blockchainRID]
     */
    fun attachModel(blockchainRID: String, model: Model)

    /**
     * Detaches a model associated with key [blockchainRID]
     */
    fun detachModel(blockchainRID: String)

    /**
     * Retrieves a model associated with key [blockchainRID]
     */
    fun retrieveModel(blockchainRID: String): Model?

}