package ru.krasview.kvlib.widget.lists

import android.content.Context
import ru.krasview.kvsecret.secret.ApiConst

open class LetterShowList(context: Context?, map: Map<String?, Any?>?) : AllShowList(context, map ?: emptyMap()) {
    //не убирать
    override fun setConstData() {}
    override val apiAddress: String
        protected get() = ApiConst.LETTER_SHOW + map["name"]
}