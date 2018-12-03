// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

class ConfirmationProofMaterial(val txHash: ByteArray,
                                val txHashes: Array<ByteArray>,
                                val header: ByteArray,
                                val witness: ByteArray)