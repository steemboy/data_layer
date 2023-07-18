package ru.lea.data_layer.base

import android.os.Handler
import android.os.Looper
import java.lang.reflect.Type

class Promise<E> {
    enum class State {
        None, Resolved, Rejected
    }
    var type : Type? = null
    private val hndler = Handler(Looper.getMainLooper())
    private var state = State.None

    private var resolvedObject: E? = null
    private var rejectedObject = ""
    private var onErrorListener: ((s: String) -> Unit)? = null
    private var onSuccessListener: ((s: E) -> Unit)? = null

    fun then(listener: (s: E) -> Unit): Promise<E> {
        onSuccessListener = listener
        if (state == State.Resolved)
            hndler.post { onSuccessListener!!(resolvedObject!!) }
        return this
    }

    fun onMessage(data: E) : Promise<E> {
        if (state != State.Resolved) {
            state = State.Resolved
            resolvedObject = data
            if (onSuccessListener != null)
                hndler.post { onSuccessListener!!(resolvedObject!!) }
        }
        return this
    }

    fun onError(error: String): String {
        if (state != State.Rejected) {
            state = State.Rejected
            rejectedObject = error
            if (onErrorListener != null)
                hndler.post { onErrorListener!!(error) }
        }
        return error
    }

    fun error(listener: (s: String) -> Unit) {
        onErrorListener = listener
        if (state == State.Rejected)
            hndler.post { onErrorListener!!(rejectedObject) }
    }
}
