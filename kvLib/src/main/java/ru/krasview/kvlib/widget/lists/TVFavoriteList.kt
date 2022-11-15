package ru.krasview.kvlib.widget.lists

import android.content.Context
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst

class TVFavoriteList(context: Context?) : TVList(context) {
    override val authRequest: Int
        protected get() = AuthRequestConst.AUTH_TV
    override val apiAddress: String
        protected get() = ApiConst.TV

    override fun setConstData() {
        /*Map<String, Object> m;
		m = new HashMap<String, Object>();
		m.put("type", "record_favorite");
		m.put("name", "Записи");
		data.add(m);*/
    }

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
            m["name"] = "<пусто>"
            m["type"] = null
            task!!.onStep(m)
            return
        }
        var num = 0
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            if (Parser.getValue("star", locNode) == "1") {
                m = HashMap()
                m["id"] = Parser.getValue("id", locNode)
                m["name"] = Parser.getValue("name", locNode)
                m["uri"] = Parser.getValue("uri", locNode)
                m["img_uri"] = Parser.getValue("image", locNode)
                //m.put("image", Parser.getImage(Parser.getValue("image", locNode)));
                m["state"] = Parser.getValue("state", locNode)
                m["star"] = Parser.getValue("star", locNode)
                m["type"] = "channel"
                if (task!!.isCancelled) {
                    return
                }
                num++
                task.onStep(m)
            }
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