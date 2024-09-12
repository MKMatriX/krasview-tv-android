package ru.krasview.kvlib.widget.lists

import android.annotation.SuppressLint
import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

@SuppressLint("ViewConstructor")
class OneSeasonSeriesList(context: Context?, map: Map<String?, Any?>?) :
    AllSeriesList(context, map ?: emptyMap()) {
    override val apiAddress: String
        get() = ApiConst.SEASON + "?id=" + map["id"]
}