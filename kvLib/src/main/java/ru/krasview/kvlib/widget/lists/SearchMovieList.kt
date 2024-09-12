package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

class SearchMovieList(context: Context?) : SearchShowList(context) {
    override val apiAddress: String
        protected get() = ApiConst.MOVIE
}