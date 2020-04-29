package net.postchain.e2e.tools

import net.postchain.gtv.GtvString

class CityTxSender(
        apiUrl: String,
        blockchainRid: String,
        privKey: String,
        pubKey: String
) : TxSender(
        apiUrl,
        blockchainRid,
        privKey,
        pubKey
) {

    fun postAddCityTx(city: String) {
        postTx { txBuilder ->
            txBuilder.addOperation("add_city", arrayOf(GtvString(city)))
        }
    }
}