package ru.krasview.kvlib.widget.lists

import android.content.Context

class AlfabetMovieList(context: Context?) : AlfabetShowList(context) {
    override fun setType(map: MutableMap<String?, Any?>) {
        map["type"] = "letter_movie"
    }
}