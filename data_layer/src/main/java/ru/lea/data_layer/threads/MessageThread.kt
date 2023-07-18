package ru.lea.data_layer.threads

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import ru.lea.data_layer.base.Message
import ru.lea.data_layer.base.Promise
import ru.lea.data_layer.holders.ClientHolder
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class MessageThread<E>(private val client : ClientHolder<E>, private val disconnect: (MessageThread<E>) -> Unit) {
    private var queue = LinkedBlockingQueue<Message>()
    private var isInterrupted = false
    private val executor = Executors.newFixedThreadPool(3)
    private val hndle = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val requests = HashMap<String, Promise<Any>>()

    init {
        executor.execute(run())
        executor.execute(send())
    }

    private fun run() = Runnable {
        hndle.post { client.onConnect() }
        client.write = ::write

        val istr = client.inputStream()
        while (!isInterrupted)
            try {
                val buf = ByteArray(4)
                istr.read(buf)
                val s = ByteBuffer.wrap(buf).int
                val buffer = ByteArray(s)
                var bytes = 0
                do {
                    val data = ByteArray(min(1024, s - bytes))
                    val b = istr.read(data)
                    System.arraycopy(data, 0, buffer, bytes, b)
                    bytes += b
                } while (bytes < buffer.size)
                executor.execute(work(buffer))
            } catch (ex: Exception) {
                break
            }
        if(!isInterrupted)
            interrupt()
        client.write = null
        hndle.post {
            client.onDisconnect()
            disconnect.invoke(this@MessageThread)
        }
    }

    fun interrupt() {
        isInterrupted = true
        executor.shutdownNow()
        queue.put(Message("close", "close"))
        client.close()
    }

    private fun send(m: Message, str: String, error: Boolean = false) {
        m.action = if(error) "error" else "response"
        m.message = str
        write(m)
    }

    private fun write(m: Message, listener: Promise<Any>? = null) {
        if (listener != null) {
            m.uuid = UUID.randomUUID().toString()
            requests[m.uuid] = listener
        }
        queue.put(m)
    }

    private fun send() = Runnable {
        while (!isInterrupted)
            queue.take()?.apply {
                try {
                    if (service == "close" && action == "close")
                        return@Runnable
                    val bytes = gson.toJson(this).toByteArray(Charset.defaultCharset())
                    val len = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array()
                    client.outputStream().apply {
                        write(len)
                        write(bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hndle.post { requests[uuid]?.onError("Ошибка при отправке сообщения.\n" + e.message) }
                }
            }
    }

    private fun work(buffer: ByteArray) = Runnable {
        try {
            val m = gson.fromJson(String(buffer), Message::class.java)
            when {
                m.action.isEmpty() -> send(m, "Не передан тип операции", true)
                m.action == "error" -> {}
                m.action == "response" -> requests[m.uuid]?.apply {
                    requests.remove(m.uuid)
                    if (m.success) {
                        if (m.message != "")
                            onMessage(gson.fromJson(m.message, type))
                    } else
                        onError(m.message)
                }
                else -> client.services[m.service]?.apply {
                    methods[m.action]?.apply {
                        hndle.post {
                            try {
                                val o: Any? = if (parameterTypes.isNotEmpty())
                                    invoke(cls, gson.fromJson(m.message, parameterTypes[0]))
                                else
                                    invoke(cls)
                                m.success = true
                                send(m, if (o != null) gson.toJson(o) else "")
                            } catch (ex: Exception) {
                                send(m, ex.message ?: "Не опознанная ошибка")
                            }
                        }
                    } ?: send(m, "В сервисе ${m.service} метод ${m.action} не найден", true)
                } ?: send(m, "Сервис ${m.service} не найден", true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
