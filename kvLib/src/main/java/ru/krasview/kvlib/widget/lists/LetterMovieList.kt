package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

class LetterMovieList(context: Context?, map: Map<String?, Any?>?) : LetterShowList(context, map ?: emptyMap()) {
    override val apiAddress: String
        protected get() = ApiConst.MOVIE + "/letter/" + map["name"]
}