package ru.krasview.kvlib.widget.lists

import android.app.Activity
import android.content.Context
import org.w3c.dom.Document
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst
import java.util.*

open class TVList(context: Context?) : ru.krasview.kvlib.widget.List(context, emptyMap()) {
    protected var firstRefresh = true
    override val apiAddress: String
        get() = ApiConst.TV
    override val authRequest: Int
        get() = AuthRequestConst.AUTH_TV
    var timer: Timer? = null
    private fun deleteTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
    }

    private fun restartTimer() {
        deleteTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if (!firstRefresh) {
                    refreshCurrentProgram()
                }
                firstRefresh = false
            }
        }, 0, (1000 * 60).toLong())
    }

    override fun enter() {
        super.enter()
        restartTimer()
    }

    override fun exit() {
        super.exit()
        deleteTimer()
    }

    private fun refreshCurrentProgram() {
        val localData: List<MutableMap<String, Any?>> = adapter?.data ?: emptyList()
        for (i in localData.indices) {
            val item = localData[i]
            item["current_program_name_old"] = item["current_program_name"]
            item.remove("current_program_name")
            (context as Activity).runOnUiThread { adapter?.notifyDataSetChanged() }
        }
    }

    public override fun setConstData() {
        val m: MutableMap<String?, Any?>
        m = HashMap()
        m["type"] = "favorite_tv"
        m["name"] = "Избранные телеканалы"
        data.add(m)

        /*m = new HashMap<String, Object>();
		m.put("type", "record");
		m.put("name", "Записи");
		data.add(m);*/
    }

    override fun parseData(doc: String?, task: LoadDataToGUITask?) {
        val mDocument: Document? = Parser.XMLfromString(doc)
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
        for (nodeIndex in 0 until numOfChannel) {
            val locNode = nListChannel.item(nodeIndex)
            m = HashMap()
            m["id"] = Parser.getValue("id", locNode)
            m["name"] = Parser.getValue("name", locNode)
            m["uri"] = Parser.getValue("uri", locNode)
            m["img_uri"] = Parser.getValue("image", locNode)
            m["state"] = Parser.getValue("state", locNode)
            m["star"] = Parser.getValue("star", locNode)
            m["type"] = "channel"
            if (task!!.isCancelled) {
                return
            }
            task.onStep(m)
        }
    }
}