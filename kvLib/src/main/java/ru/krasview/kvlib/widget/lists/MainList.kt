package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvlib.widget.List

class MainList(context: Context?) : List(context, emptyMap()) {
    public override fun setConstData() {
        var m: MutableMap<String?, Any?>
        if (account.isKrasviewAccount) {
            m = HashMap()
            m["type"] = "my_shows_all"
            m["name"] = "Я смотрю"
            data.add(m)
        }
        if (account.isTVAccount) {
            m = HashMap()
            m["type"] = TypeConsts.TV
            m["name"] = "Телевидение"
            data.add(m)
        }
        m = HashMap()
        m["type"] = TypeConsts.MOVIE
        m["name"] = "Фильмы"
        data.add(m)
        m = HashMap()
        m["type"] = TypeConsts.ALL_SHOW
        m["name"] = "Сериалы"
        data.add(m)
        m = HashMap()
        m["type"] = TypeConsts.ALL_ANIME
        m["name"] = "Аниме"
        data.add(m)
    }
}