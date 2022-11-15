package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvlib.widget.List

open class AlfabetShowList(context: Context?) : List(context, emptyMap()) {
    private val abc = ("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

    protected open fun setType(map: MutableMap<String?, Any?>) {
        map["type"] = "letter_series"
    }

    public override fun setConstData() {
        val size = abc.length
        var m: MutableMap<String?, Any?>
        for (i in 0 until size) {
            m = HashMap()
            setType(m)
            m["name"] = "" + abc[i]
            data.add(m)
        }
    }
}