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

/**
 * Asserts the map contains exactly the expected keys. There must not be any extra elements.
 * @see [containsAll]
 */
fun <K, V> Assert<Map<K, V>>.containsExactlyKeys(vararg keys: K) {
    if (actual.size == keys.size && keys.all { actual.containsKey(it) }) return
    expected("to contain exactly keys:${show(keys)} but was:${show(actual.keys)}")
}