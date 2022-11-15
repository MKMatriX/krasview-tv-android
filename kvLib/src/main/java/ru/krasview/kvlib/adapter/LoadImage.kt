package ru.krasview.kvlib.adapter

import android.graphics.Bitmap
import android.os.AsyncTask
import android.widget.ImageView
import ru.krasview.kvlib.indep.KVHttpClient.Companion.getImage

internal class LoadImage(
    var mAdapter: CombineSimpleAdapter,
    var mImage: ImageView,
    var mMap: MutableMap<String, Any?>
) : AsyncTask<String, Void?, Bitmap>() {
    override fun doInBackground(vararg arg0: String): Bitmap {
        return getImage(arg0[0])
    }

    override fun onPostExecute(bmp: Bitmap) {
        mMap["image"] = bmp
        mAdapter.notifyDataSetChanged()
    }
}