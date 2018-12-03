package net.postchain.cli

interface Command {

    /**
     * TODO: [et]
     */
    fun key(): String

    /**
     * TODO: [et]
     */
    fun execute()
}