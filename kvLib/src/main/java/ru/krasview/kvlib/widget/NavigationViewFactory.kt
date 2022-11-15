package ru.krasview.kvlib.widget

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import ru.krasview.kvlib.indep.consts.TagConsts
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvlib.interfaces.Factory
import ru.krasview.kvlib.widget.lists.*

class NavigationViewFactory : Factory {
    private var c: Context? = null
    override fun getView(map: Map<String?, Any?>, context: Context): View {
        c = context
        var view: View? = null
        val type = map[TagConsts.TYPE] as String?
        // Log.d("Navigation", "type: " + type);
        view = when {
            type == null -> {
                get_null()
            }
            type == TypeConsts.MAIN -> {
                MainList(c)
            }
            type == TypeConsts.TV -> {
                TVList(c)
            }
            type == TypeConsts.FAVORITE_TV -> {
                TVFavoriteList(c)
            }
            type == TypeConsts.MY_ALL -> {
                UserShowList(c)
            }
            type == "my_view" -> {
                UserShowList(c, map["section"] as String?)
            }
            type == TypeConsts.DESCRIPTION -> {
                Serial(c, map)
            }
            type == "series" -> {
                SeasonList(c, map)
            }
            type == "anime" -> {
                SeasonList(c, map)
            }
            type == "movie" -> {
                MovieList(c, map)
            }
            type == "season_list" -> {
                SeasonList(c, map)
            }
            type == TypeConsts.ALL_SERIES -> {
                AllSeriesList(c, map)
            }
            type == TypeConsts.SEASON -> {
                OneSeasonSeriesList(c, map)
            }
            type == TypeConsts.ALL_SHOW -> {
                AllShowList(c)
            }
            type == TypeConsts.ALL_ANIME -> {
                AllAnimeList(c)
            }
            type == TypeConsts.ALPHABET_SHOW -> {
                AlfabetShowList(c)
            }
            type == TypeConsts.ALPHABET_ANIME -> {
                AlfabetAnimeList(c)
            }
            type == TypeConsts.LETTER_SHOW -> {
                LetterShowList(c, map)
            }
            type == TypeConsts.LETTER_ANIME -> {
                LetterAnimeList(c, map)
            }
            type == TypeConsts.SEARCH_SHOW -> {
                SearchShowList(c)
            }
            type == TypeConsts.SEARCH_ANIME -> {
                SearchAnimeList(c)
            }
            type == TypeConsts.NEW_SERIES -> {
                NewSeriesList(c, map)
            }
            type == TypeConsts.TV_RECORD -> {
                TVRecordList(c)
            }
            type == TypeConsts.FAVORITE_TV_RECORD -> {
                TVFavoriteRecordList(c)
            }
            type == TypeConsts.DATE_LIST -> {
                DateList(c, map)
            }
            type == TypeConsts.RECORD_LIST -> {
                RecordList(c, map)
            }
            type == TypeConsts.MOVIE -> {
                AllMovieList(c, map)
            }
            type == TypeConsts.ALL_MOVIE -> {
                AllMovieList(c, map)
            }
            type == "alfabet_movie" -> {
                AlfabetMovieList(c)
            }
            type == "letter_movie" -> {
                LetterMovieList(c, map)
            }
            type == "search_movie" -> {
                SearchMovieList(c)
            }
            type == "faves" -> {
                FavesList(c, map)
            }
            type == TypeConsts.GENRES -> {
                AllGenreShowList(c, map)
            }
            type.contains("/genre") -> {
                GenreShowList(c, map)
            }
            else -> {
                get_unknown(type)
            }
        }
        if (implementsInterface(view, List::class.java)) {
            (view as List?)!!.factory = this
            view!!.onItemClickListener = KVItemClickListener(
                view
            )
        }
        return view!!
    }

    private fun get_unknown(type: String): View {
        val text = TextView(c)
        text.text = "тип $type"
        text.textSize = 30f
        text.gravity = Gravity.CENTER
        return text
        //return null;
    }

    companion object {
        private fun implementsInterface(`object`: Any?, inter: Class<*>): Boolean {
            return inter.isInstance(`object`)
        }

        private fun get_null(): View? {
            return null
        }
    }
}