package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.Html
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

open class UserShowList : List {
    var mSection: String? = null

    constructor(context: Context?) : super(context, emptyMap()) {}
    protected constructor(context: Context?, map: Map<String?, Any?>?) : super(context, map ?: emptyMap()) {}
    constructor(context: Context?, section: String?) : super(context, emptyMap()) {
        mSection = section
    }

    override val authRequest: Int
        get() = AuthRequestConst.AUTH_KRASVIEW
    override val apiAddress: String
        get() {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val sort = prefs.getString("my_view_sort", "default")
            return if (mSection == null) ApiConst.USER else ApiConst.BASE + mSection + "/user" + if (sort != "default") "?sort=$sort" else ""
        }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        var m: MutableMap<String, Any?>
        var empty = true
        if (doc == "") {
            m = HashMap()
            m["name"] = "<пусто>"
            m["type"] = null
            task!!.onStep(m)
            empty = false
            return
        }
        if (doc == "error") {
            m = HashMap()
            m["name"] = "<ошибка авторизации>"
            m["type"] = null
            task!!.onStep(m)
            empty = false
            return
        }
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("unit")
        val numOfChannel = nListChannel.length
        if (numOfChannel == 0) {
            m = HashMap()
            m["name"] = "<нет подписок>"
            m["type"] = null
            task!!.onStep(m)
            empty = false
            return
        }
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            val s = Parser.getValue("section", locNode)
            m["id"] = Parser.getValue("id", locNode)
            m["name"] =
                Html.fromHtml(Parser.getValue("title", locNode))
            m["img_uri"] = Parser.getValue("thumb", locNode)
            m["description"] = Parser.getValue("description", locNode)
            m["type"] = "series"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
            empty = false
        }
        if (empty == true) {
            m = HashMap()
            m["name"] = "<нет подписок>"
            m["type"] = null
            task!!.onStep(m)
        }
    }
}