package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

class LetterAnimeList(context: Context?, m: Map<String?, Any?>?) : LetterShowList(context, m) {
    override val apiAddress: String
        protected get() = ApiConst.LETTER_ANIME + map["name"]
}