package net.postchain.api.rest.json

import com.google.gson.*
import net.postchain.base.ConfirmationProof
import net.postchain.base.Side
import net.postchain.common.toHex
import net.postchain.core.MultiSigBlockWitness
import java.lang.reflect.Type

internal class ConfirmationProofSerializer : JsonSerializer<ConfirmationProof> {

    override fun serialize(src: ConfirmationProof?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val proof = JsonObject()
        if (src == null) {
            return proof
        }
        proof.add("hash", JsonPrimitive(src.txHash.toHex()))
        proof.add("blockHeader", JsonPrimitive(src.header.toHex()))

        val sigs = JsonArray()
        (src.witness as MultiSigBlockWitness).getSignatures().forEach {
            val sig = JsonObject()
            sig.addProperty("pubKey", it.subjectID.toHex())
            sig.addProperty("signature", it.data.toHex())
            sigs.add(sig)
        }
        proof.add("signatures", sigs)
        val path = JsonArray()
        src.merklePath.forEach {
            val pathElement = JsonObject()
            pathElement.addProperty("side", if (it.side == Side.LEFT) 0 else 1)
            pathElement.addProperty("hash", it.hash.toHex())
            path.add(pathElement)
        }
        proof.add("merklePath", path)
        return proof
    }
}