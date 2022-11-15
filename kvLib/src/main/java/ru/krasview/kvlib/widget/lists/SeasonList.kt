package ru.krasview.kvlib.widget.lists

import android.annotation.SuppressLint
import android.content.Context
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

@SuppressLint("ViewConstructor")
class SeasonList(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    override val apiAddress: String
        get() = ApiConst.ALL_SEASONS + "?id=" + map["id"]

    public override fun setConstData() {
        var m: MutableMap<String?, Any?>
        if (account.isKrasviewAccount) {
            m = HashMap()
            m["type"] = "new_series"
            m["name"] = "Новые серии"
            m["id"] = map["id"]
            data.add(m)
        }
        m = HashMap()
        m["type"] = "series_light"
        m["name"] = "Описание"
        m["id"] = map["id"]
        m["img_uri"] = map["img_uri"] as String?
        m["description"] = map["description"] as String?
        m["show_name"] = map["name"]
        data.add(m)
        m = HashMap()
        m["type"] = "all_series"
        m["name"] = "Все серии"
        m["id"] = map["id"]
        data.add(m)
    }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document = Parser.XMLfromString(doc) ?: return
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("unit")
        val numOfChannel = nListChannel.length
        var m: MutableMap<String, Any?>
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["name"] = Parser.getValue("title", locNode)
            m["type"] = "season_series"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}