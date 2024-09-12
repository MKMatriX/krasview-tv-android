package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.text.Html
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

class DateList(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    override val apiAddress: String
        protected get() = ApiConst.DAYS + "?" + "id=" + map["id"]

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        var num = 0
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("item")
        val numOfChannel = nListChannel.length
        var m: MutableMap<String, Any?>
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["channel_id"] = map["id"]
            m["name"] = Html.fromHtml(Parser.getValue("date", locNode))
            m["type"] = "record_list"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m, null)
            num++
        }
        if (num == 0) {
            m = HashMap()
            m["name"] = "нет записей"
            m["type"] = null
            task!!.onStep(m)
            return
        }
    }
}