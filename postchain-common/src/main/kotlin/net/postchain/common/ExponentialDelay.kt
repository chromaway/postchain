package net.postchain.common

/**
 * This is a (sort of) generic class that is able to make delays exponentially longer and longer.
 */
class ExponentialDelay (
         val MAX_DELAY_MILLIS: Long = 60000, // Default 1 min max delay
         var delayCounterMillis: Long = 20L  // Default we begin with a delay of 20 milliseconds
    ) {
        private var executionCounter = 0L // Number of times we've done this

        companion object {
            const val DELAY_POWER_BASE: Double = 2.0
        }

        fun getDelayMillis(): Long {
            if (delayCounterMillis < MAX_DELAY_MILLIS) {
                // must calculate new delay
                //println("ec = $executionCounter")
                delayCounterMillis = (delayCounterMillis * (Math.pow(DELAY_POWER_BASE, executionCounter.toDouble()))).toLong()
                executionCounter += 1
                if (delayCounterMillis > MAX_DELAY_MILLIS) {
                    delayCounterMillis = MAX_DELAY_MILLIS
                }
            }

            return delayCounterMillis
        }

}