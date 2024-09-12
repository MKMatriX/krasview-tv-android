package ru.krasview.tv.player

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.example.kvlib.R
import org.w3c.dom.Node
import ru.krasview.kvlib.indep.AuthAccount
import ru.krasview.kvlib.indep.HTTPClient
import ru.krasview.kvlib.indep.Parser

//import androidx.core.content.ContextCompat;
class TVController : FrameLayout {
    var mVideo: VideoInterface? = null
    var mMap: Map<String, Any?>? = null
    var mProgressBar: ProgressBar? = null
    var mTitle: TextView? = null
    var mTime: TextView? = null
    var mInfo: TextView? = null
    var mStop: ImageButton? = null
    var mSize: ImageButton? = null
    var mHandler: Handler = TVControllerHandler(this)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    private fun init() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.kv_controller_tv, this, true)
        mTitle = findViewById<View>(R.id.progress_overlay_tv_name) as TextView
        mTime = findViewById<View>(R.id.progress_overlay_tv_time) as TextView
        mInfo = findViewById<View>(R.id.player_overlay_info) as TextView
        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        mStop = findViewById<View>(R.id.progress_overlay_tv_pause) as ImageButton
        mStop!!.setOnClickListener(listener)
        mSize = findViewById<View>(R.id.progress_overlay_tv_size) as ImageButton
        mSize!!.setOnClickListener(listener)
        mProgressBar!!.progress = 0
        mTitle!!.text = ""
        mTime!!.text = ""
    }

    var listener = OnClickListener { arg0 ->
        if (arg0.id == R.id.progress_overlay_tv_pause) {
            //case R.id.progress_overlay_tv_pause:
            if (mVideo == null) {
                return@OnClickListener
            }
            if (mVideo!!.isPlaying_) {
                mVideo!!.stop()
                mStop!!.setBackgroundResource(R.drawable.ic_new_play)
            } else {
                mVideo!!.play()
                mStop!!.setBackgroundResource(R.drawable.ic_stop)
            }
            //break;
        } else if (arg0.id == R.id.progress_overlay_tv_size) {
            //case R.id.progress_overlay_tv_size:
            var msg = ""
            when (mVideo!!.changeSizeMode()) {
                SURFACE_BEST_FIT -> msg = "Оптимально"
                SURFACE_FIT_HORIZONTAL -> msg = "По горизонтали"
                SURFACE_FIT_VERTICAL -> msg = "По вертикали"
                SURFACE_FILL -> msg = "Заполнение"
                SURFACE_16_9 -> msg = "16 на 9"
                SURFACE_4_3 -> msg = "4 на 3"
                SURFACE_ORIGINAL -> msg = "По центру"
                SURFACE_FROM_SETTINGS -> msg = "Из настроек"
            }
            (context as VideoActivity).showInfo(msg, 1000)
            //break;
        }
    }

    fun setMap(map: Map<String, Any?>?) {
        mMap = map
        mProgressBar!!.progress = 85
        mHandler.removeMessages(GET_INFO)
        mHandler.sendEmptyMessage(GET_INFO)
        mVideo!!.setVideoAndStart(map?.get("uri") as String?, emptyList())
    }

    fun setVideo(video: VideoInterface?) {
        mVideo = video
        mVideo!!.setOnErrorListener { mp, what, extra ->
            (context as VideoActivity).showInfo("Невозможно воспроизвести поток")
            (context as VideoActivity).showOverlay(false)
            true
        }
        /*if(mVideo.getClass().equals(VLCView.class)) {
			//Drawable d = getResources().getDrawable(R.drawable.po_seekbar);
			Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.po_seekbar);
			mProgressBar.setProgressDrawable(d);
		} else if(mVideo.getClass().equals(AVideoView.class)) {

		}*/
    }

    var getInfoTask: GetInfoTask? = null
    val info: Unit
        get() {
            Log.i("Debug", "Получить телепрограмму")
            if (mMap == null) {
                return
            }
            if (getInfoTask != null) {
                getInfoTask!!.cancel(true)
            }
            getInfoTask = GetInfoTask()
            getInfoTask!!.execute(mMap!!["id"] as String?)
        }

    private class TVControllerHandler internal constructor(var mTVController: TVController) :
        Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GET_INFO -> {
                    mTVController.info
                    removeMessages(GET_INFO)
                    sendEmptyMessageDelayed(GET_INFO, (60 * 1000).toLong())
                }
            }
        }
    }

    inner class GetInfoTask : AsyncTask<String?, Void?, Node?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            // mProgressBar.setProgress(0);
            //  mTitle.setText("");
            // mTime.setText("");
        }

        override fun doInBackground(vararg params: String?): Node? {
            val str =
                HTTPClient.getXML(ru.krasview.kvsecret.secret.ApiConst.RECORD, "id=" + params[0], AuthAccount.AUTH_TYPE_TV)
            val document = Parser.XMLfromString(str) ?: return null
            document.normalizeDocument()
            val nList = document.getElementsByTagName("item")
            return if (nList.length == 0) {
                null
            } else nList.item(0)
        }

        override fun onPostExecute(result: Node?) {
            super.onPostExecute(result)
            if (result == null) {
                return
            }
            mTitle!!.text = Parser.getValue("name", result)
            mTime!!.text = Parser.getValue("time", result)
            val i: Int
            i = try {
                Parser.getValue("percent", result).toInt()
            } catch (e: Exception) {
                0
            }
            mProgressBar!!.progress = i
        }

    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    listener.onClick(mStop)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    mVideo!!.stop()
                    mStop!!.setBackgroundResource(R.drawable.ic_new_play)
                    return true
                }
            }
        }
        return false
    }

    fun end() {}

    companion object {
        private const val SURFACE_BEST_FIT = 0
        private const val SURFACE_FIT_HORIZONTAL = 1
        private const val SURFACE_FIT_VERTICAL = 2
        private const val SURFACE_FILL = 3
        private const val SURFACE_16_9 = 4
        private const val SURFACE_4_3 = 5
        private const val SURFACE_ORIGINAL = 6
        private const val SURFACE_FROM_SETTINGS = 7
        const val GET_INFO = 1
    }
}