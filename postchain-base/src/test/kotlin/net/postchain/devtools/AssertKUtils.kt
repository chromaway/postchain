// Copyright (c) 2020 ChromaWay AB. See README for license information.

package assertk

import assertk.assertions.support.fail

// TODO: [et]: See postchain-common/test module

/**
 * Asserts the ByteArray content is equal to the content of expected one.
 * @see [ByteArray.contentEquals] function
 */
fun Assert<ByteArray>.isContentEqualTo(expected: ByteArray) {
    if (actual.contentEquals(expected)) return
    this.fail(expected, actual)
}

/**
 * Asserts the Array content is equal to the content of expected one.
 * @see [Array.contentEquals] function
 */
fun <T> Assert<Array<out T>>.isContentEqualTo(expected: Array<out T>) {
    if (actual.contentEquals(expected)) return
    this.fail(expected, actual)
}
