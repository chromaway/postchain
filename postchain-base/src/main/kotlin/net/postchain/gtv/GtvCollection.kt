// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv


/**
 * A supertype for all types that can hold other [Gtv]
 *
 * Typically a collection has a size
 */
abstract class GtvCollection: AbstractGtv() {

    abstract fun getSize(): Int

}