package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvsecret.secret.ApiConst

class AllMovieList(context: Context?, map: Map<String?, Any?>?) : AllShowList(context, map ?: emptyMap()) {
    override val apiAddress: String
        protected get() = ApiConst.MOVIE
    protected override val type: String?
        protected get() = "movie"

    override fun setConstData() {
        var m: MutableMap<String?, Any?>
        if (account.isKrasviewAccount) {
            m = HashMap()
            m["type"] = "my_view"
            m["section"] = "movie"
            m["name"] = "Подписки"
            data.add(m)
            m = HashMap()
            m["type"] = "faves"
            m["section"] = "movie"
            m["name"] = "Избранное"
            data.add(m)
        }
        m = HashMap()
        m["type"] = "alfabet_movie"
        m["name"] = "По алфавиту"
        data.add(m)
        m = HashMap()
        m["type"] = TypeConsts.GENRES
        m["section"] = "movie/genre"
        m["name"] = "По жанрам"
        data.add(m)
    }
}