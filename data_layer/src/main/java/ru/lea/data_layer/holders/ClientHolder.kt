package ru.lea.data_layer.holders

import android.util.ArrayMap
import com.google.gson.Gson
import ru.lea.data_layer.base.Holder
import ru.lea.data_layer.base.Message
import ru.lea.data_layer.base.Promise
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy

abstract class ClientHolder<T>(val name: String): Holder<T>() {
    var connected = false
    var write: ((Message, Promise<Any>) -> Unit)? = null
    private val gson = Gson()
    var services = ArrayMap<String, ServiceHolder>()
    protected fun addService(s: Any) {
        services[s.javaClass.simpleName] = ServiceHolder(s)
    }

    abstract fun connect()
    abstract fun inputStream(): InputStream
    abstract fun outputStream(): OutputStream

    open fun onConnect() {
        connected = true
    }
    open fun onDisconnect() {
        connected = false
    }

    protected fun <T> addApi(c: Class<T>) = Proxy.newProxyInstance(c.classLoader, arrayOf(c)) { _, m, a ->
        Promise<Any>().apply {
            if(m.genericReturnType is ParameterizedType)
                type = (m.genericReturnType as ParameterizedType).actualTypeArguments[0]
            if(connected) {
                val mes = Message(c.simpleName, m.name)
                if (!a.isNullOrEmpty() && a[0] != null)
                    mes.message = gson.toJson(a[0])
                write?.invoke(mes, this)
            } else
                onError("Нет соединения с сервером")
        }
    } as T
}
