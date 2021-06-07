// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import assertk.Assert
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNullOrEmpty
import assertk.assertions.support.expected
import assertk.assertions.support.show

// TODO: [et]: See postchain-common/test module

/**
 * Asserts the collection has the expected size.
 */
fun <T : Map<*, *>> Assert<T>.hasSize(size: Int) {
    assert(actual.size, "size").isEqualTo(size)
}

/**
 * Asserts the map is empty.
 * @see [isNotEmpty]
 * @see [isNullOrEmpty]
 */
fun <T : Map<*, *>> Assert<T>.isEmpty() {
    if (actual.isEmpty()) return
    expected("to be empty but was:${show(actual)}")
}