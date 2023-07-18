package ru.lea.data_layer

import ru.lea.data_layer.holders.ClientHolder
import ru.lea.data_layer.holders.ServerHolder
import ru.lea.data_layer.threads.MessageThread

class ServerService<T, E>(private val holder: ServerHolder<T, E>) {
    private var isWork = false
    private var threads = ArrayList<MessageThread<E>>()

    fun start() {
        stop()
        isWork = true
        work()
    }

    private fun work() {
        Thread {
            holder.onServerStart()
            try {
                holder.init()
                while (isWork) {
                    val c = holder.accept()
                    val ch = holder.auth(c)
                    if (ch != null)
                        onConnect(ch)
                    else {
                        holder.closeClient(c)
                        holder.onError("Клиент не прошёл авторизацию")
                    }
                }
            } catch (ex: Exception) {
                holder.onError(ex.message ?: "Не опознанная ошибка")
            }
            if (isWork)
                stop()
            holder.onServerStop()
        }.start()
    }

    fun stop() {
        isWork = false
        for(t in threads)
            t.interrupt()
        threads.clear()
        holder.close()
    }

    fun isWork() = isWork

    private fun onConnect(client: ClientHolder<E>) {
        threads.add(MessageThread(client, ::onDisconnect))
    }

    private fun onDisconnect(mt: MessageThread<E>) {
        threads.remove(mt)
    }
}
