package ru.krasview.kvlib.widget.lists

import android.app.Activity
import android.content.Context
import android.text.Html
import android.widget.AbsListView
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvsecret.secret.ApiConst

open class AllShowList : UserShowList {
    var page = 1
    protected open val type: String?
        protected get() = "series"

    constructor(context: Context?) : super(context) {}
    protected constructor(context: Context?, map: Map<String?, Any?>?) : super(context, map) {}

    override val apiAddress: String
        protected get() = ApiConst.SHOW
    override val authRequest: Int
        protected get() = AuthRequestConst.AUTH_NONE

    public override fun setConstData() {
        var m: MutableMap<String?, Any?>
        if (account.isKrasviewAccount) {
            m = HashMap()
            m["type"] = "my_view"
            m["section"] = "series"
            m["name"] = "Я смотрю"
            data.add(m)
            m = HashMap()
            m["type"] = "faves"
            m["section"] = "series"
            m["name"] = "Избранное"
            data.add(m)
        }
        m = HashMap()
        m["type"] = "alfabet_series"
        m["name"] = "По алфавиту"
        data.add(m)
        m = HashMap()
        m["type"] = TypeConsts.GENRES
        m["section"] = "series/genre"
        m["name"] = "По жанрам"
        data.add(m)
    }

    private fun loadNext() {
        (adapter as AllShowAdapter).loadNext()
    }

    override fun createAdapter(): CombineSimpleAdapter? {
        return AllShowAdapter(data, apiAddress, authRequest)
    }

    private inner class AllShowAdapter(
        constData: MutableList<Map<String?, Any?>>?,
        address: String?, auth: Int
    ) : CombineSimpleAdapter(this@AllShowList, constData, address, auth) {
        fun loadNext() {
            mData.removeAt(mData.size - 1)
            notifyDataSetChanged()
            var params = ""
            page++
            if (mAddress == null) {
                return
            }
            params = "page=$page&$params"
            loadDataFromAddress(mAddress, params)
        }
    }

    override fun refresh() {
        page = 1
        super.refresh()
    }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document = Parser.XMLfromString(doc) ?: return
        mDocument.normalizeDocument()
        val nListChannel = mDocument.getElementsByTagName("unit")
        val numOfChannel = nListChannel.length
        var m: MutableMap<String, Any?>?
        if (data.size > 0) {
            m = data[data.size - 1] as MutableMap<String, Any?>
            if (m["type"] == "next") {
                data.remove(m.toMap())
                (context as Activity).runOnUiThread { adapter?.notifyDataSetChanged() }
            }
        }
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["name"] = Html.fromHtml(Parser.getValue("title", locNode))
            m["img_uri"] = Parser.getValue("thumb", locNode)
            m["description"] = Parser.getValue("description", locNode)
            m["type"] = type
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
        m = HashMap()
        m["type"] = "next"
        m["name"] = "..."
        if (task!!.isCancelled) {
            return
        }
        task.onStep(m)
    }

    override fun init() {
        super.init()
        setOnScrollListener(object : OnScrollListener {
            var past = 0
            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val i = (firstVisibleItem + visibleItemCount) - 1
                if (i == (adapter?.count ?: -2) - 1) {
                    val map = adapter?.getItem(i) as Map<String, Any?>?
                    if (map == null || map["name"] == null) {
                        return
                    }
                    if (map["name"].toString() == "..." && i != past) {
                        loadNext()
                        past = i
                    }
                }
            }

            override fun onScrollStateChanged(arg0: AbsListView, arg1: Int) {
                // TODO Auto-generated method stub
            }
        })
    }
}