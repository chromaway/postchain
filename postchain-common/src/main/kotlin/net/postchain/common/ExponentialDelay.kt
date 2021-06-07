// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.common

/**
 * This is a (sort of) generic class that is able to make delays exponentially longer and longer.
 */
class ExponentialDelay (
         val MAX_DELAY_MILLIS: Long = 60000, // Default 1 min max delay
         var delayCounterMillis: Long = 100L  // Default we begin with a delay of 100 milliseconds
    ) {
        private var executionCounter = 0L // Number of times we've done this

        companion object {
            const val DELAY_POWER_BASE: Double = 2.0
        }

        fun getDelayMillisAndIncrease(): Long {
            val result = delayCounterMillis;
            if (delayCounterMillis < MAX_DELAY_MILLIS) {
                executionCounter += 1
                // must calculate new delay
                //println("ec = $executionCounter")
                delayCounterMillis = (delayCounterMillis * (Math.pow(DELAY_POWER_BASE, executionCounter.toDouble()))).toLong()
                if (delayCounterMillis > MAX_DELAY_MILLIS) {
                    delayCounterMillis = MAX_DELAY_MILLIS
                }
            }

            return result
        }

}