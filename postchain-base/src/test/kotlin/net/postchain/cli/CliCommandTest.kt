// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.toHex
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Test
import kotlin.test.assertEquals

class CliCommandTest {

    // mostly test external lib
    @Test
    fun testMnemonic() {
        val cs = SECP256K1CryptoSystem()
        val privKey = cs.getRandomBytes(32)

        val wordList = MnemonicCode.INSTANCE.toMnemonic(privKey)

        val reverse = MnemonicCode.INSTANCE.toEntropy(wordList)

        assertEquals(privKey.toHex(), reverse.toHex())
    }
}