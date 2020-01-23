// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.path


open class Path<T: PathElement>(val pathElements: List<T>) {

    /**
     * @return the path element we are at now.
     */
    fun getCurrentPathElement(): T {
        return  pathElements.first()
    }

    // For debug
    fun size(): Int {
        return this.pathElements.size
    }
}