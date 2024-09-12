package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.net.Uri
import android.text.Html
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.interfaces.SearchInterface
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

open class SearchShowList(context: Context?) : List(context, emptyMap()), SearchInterface {
    override fun goSearch(str: String) {
        val builder = Uri.parse(apiAddress).buildUpon()
        builder.appendQueryParameter("search", str)
        adapter?.setAddress(builder.build().toString())
        refresh()
    }

    override val apiAddress: String
        protected get() = ApiConst.SHOW

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
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
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
        }
    }
}