package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.text.Html
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

class MovieList(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    public override fun setConstData() {
        val m: MutableMap<String?, Any?>
        m = HashMap()
        m["type"] = "series_light"
        m["name"] = "Описание"
        m["id"] = map["id"]
        m["img_uri"] = map["img_uri"] as String?
        m["description"] = map["description"] as String?
        m["show_name"] = map["name"]
        data.add(m)
    }

    override val apiAddress: String
        get() = ApiConst.SHOW + "?" + "id=" + map["id"] + "&movie"

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
            m["name"] = Html.fromHtml(Parser.getValue("title", locNode))
            m["uri"] = Parser.getValue("file", locNode)
            m["type"] = "video"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}