package ru.lea.data_layer.base

abstract class Holder<T> {
    var socket: T? = null

    fun init(soc: T? = null) {
        close()
        socket = soc ?: createSocket()
    }

    abstract fun createSocket(): T?
    abstract fun close()
    open fun onError(e: String) { }
}
