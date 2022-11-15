package ru.krasview.kvlib.widget.lists

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.kvlib.R
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst

class NewSeriesList(context: Context?, map: Map<String?, Any?>?) : AllSeriesList(context, map ?: emptyMap()) {
    override val apiAddress: String
        protected get() = ApiConst.USER + "?series=" + map["id"]

    override fun createAdapter(): CombineSimpleAdapter? {
        return object :
            CombineSimpleAdapter(this, data, apiAddress, AuthRequestConst.AUTH_KRASVIEW) {
            inner class LightViewHolder {
                var name: TextView? = null
                var comment: TextView? = null
                var background: View? = null
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                val map = getItem(position) as Map<String, Any?>?
                val type = map!!["type"] as String?
                val holder: LightViewHolder
                if (convertView == null) {
                    holder = LightViewHolder()
                    val inflater = parent.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    convertView = inflater.inflate(R.layout.kv_video_item, parent, false)
                    holder.comment = convertView.findViewById<View>(R.id.comment) as TextView
                    holder.background = convertView.findViewById(R.id.top) as View
                    holder.name = convertView.findViewById<View>(R.id.txt) as TextView
                    if (ListAccount.fromLauncher) {
                        convertView.setBackgroundResource(R.drawable.selector)
                        holder.name!!.setTextColor(colors)
                    }
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as LightViewHolder
                }
                if (type != null && type == "billing") {
                    holder.name!!.visibility = GONE
                    holder.background!!.visibility = VISIBLE
                } else {
                    holder.name!!.visibility = VISIBLE
                    if (map["state"] != null && map["state"] == "0") {
                        holder.background!!.setBackgroundColor(Color.argb(100, 100, 100, 100))
                    } else {
                        holder.background!!.setBackgroundColor(Color.argb(0, 0, 0, 0))
                    }
                    holder.background!!.visibility = GONE
                }
                holder.name!!.text = map["name"] as CharSequence?
                if (type != null && type != "video") {
                    return convertView!!
                }
                if (type != null && (map["first"] as Boolean?)!!) {
                    holder.comment!!.text = "Последняя просмотренная"
                    holder.comment!!.visibility = VISIBLE
                } else {
                    holder.comment!!.visibility = GONE
                }
                return convertView!!
            }

            override fun emptyList(task: LoadDataToGUITask?): Boolean {
                val m: MutableMap<String, Any?>
                m = HashMap()
                m["type"] = "all_series"
                m["name"] = "Все серии"
                m["id"] = map["id"]
                task!!.onStep(m)
                return true
            }
        }
    }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        var m: MutableMap<String, Any?>
        val nListChannel = mDocument.getElementsByTagName("unit")
        val numOfChannel = nListChannel.length
        val locNode0 = nListChannel.item(0)
        m = HashMap()
        m["id"] = Parser.getValue("id", locNode0)
        m["name"] =
            Html.fromHtml(Parser.getValue("title", locNode0))
        m["uri"] = Parser.getValue("file", locNode0)
        m["first"] = true
        m["type"] = "video"
        //m.put("request_time", true);
        if (task!!.isCancelled) {
            return
        }
        task.onStep(m)
        for (nodeIndex in 1 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["name"] = Html.fromHtml(Parser.getValue("title", locNode))
            m["uri"] = Parser.getValue("file", locNode)
            m["first"] = false
            m["type"] = "video"
            //m.put("request_time", true);
            if (task.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}