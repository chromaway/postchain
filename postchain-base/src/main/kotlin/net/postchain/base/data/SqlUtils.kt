package net.postchain.base.data

import java.sql.Timestamp
import java.time.Instant

object SqlUtils {

    fun toTimestamp(time: Instant? = null): Timestamp {
        return if (time == null) {
            Timestamp(Instant.now().toEpochMilli())
        } else {
            Timestamp(time.toEpochMilli())
        }
    }

}