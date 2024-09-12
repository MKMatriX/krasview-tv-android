package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvsecret.secret.ApiConst

class AllAnimeList(context: Context?) : AllShowList(context) {
    override val apiAddress: String
        get() = ApiConst.ANIME

    override fun setConstData() {
        var m: MutableMap<String?, Any?>
        if (account.isKrasviewAccount) {
            m = HashMap()
            m["type"] = "my_view"
            m["section"] = "anime"
            m["name"] = "Я смотрю"
            data.add(m)
            m = HashMap()
            m["type"] = "faves"
            m["section"] = "anime"
            m["name"] = "Избранное"
            data.add(m)
        }
        m = HashMap()
        m["type"] = "alfabet_anime"
        m["name"] = "По алфавиту"
        data.add(m)
        m = HashMap()
        m["type"] = TypeConsts.GENRES
        m["section"] = "anime/genre"
        m["name"] = "По жанрам"
        data.add(m)
    }
}