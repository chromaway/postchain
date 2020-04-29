// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

interface Command {

    /**
     * TODO: [et]
     */
    fun key(): String

    /**
     * TODO: [et]
     */
    fun execute(): CliResult
}