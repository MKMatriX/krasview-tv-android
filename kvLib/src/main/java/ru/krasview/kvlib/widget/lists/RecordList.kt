package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.text.Html
import android.util.Log
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class RecordList(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    override val apiAddress: String
        protected get() = (ApiConst.RECORD + "?id=" + map["channel_id"]
                + "&date=" + map["id"])

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
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
            if (Parser.getValue("record", locNode) != null) {
                Log.i("Debug", "запись передачи " + Parser.getValue("record", locNode))
            }
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            val date = Date(1000 * Parser.getValue("start", locNode).toLong())
            val df: DateFormat = SimpleDateFormat("HH:mm")
            val reportDate = df.format(date)
            m["name"] = "$reportDate " + Html.fromHtml(
                Parser.getValue(
                    "name",
                    locNode
                )
            )
            m["uri"] = Parser.getValue("record", locNode)
            if (task!!.isCancelled) {
                return
            }
            if (Parser.getValue("record", locNode) == null) {
                m["state"] = "0"
            } else {
                m["type"] = "tv_record"
                task.onStep(m)
            }
        }
        if (data.size == 0) {
            m = HashMap()
            m["name"] = "<нет записей>"
            m["type"] = null
            task!!.onStep(m)
            return
        }
    }
}