package ru.krasview.kvlib.widget.lists

import android.content.Context
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser

class TVFavoriteRecordList(context: Context?) : TVRecordList(context) {
    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("channel")
        val numOfChannel = nListChannel.length
        var m: MutableMap<String, Any?>
        if (numOfChannel == 0) {
            m = HashMap()
            m = HashMap()
            m["name"] = "<пусто>"
            m["type"] = null
            task!!.onStep(m)
            return
        }
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            if (Parser.getValue("star", locNode) == "1") {
                m = HashMap()
                m["id"] = Parser.getValue("id", locNode)
                m["name"] = Parser.getValue("name", locNode)
                m["uri"] = Parser.getValue("uri", locNode)
                m["img_uri"] = Parser.getValue("image", locNode)
                m["state"] = Parser.getValue("state", locNode)
                m["star"] = Parser.getValue("star", locNode)
                m["type"] = "channel_date_list"
                if (task!!.isCancelled) {
                    return
                }
                task.onStep(m)
            }
        }
    }
}