package ru.krasview.kvlib.adapter

import android.os.AsyncTask
import android.util.Log
import ru.krasview.kvlib.indep.HTTPClient.getXML
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst

internal class LoadCurrentProgram(
    var mAdapter: CombineSimpleAdapter,
    var mMap: MutableMap<String, Any?>
) : AsyncTask<String?, Void?, Map<String, Any?>?>() {
    override fun doInBackground(vararg arg0: String?): Map<String, Any?>? {
        val str = getXML(ApiConst.RECORD, "id=" + mMap["id"], AuthRequestConst.AUTH_NONE)
        val m: MutableMap<String, Any?>
        if (str == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
            m = HashMap()
            m["name"] = "<невозможно подключиться к серверу>"
            m["progress"] = "0".toInt()
            m["time"] = ""
            return m
        }
        if (str == "") {
            m = HashMap()
            m["name"] = "<пусто>"
            m["progress"] = "0".toInt()
            m["time"] = ""
            return m
        }
        val mDocument = Parser.XMLfromString(str)
        if (mDocument == null) {
            m = HashMap()
            m["name"] = ""
            m["time"] = ""
            m["progress"] = 0
            return m
        }
        mDocument.normalizeDocument()
        val mainNode = mDocument.firstChild
        m = HashMap()
        m["name"] = Parser.getValue("name", mainNode)
        m["time"] = Parser.getValue("time", mainNode)
        try {
            m["progress"] = Parser.getValue("percent", mainNode).toInt()
        } catch (e: Exception) {
            Log.i(
                "Debug",
                "неверный persent " + " id= " + mMap["id"] + "name= " + Parser.getValue(
                    "name",
                    mainNode
                )
            )
            m["progress"] = 0
        }
        return m
    }

    override fun onPostExecute(result: Map<String, Any?>?) {
        if (result == null) {
            return
        }
        mMap["current_program_name"] = result["name"] as CharSequence
        mMap["current_program_progress"] = result["progress"] as Int
        mMap["current_program_time"] = result["time"] as CharSequence
        mAdapter.notifyDataSetChanged()
    }
}