package net.postchain.api.rest.json

import com.google.gson.*
import net.postchain.base.ConfirmationProof
import net.postchain.common.toHex
import net.postchain.core.MultiSigBlockWitness
import net.postchain.gtv.Gtv
import net.postchain.gtv.make_gtv_gson
import java.lang.reflect.Type

internal class ConfirmationProofSerializer : JsonSerializer<ConfirmationProof> {

    val gson = make_gtv_gson()

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
        val merkleProofTree = gson.toJsonTree(src.proof.serializeToGtv(), Gtv::class.java) // TODO: Feels like I'm mixing two styles here?
        proof.add("merkleProofTree", merkleProofTree)
        return proof
    }
}