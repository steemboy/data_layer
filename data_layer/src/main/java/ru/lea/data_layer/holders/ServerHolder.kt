package ru.lea.data_layer.holders

import android.util.ArrayMap
import ru.lea.data_layer.base.Holder

abstract class ServerHolder<T, E>: Holder<T>() {
    val clients = ArrayMap<String, ClientHolder<E>>()
    fun addClientHolder(ch: ClientHolder<E>) {
        clients[ch.name] = ch
    }
    fun removeClientHolder(name: String) {
        clients[name]?.close()
        clients.remove(name)
    }
    abstract fun auth(client: E): ClientHolder<E>?

    abstract fun closeClient(client: E)
    abstract fun accept() : E

    open fun onServerStart() { }
    open fun onServerStop() { }
}
