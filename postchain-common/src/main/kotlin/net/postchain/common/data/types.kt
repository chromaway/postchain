package net.postchain.common.data

typealias Hash = ByteArray

typealias TreeHasher = (Hash, Hash) -> Hash

const val KECCAK256 = "KECCAK-256"
const val SHA256 = "SHA-256"

const val HASH_LENGTH = 32
val EMPTY_HASH = ByteArray(HASH_LENGTH) { 0 }