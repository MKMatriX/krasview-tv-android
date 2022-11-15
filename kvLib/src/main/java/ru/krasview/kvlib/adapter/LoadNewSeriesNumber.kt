package ru.krasview.kvlib.adapter

import android.os.AsyncTask
import org.w3c.dom.Document
import ru.krasview.kvlib.indep.HTTPClient.getXML
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst

internal class LoadNewSeriesNumber(
    private val mAdapter: CombineSimpleAdapter,
    var mMap: MutableMap<String, Any?>
) : AsyncTask<Void, Void?, Int>() {
    override fun doInBackground(vararg arg0: Void): Int {
        val address = ApiConst.USER + "?series=" + mMap["id"]
        val result = getXML(address, "", AuthRequestConst.AUTH_KRASVIEW)
        return if (result == "") {
            0
        } else {
            val mDocument: Document?
            mDocument = Parser.XMLfromString(result)
            if (mDocument == null) {
                return 0
            }
            mDocument.normalizeDocument()
            val nListChannel = mDocument.getElementsByTagName("unit")
            nListChannel.length - 1
        }
    }

    override fun onPostExecute(result: Int) {
        mMap["new_series"] = result
        mAdapter.notifyDataSetChanged()
    }
}