package ru.krasview.kvlib.adapter

import android.os.AsyncTask

class LoadDataToGUITask(private val mAdapter: CombineSimpleAdapter) :
    AsyncTask<String, Map<String, Any?>, Void?>() {
    protected override fun doInBackground(vararg params: String): Void? {
        val str = params[0]
        if (str == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
            val m: MutableMap<String, Any?> = HashMap()
            m["name"] = "<невозможно подключиться к серверу>"
            m["type"] = null
            onStep(m)
            return null
        }
        if (str == "") {
            if (mAdapter.emptyList(this)) {
                return null
            }
            var m: MutableMap<String, Any?> = HashMap()
            m = HashMap()
            m["name"] = "<пусто>"
            m["type"] = null
            onStep(m)
            return null
        }
        if (str == "auth failed" || str == "too many attempts") {
            val m: MutableMap<String, Any?> = HashMap()
            m["name"] = "<неверный логин или пароль>"
            m["type"] = null
            onStep(m)
            return null
        }
        mAdapter.parseData(str, this)
        return null
    }

    override fun onProgressUpdate(vararg progress: Map<String, Any?>?) {
        if (progress.size == 1) {
            mAdapter.mData.add(progress[0]!!.toMutableMap())
        }
        if (progress.size == 2) {
            mAdapter.mData.add(0, progress[0]!!.toMutableMap())
        }
        mAdapter.notifyDataSetChanged()
    }

    override fun onPostExecute(result: Void?) {
        if (isCancelled) {
            return
        }
        mAdapter.postExecute()
    }

    fun onStep(vararg m: Map<String, Any?>?) {
        publishProgress(*m)
    }
}