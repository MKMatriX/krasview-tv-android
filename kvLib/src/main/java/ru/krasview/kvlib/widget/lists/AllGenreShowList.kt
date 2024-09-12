package ru.krasview.kvlib.widget.lists

import android.content.Context
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

class AllGenreShowList(context: Context?, map: Map<String?, Any?>) : List(context, map ?: emptyMap()) {
    var section: String?

    init {
        section = map["section"] as String?
    }

    protected fun setType(map: MutableMap<String?, Any?>) {
        map["type"] = section
    }

    override val apiAddress: String
        protected get() = ApiConst.BASE + "/genres"

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("unit")
        val numOfChannel = nListChannel.length
        var m: MutableMap<String, Any?>
        if (numOfChannel == 0) {
            m = HashMap()
            m["name"] = "<пусто>"
            m["type"] = null
            task!!.onStep(m)
            return
        }
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["name"] = Parser.getValue("title", locNode)
            m["type"] = section
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}