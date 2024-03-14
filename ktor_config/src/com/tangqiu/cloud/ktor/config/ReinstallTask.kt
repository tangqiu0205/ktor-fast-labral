package com.tangqiu.cloud.ktor.config

class ReinstallTask(private val checker: () -> Boolean, private var reinstall: () -> Unit) : Thread() {

    companion object {
        @Volatile
        private var instance: ReinstallTask? = null

        fun obtain(checker: () -> Boolean, reinstall: () -> Unit): ReinstallTask =
            instance?.apply {
                this.reinstall = reinstall
            } ?: ReinstallTask(checker, reinstall).also {
                it.start()
                instance = it
            }
    }

    override fun run() {
        super.run()
        while (!interrupted()) {
            sleep(5000L)
            if (checker.invoke()) {
                interrupt()
            }

        }

        reinstall.invoke()

        instance = null
    }
}