package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

class GenreShowList(context: Context?, map: Map<String?, Any?>) : AllShowList(context, map ?: emptyMap()) {
    var section: String?
    override fun setConstData() {}

    init {
        section = map["type"] as String?
    }

    override val apiAddress: String
        protected get() = ApiConst.BASE + section + "?id=" + map["id"]
}