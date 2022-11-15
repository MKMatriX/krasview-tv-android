package ru.krasview.kvlib.widget

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.kvlib.R
import org.w3c.dom.Element
import ru.krasview.kvlib.indep.KVHttpClient
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import ru.krasview.kvlib.interfaces.ViewProposeListener
import ru.krasview.kvsecret.secret.ApiConst

open class SerialDescription : RelativeLayout {
    protected var image: ImageView? = null
    protected var text: TextView? = null
    @JvmField
	protected var button: Button? = null
    protected var name: TextView? = null
    protected var params: TextView? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init1()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init1()
    }

    constructor(context: Context?) : super(context) {
        init1()
    }

    private fun init1() {
        val ltInflater = (context as Activity).layoutInflater
        ltInflater.inflate(R.layout.kv_serial_description, this, true)
        image = findViewById<View>(R.id.image) as ImageView
        text = findViewById<View>(R.id.text) as TextView
        name = findViewById<View>(R.id.name) as TextView
        button = findViewById<View>(R.id.button) as Button
        params = findViewById<View>(R.id.params) as TextView
    }

    override fun setTag(tag: Any) {
        super.setTag(tag)
        val hm = tag as HashMap<String, Any?>
        ImageAsyncTask().execute(hm["img_uri"] as String?)
        text!!.text = hm["description"] as String?
        name!!.text = hm["show_name"] as CharSequence?
        val get_description = ApiConst.GET_DESCRIPTION
        KVHttpClient.getXMLAsync(get_description, "id=" + hm["id"], object : OnLoadCompleteListener {
            override fun loadComplete(result: String) {}
            override fun loadComplete(address: String, result: String) {
                Log.d("Description", result)
                if (address == get_description) {
                    val mDocument = Parser.XMLfromString(result) ?: return
                    mDocument.normalizeDocument()
                    val node = mDocument.getElementsByTagName("description").item(0)
                    val desc = node as Element
                    var `val` = ""
                    var text = ""
                    text = desc.getElementsByTagName("year").item(0).textContent
                    if (text !== "") `val` += "<b>Год</b>: $text<br>"
                    text = desc.getElementsByTagName("genres").item(0).textContent
                    if (text !== "") `val` += "<b>Жанр</b>: $text<br>"
                    text = desc.getElementsByTagName("country").item(0).textContent
                    if (text !== "") `val` += "<b>Производство</b>: $text<br>"
                    text = desc.getElementsByTagName("production_company").item(0).textContent
                    if (text !== "") `val` += "<b>Компания</b>: $text<br>"
                    text = desc.getElementsByTagName("director").item(0).textContent
                    if (text !== "") `val` += "<b>Режиссёр</b>: $text<br>"
                    text = desc.getElementsByTagName("actors").item(0).textContent
                    if (text !== "") `val` += "<b>Актеры</b>: $text<br>"
                    text = desc.getElementsByTagName("writers").item(0).textContent
                    if (text !== "") `val` += "<b>Сценаристы</b>: $text<br>"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        params!!.text = Html.fromHtml(`val`, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        params!!.text = Html.fromHtml(`val`)
                    }
                }
            }
        })
    }

    internal inner class ImageAsyncTask : AsyncTask<String, Void?, Bitmap>() {
        protected override fun doInBackground(vararg params: String): Bitmap {
            return KVHttpClient.getImage(params[0])
        }

        override fun onPostExecute(bmp: Bitmap) {
            image!!.setImageBitmap(bmp)
        }
    }

    @JvmField
	protected var mViewProposeListener: ViewProposeListener? = null
    fun setViewProposeListener(listener: ViewProposeListener?) {
        mViewProposeListener = listener
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            val e = KeyEvent(event.action, KeyEvent.KEYCODE_DPAD_CENTER)
            return super.dispatchKeyEvent(e)
        }
        return super.dispatchKeyEvent(event)
    }
}