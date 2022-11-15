package ru.krasview.kvlib.widget.lists

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.kvlib.R
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import ru.krasview.kvlib.widget.List
import ru.krasview.kvsecret.secret.ApiConst

@SuppressLint("ViewConstructor")
class PackageList constructor(context: Context?, map: Map<String?, Any?>?) : List(context, map ?: emptyMap()) {
    var onLoadCompleteListener: OnLoadCompleteListener? = null
    override fun showBilling(): Boolean {
        return false
    }

    override val authRequest: Int
        get() = AuthRequestConst.AUTH_TV
    override val apiAddress: String
        get() = ApiConst.PACKET

    public override fun setConstData() {
        val m: MutableMap<String?, Any?>
        m = HashMap()
        m["type"] = "favorite_tv"
        m["name"] = "Избранные телеканалы"
        m["name"] = "пакет \"Полный\""
        m["id"] = "1"
        m["product"] = "portion"
        val sku = "month.1"
        m["sku"] = sku
        data.add(m)
    }

    override fun createAdapter(): CombineSimpleAdapter? {
        return object : CombineSimpleAdapter(this, data, apiAddress, authRequest) {
            inner class PackageViewHolder {
                var name: TextView? = null
                var price: TextView? = null
                var productType: TextView? = null
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                val map = getItem(position) as Map<String, Any?>?
                val holder: PackageViewHolder
                if (convertView == null) {
                    val inflater = parent.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    convertView = inflater.inflate(R.layout.kv_multi_item_billing, parent, false)
                    holder = PackageViewHolder()
                    holder.name = convertView.findViewById<View>(R.id.txt) as TextView
                    holder.price = convertView.findViewById<View>(R.id.price) as TextView
                    holder.productType =
                        convertView.findViewById<View>(R.id.productType) as TextView
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as PackageViewHolder
                }
                holder.name!!.text = map!!["name"] as CharSequence?
                holder.price!!.text = map["price"] as CharSequence?
                if (map["productType"] as CharSequence? == null
                    || map["productType"] as CharSequence? == ""
                ) {
                    holder.productType!!.visibility = GONE
                } else {
                    holder.productType!!.visibility = VISIBLE
                }
                holder.productType!!.text = map["productType"] as CharSequence?
                return convertView!!
            }

            override fun postExecute() {
                complite("")
            }
        }
    }

    private fun complite(result: String) {
        if (onLoadCompleteListener != null) {
            onLoadCompleteListener!!.loadComplete(result)
        }
    }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document?
        mDocument = Parser.XMLfromString(doc)
        if (mDocument == null) {
            return
        }
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("item")
        val numOfChannel = nListChannel.length
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            var m: MutableMap<String, Any?> = HashMap()
            m["name"] = Parser.getValue("name", locNode)
            m["id"] = Parser.getValue("id", locNode)
            m["product"] = "subscription"
            var sku = "test" + "." + m["product"] as String? + "." + m["id"] as String?
            m["sku"] = sku
            if (task!!.isCancelled) {
                return
            }
            m = HashMap()
            m["name"] = Parser.getValue("name", locNode)
            m["id"] = Parser.getValue("id", locNode)
            m["product"] = "portion"
            sku = "test" + "." + m["product"] as String? + "." + m["id"] as String?
            m["sku"] = sku
            if (task.isCancelled) {
                return
            }
        }
    }
}