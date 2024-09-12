package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

class SearchAnimeList(context: Context?) : SearchShowList(context) {
    override val apiAddress: String
        protected get() = ApiConst.ANIME
}