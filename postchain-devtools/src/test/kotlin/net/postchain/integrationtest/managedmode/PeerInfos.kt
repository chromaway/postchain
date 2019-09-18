package net.postchain.integrationtest.managedmode

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray

class PeerInfos {

    companion object {

        val peerInfo0 = PeerInfo(
                "127.0.0.1",
                9870,
                "03a301697bdfcd704313ba48e51d567543f2a182031efd6915ddc07bbcc4e16070".hexStringToByteArray()
        )

        val peerInfo1 = PeerInfo(
                "127.0.0.1",
                9871,
                "031B84C5567B126440995D3ED5AABA0565D71E1834604819FF9C17F5E9D5DD078F".hexStringToByteArray()
        )

        val peerInfo2 = PeerInfo(
                "127.0.0.1",
                9872,
                "03B2EF623E7EC933C478135D1763853CBB91FC31BA909AEC1411CA253FDCC1AC94".hexStringToByteArray()
        )

        val peerInfo3 = PeerInfo(
                "127.0.0.1",
                9873,
                "0203C6150397F7E4197FF784A8D74357EF20DAF1D09D823FFF8D3FC9150CBAE85D".hexStringToByteArray()
        )
    }

}