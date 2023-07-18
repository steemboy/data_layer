package ru.lea.data_layer

import ru.lea.data_layer.holders.ClientHolder
import ru.lea.data_layer.threads.MessageThread

class ClientService<T>(private var holder: ClientHolder<T>) {
    private var isWork = false
    private var messageThread : MessageThread<T>? = null

    fun start() {
        stop()
        starter()
    }

    private fun work() {
        Thread {
            var delay = 1L
            while (isWork)
                try {
                    holder.init()
                    holder.connect()
                    messageThread = MessageThread(holder, ::starter)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    holder.onError(e.message ?: "Неопознанная ошибка")
                    Thread.sleep(500 * delay)
                    if (delay < 10)
                        delay++
                }
        }.start()
    }

    fun stop() {
        isWork = false
        holder.close()
        messageThread?.interrupt()
    }

    private fun starter(me: MessageThread<T>? = null) {
        isWork = true
        work()
    }
}
