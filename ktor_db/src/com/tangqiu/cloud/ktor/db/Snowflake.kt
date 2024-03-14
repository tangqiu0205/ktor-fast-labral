package com.tangqiu.cloud.ktor.db

/**
 * id生成器：雪花算法
 */
class SnowFlake(private val dataCenter: Long = 1, private val machine: Long = 15) {

    companion object {
        private const val START_TIMESTAMP = 1591771120000L
    }

    private var sequence: Long = 0L
    private var lastTimestamp = -1L

    init {
        if (dataCenter > 31 || dataCenter < 0)
            throw IllegalArgumentException("data center id must be range in(1-31)")
        if (machine > 31 || machine < 0)
            throw IllegalArgumentException("machine id must be range in(1-31)")
    }

    fun nextId(): Long {
        var current = System.currentTimeMillis()
        if (current < lastTimestamp)
            throw RuntimeException("Clock moved backwards.  Refusing to generate id")
        if (current == lastTimestamp) {
            sequence = sequence.inc() and 4095
            if (sequence == 0L)
                current = nextMill()
        } else {
            sequence = 0L
        }
        lastTimestamp = current
        return (current.minus(START_TIMESTAMP) shl 22) or (dataCenter shl 17) or (machine shl 12) or sequence
    }

    private fun nextMill(): Long {
        var mill = System.currentTimeMillis()
        while (mill <= lastTimestamp) {
            mill = System.currentTimeMillis()
        }
        return mill
    }
}