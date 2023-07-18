package ru.lea.data_layer.holders

import android.util.ArrayMap
import ru.lea.data_layer.base.DlMethod
import java.lang.reflect.Method

class ServiceHolder(val cls: Any) {
    val methods = ArrayMap<String, Method>().apply {
        for (m in cls.javaClass.methods)
            if (m.getAnnotation(DlMethod::class.java) != null)
                put(m.name, m)
    }
}
