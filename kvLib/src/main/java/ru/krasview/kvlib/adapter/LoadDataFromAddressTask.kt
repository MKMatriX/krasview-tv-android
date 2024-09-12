package ru.krasview.kvlib.adapter

import android.os.AsyncTask
import ru.krasview.kvlib.indep.HTTPClient.getXML

internal class LoadDataFromAddressTask(private val mAdapter: CombineSimpleAdapter) :
    AsyncTask<String, Void?, String?>() {
    override fun doInBackground(vararg arg0: String): String? {
        return getXML(arg0[0], arg0[1], mAdapter.withAuth)
    }

    override fun onPostExecute(result: String?) {
        result?.let { LoadDataToGUI(it) }
    }

    private fun LoadDataToGUI(str: String) {
        val task = LoadDataToGUITask(mAdapter)
        task.execute(str)
    }
}