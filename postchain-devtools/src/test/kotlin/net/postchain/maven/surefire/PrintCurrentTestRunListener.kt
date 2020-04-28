// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.maven.surefire

import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.RunListener

class PrintCurrentTestRunListener : RunListener() {

    @Throws(Exception::class)
    override fun testRunStarted(description: Description) {
        println("Running tests: " +
                (description.className ?: "") +
                " " + (description.displayName ?: "") +
                " " + (description.toString() ?: ""))
    }

    @Throws(Exception::class)
    override fun testStarted(description: Description) {
        println("Test started: $description")
    }

    @Throws(Exception::class)
    override fun testFinished(description: Description) {
        println("Test finished: $description")
    }

    @Throws(Exception::class)
    override fun testRunFinished(result: Result) {
        println("Tests run: ${result.runCount}" +
                ", failures: ${result.failureCount}" +
                ", ignored: ${result.ignoreCount}")
    }
}