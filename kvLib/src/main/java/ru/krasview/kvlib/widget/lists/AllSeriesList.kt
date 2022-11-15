package ru.krasview.kvlib.widget.lists

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

@SuppressLint("ViewConstructor")
open class AllSeriesList(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    override val apiAddress: String
        get() = ApiConst.SHOW + "?" + "id=" + map["id"]

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
            m["name"] =
                Html.fromHtml(Parser.getValue("title", locNode))
            m["uri"] = Parser.getValue("file", locNode)
            m["type"] = "video"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}