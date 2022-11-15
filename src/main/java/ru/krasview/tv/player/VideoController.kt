package ru.krasview.tv.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.view.isGone
import com.example.kvlib.R
import org.videolan1.vlc.Util
import ru.krasview.kvlib.indep.HTTPClient
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst
import java.util.*

class VideoController : FrameLayout {
    var mPause: ImageButton? = null
    var mBackward: ImageButton? = null
    var mForward: ImageButton? = null
    var mSeekbar: SeekBar? = null
    var mTime: TextView? = null
    var mLeight: TextView? = null
    var mSize: ImageButton? = null
    var mAudio: ImageButton? = null
    var mSubtitle: ImageButton? = null
    var timer: Timer? = null
    var time = 0

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
        inflater.inflate(R.layout.kv_controller_video, this, true)
        mPause = findViewById<View>(R.id.player_overlay_play) as ImageButton
        mPause!!.setOnClickListener(listener)
        mBackward = findViewById<View>(R.id.player_overlay_backward) as ImageButton
        mBackward!!.setOnClickListener(listener)
        mForward = findViewById<View>(R.id.player_overlay_forward) as ImageButton
        mForward!!.setOnClickListener(listener)
        mSeekbar = findViewById<View>(R.id.player_overlay_seekbar) as SeekBar
        mSeekbar!!.setOnSeekBarChangeListener(mSeekListener)
        mTime = findViewById<View>(R.id.player_overlay_time) as TextView
        mLeight = findViewById<View>(R.id.player_overlay_length) as TextView
        mSize = findViewById<View>(R.id.player_overlay_size) as ImageButton
        mSize!!.setOnClickListener(listener)
        mAudio = findViewById<View>(R.id.player_overlay_audio) as ImageButton
        mAudio!!.setOnClickListener(listener)
        mSubtitle = findViewById<View>(R.id.player_overlay_subtitle) as ImageButton
        mSubtitle!!.setOnClickListener(listener)
        //mSubtitle.setVisibility(View.VISIBLE);
        timer = Timer()
    }

    private var listener = OnClickListener { v ->
        //switch(v.getId()){
        when (v.id) {
            R.id.player_overlay_subtitle -> {
                //	case R.id.player_overlay_subtitle:
                mVideo!!.changeSubtitle()
//                (context as VideoActivity).showInfo(, 1000)
                //	break;
            }
            R.id.player_overlay_audio -> {
                //case R.id.player_overlay_audio:
                mVideo!!.changeAudio()
//                (context as VideoActivity).showInfo(mVideo!!.changeAudio(), 1000)
                //	break;
            }
            R.id.player_overlay_play -> {
                //case R.id.player_overlay_play:
                if (mVideo!!.isPlaying_) {
                    mVideo!!.pause()
                    mPause!!.setBackgroundResource(R.drawable.ic_new_play)
                    (context as VideoActivity).showOverlay(false)
                } else {
                    mVideo!!.play()
                    mPause!!.setBackgroundResource(R.drawable.ic_new_pause)
                }
                //break;
            }
            R.id.player_overlay_backward -> {
                //case R.id.player_overlay_backward:
                goBackward()
                //break;
            }
            R.id.player_overlay_forward -> {
                //case R.id.player_overlay_forward:
                goForward()
                //	break;
            }
            R.id.player_overlay_size -> {
                //case R.id.player_overlay_size:
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
    }
    var mHandler: Handler = VideoControllerHandler(this)

    private class VideoControllerHandler internal constructor(var mTVController: VideoController) :
        Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                CHECK_TRACKS -> mTVController.setESTrackLists()
                UPDATE_REMOTE_PROGRESS -> {}
            }
        }
    }

    private val mSeekListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            //   mDragging = true;
            //   showOverlay(OVERLAY_INFINITE);
            Log.i(TAG, "Юзер start touch ")
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            Log.i(TAG, "Юзер stop touch")
            //  mDragging = false;
            //  showOverlay();
            //  hideInfo();
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            //Log.i("Debug", "кто-то перемотал видео");
            if (fromUser) {
                Log.i(TAG, "Юзер перемотал видео")
                //  mLibVLC.setTime(progress);
                //  setOverlayProgress();
                //  mTime.setText(Util.millisToString(progress));
                //  showInfo(Util.millisToString(progress));

                //	mLibVLC.setTime(progress);
                //    setOverlayProgress();
                mVideo!!.setPosition(progress)
                showProgress()
            } else {
                mSeekbar!!.max = mVideo!!.leight
                mSeekbar!!.progress = progress
            }
        }
    }

    fun setVideo(video: VideoInterface?) {
        mVideo = video
        mVideo!!.setOnErrorListener { mp, what, extra ->
            (context as VideoActivity).showInfo("Невозможно воспроизвести видео")
            (context as VideoActivity).showOverlay(false)
            true
        }
        if (mVideo!!.javaClass == VLCView::class.java) {
            var d = resources.getDrawable(R.drawable.po_seekbar)
            mSeekbar!!.progressDrawable = d
            d = resources.getDrawable(R.drawable.ic_seekbar_thumb)
            //android:thumb="@drawable/ic_seekbar_thumb"
            mSeekbar!!.thumb = d
        } else if (mVideo!!.javaClass == AVideoView::class.java) {
        }
    }

    fun showProgress() {
        /*Throwable trace = new Exception();
		Log.d("Debug", "Показ прогресса " + Util.millisToString(mVideo.getTime()), trace);*/
        mSeekListener.onProgressChanged(mSeekbar, mVideo!!.progress, false)
        mTime!!.text =
            "" + Util.millisToString(mVideo!!.time.toLong())
        mLeight!!.text =
            "" + Util.millisToString(mVideo!!.leight.toLong())
    }

    private fun goBackward() {
        mVideo!!.time = -10000
        showProgress()
    }

    private fun goForward() {
        mVideo!!.time = 10000
        showProgress()
    }

    var id: String? = null
    fun setMap(map: Map<String, Any?>?) {
        time = 0
        mMap = map?.toMutableMap() ?: return
        val videoUrl = mMap!!["uri"] as? String ?: ""
        //SentProgressRunnable
        val task: AsyncTask<Void, Void?, Int> = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Void, Void?, Int>() {
            protected override fun doInBackground(vararg arg0: Void): Int {
                id = mMap!!["id"] as String?
                val rt = mMap!!["rt"] as Boolean?
                mMap!!.remove("rt")
                if (rt == null && !(mMap!!["request_time"] as Boolean) || rt == false) {
                    //Log.i("VideoController", "VideoController не запрашивать время " +rt);
                    return 0
                } else {
                    //Log.i("VideoController", "VideoController запрашивать время");
                }

                // todo request subtitles
                val subs = mutableListOf<String>()
                val rootUrl = videoUrl.dropLastWhile { it != '/' }
                (0..2).forEach {
                    val assUrl = rootUrl.plus("$it.ass")
                    try {
                        val sub = HTTPClient.run(assUrl)
                        if (sub.isNotEmpty() && !sub.contains("404 Not Found"))
                            subs.add(assUrl)
                    } catch (_: Exception) { }

                    val srtUrl = rootUrl.plus("$it.srt")
                    try {
                        val sub = HTTPClient.run(srtUrl)
                        if (sub.isNotEmpty() && !sub.contains("404 Not Found"))
                            subs.add(srtUrl)
                    } catch (_: Exception) { }

                    val ssaUrl = rootUrl.plus("$it.ssa")
                    try {
                        val sub = HTTPClient.run(ssaUrl)
                        if (sub.isNotEmpty() && !sub.contains("404 Not Found"))
                            subs.add(ssaUrl)
                    } catch (_: Exception) { }
                }
                if (subs.isNotEmpty()) {
                    mMap!!["subtitles"] = subs
                    Log.e("subtitle", "found ${subs.count()}")
                }

//                // todo request subtitles
//                val mpdUrl = videoUrl.dropLastWhile { it != '.' }.plus("mpd")
//                Log.e("mpd", mpdUrl)
//                try {
//                    val mpd = HTTPClient.run(mpdUrl)
//                    if (mpd.isNotEmpty()) {
//                        mMap!!["uri"] = mpdUrl
//                    }
//                } catch (e: Exception) {
//                    Log.i("http", "error")
//                }

                val result = HTTPClient.getXML(
                    ru.krasview.kvsecret.secret.ApiConst.GET_POSITION,
                    "id=$id",
                    AuthRequestConst.AUTH_KRASVIEW
                )
                return if (result != null && result != "" && result != "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
                    val r = result.toFloat().toInt()
                    //Log.i("VideoController", "Получено время " + Util.millisToString(r*1000));
                    r * 1000
                } else {
                    0
                }
            }

            override fun onPostExecute(result: Int) {
                time = result
                mVideo!!.setVideoAndStart(mMap!!["uri"] as String?, mMap!!["subtitles"] as? List<String>)
                mVideo!!.setPosition(time)
                showProgress()

                class LooperThread : Thread() {
                    var mHandler: Handler? = null
                    override fun run() {
                        Looper.prepare()
                        mHandler = object : Handler(Looper.myLooper()!!) {
                            override fun handleMessage(msg: Message) {

                                val address = ApiConst.SET_POSITION
                                val progress = mVideo!!.time
                                if (progress > 0 && mVideo!!.isPlaying_) {
                                    val params = "video_id=" + id + "&time=" + progress / 1000
                                    Log.i(
                                        TAG,
                                        "Отправлено: id=" + id + " time=" + Util.millisToString(
                                            progress.toLong()
                                        )
                                    )
                                    HTTPClient.getXML(
                                        address,
                                        params,
                                        AuthRequestConst.AUTH_KRASVIEW
                                    )
                                }
                            }
                        }
                        Looper.loop()
                    }

                }

                val updatePos: TimerTask = object : TimerTask() {
                    override fun run() {
                        LooperThread().run()
                    }
                }
                timer!!.schedule(updatePos, 0, 20000)
            }
        }
        task.execute()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        //Log.d("Debug","нажата клавиша");
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (!event.isLongPress) listener.onClick(mPause)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    listener.onClick(mBackward)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    listener.onClick(mForward)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    mVideo!!.stop()
                    mPause!!.setBackgroundResource(R.drawable.ic_new_play)
                    return true
                }
                KeyEvent.KEYCODE_0 -> {
                    mVideo!!.time = 0
                    return true
                }
                KeyEvent.KEYCODE_1 -> {
                    mVideo!!.time = 10
                    return true
                }
                KeyEvent.KEYCODE_2 -> {
                    mVideo!!.time = 20
                    return true
                }
                KeyEvent.KEYCODE_3 -> {
                    mVideo!!.time = 30
                    return true
                }
                KeyEvent.KEYCODE_4 -> {
                    mVideo!!.time = 40
                    return true
                }
                KeyEvent.KEYCODE_5 -> {
                    mVideo!!.time = 50
                    return true
                }
                KeyEvent.KEYCODE_6 -> {
                    mVideo!!.time = 60
                    return true
                }
                KeyEvent.KEYCODE_7 -> {
                    mVideo!!.time = 70
                    return true
                }
                KeyEvent.KEYCODE_8 -> {
                    mVideo!!.time = 80
                    return true
                }
                KeyEvent.KEYCODE_9 -> {
                    mVideo!!.time = 90
                    return true
                }
                else -> Log.i(TAG, "Нажата клавиша: " + event.keyCode)
            }
        }
        return false
    }

    fun checkTrack() {
        mHandler.removeMessages(CHECK_TRACKS)
        mHandler.sendEmptyMessageDelayed(CHECK_TRACKS, 2500)
    }

    private fun setESTrackLists() {
        mAudio?.isGone = mVideo!!.audioTracksCount < 2 && mVideo!!.spuTracksCount < 2 && mVideo!!.videoTracksCount < 2
    }

    fun end() {
        timer!!.cancel()
        timer!!.purge()
        Log.i(TAG, "end")
    }

    operator fun next() {
        (context as VideoActivity).onNext(false)
    }

    companion object {
        var mVideo: VideoInterface? = null
        const val TAG = "Krasview/VideoContro"
        private const val CHECK_TRACKS = 42
        private const val UPDATE_REMOTE_PROGRESS = 43
        private const val SURFACE_BEST_FIT = 0
        private const val SURFACE_FIT_HORIZONTAL = 1
        private const val SURFACE_FIT_VERTICAL = 2
        private const val SURFACE_FILL = 3
        private const val SURFACE_16_9 = 4
        private const val SURFACE_4_3 = 5
        private const val SURFACE_ORIGINAL = 6
        private const val SURFACE_FROM_SETTINGS = 7
        var mMap: MutableMap<String, Any?>? = null
    }
}