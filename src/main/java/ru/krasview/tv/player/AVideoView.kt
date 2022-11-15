package ru.krasview.tv.player

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.VideoView

class AVideoView(context: Context) : VideoView(context), VideoInterface {
    var mTVController: TVController? = null
    var mVideoController: VideoController? = null
    var mMap: Map<String, Any?>? = null
    var mMediaPlayer: MediaPlayer? = null
    var mVideoWidth = 100
    var mVideoHeight = 100
    var dw = 1
    var dh = 1
    var stopped = false
    private var mCurrentSize = SURFACE_FROM_SETTINGS
    var pref_aspect_ratio: String? = "default"
    var pref_aspect_ratio_video: String? = "default"

    init {
        init()
    }

    private fun init() {
        setOnPreparedListener { pMp ->
            mMediaPlayer = pMp //use a global variable to get the object
            mVideoWidth = pMp.videoWidth
            mVideoHeight = pMp.videoHeight
            changeSize()
        }
        this.isFocusable = false
        this.isClickable = false
    }

    private val prefs: Unit
        get() {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            pref_aspect_ratio = prefs?.getString("aspect_ratio", "default")
            pref_aspect_ratio_video = if (mMap!!["type"] == "video") {
                prefs?.getString("aspect_ratio_video", "default")
            } else {
                prefs?.getString("aspect_ratio_tv", "default")
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (dw * dh == 1) {
            return
        }
        setMeasuredDimension(dw, dh)
    }

    override fun setVideoAndStart(address: String?, subtitles: List<String>?) {
        var address: String? = address
        if (address == null) {
            address = "null"
        }
        setVideoPath(address)
        if (!this.isPlaying_) {
            start()
        }
    }

    override fun stop() {
        (this as VideoView).stopPlayback()
        stopped = true
    }

    override fun setTVController(tc: TVController?) {
        mTVController = tc
        mTVController!!.setVideo(this)
    }

    override fun setMap(map: Map<String, Any?>?) {
        mMap = map
        if (mTVController != null) {
            mTVController!!.setMap(mMap!!)
        }
        if (mVideoController != null) {
            mVideoController!!.setMap(mMap)
        }
        prefs
        stopped = false
    }

    override fun play() {
        if (stopped) {
            val mLocation = mMap!!["uri"] as String?
            stopped = false
            setVideoAndStart(mLocation!!, emptyList())
        }
        start()
    }

    override val isPlaying_: Boolean
        get() = !stopped

    override fun setVideoController(vc: VideoController?) {
        mVideoController = vc
        mVideoController!!.setVideo(this)
    }

    override fun showOverlay(): Boolean {
        mHandler.removeMessages(SHOW_PROGRESS)
        mHandler.sendEmptyMessage(SHOW_PROGRESS)
        return true
    }

    override fun hideOverlay(): Boolean {
        return false
    }

    override val progress: Int
        get() = this.currentPosition

    override val leight: Int
        get() = this.duration

    override var time: Int
        get() = this.currentPosition
        set(value) { seekTo(time) }


    override fun setPosition(time: Int) {
        this.time = time
    }

    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SURFACE_SIZE -> {}
                SHOW_PROGRESS -> setOverlayProgress()
            }
        }
    }

    private fun setOverlayProgress() {
        if (mVideoController != null) {
            mVideoController!!.showProgress()
        }
        mHandler.removeMessages(SHOW_PROGRESS)
        val msg = mHandler.obtainMessage(SHOW_PROGRESS)
        mHandler.sendMessageDelayed(msg, 1000)
        return
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mTVController != null) {
            return mTVController!!.dispatchKeyEvent(event)
        }
        return if (mVideoController != null) {
            mVideoController!!.dispatchKeyEvent(event)
        } else true
    }

    override fun changeSizeMode(): Int {
        mCurrentSize++
        if (mCurrentSize > SURFACE_FROM_SETTINGS) {
            mCurrentSize = 0
        }
        changeSize()
        return mCurrentSize
    }

    private fun changeSize() {
        calcSize()
        holder.setFixedSize(dw, dh)
        forceLayout()
        invalidate()
    }

    private fun calcSize() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val currentapiVersion = Build.VERSION.SDK_INT
        if (currentapiVersion >= 13) {
            val size = Point()
            display.getSize(size)
            dw = size.x
            dh = size.y
        } else {
            dw = display.width
            dh = display.height
        }

        // dw = ((Activity)getContext()).getWindow().getDecorView().getWidth();
        //dh = ((Activity)getContext()).getWindow().getDecorView().getHeight();
        //  Log.i("Debug", "calc size " + dw + "x" + dh);
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        if (dw > dh && isPortrait || dw < dh && !isPortrait) {
            val d = dw
            dw = dh
            dh = d
        }
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            return
        }
        var ar = mVideoWidth.toDouble() / mVideoHeight.toDouble()
        val dar = dw.toDouble() / dh.toDouble()
        var mult = dar
        if (pref_aspect_ratio == "4:3") {
            mult = 4 / 3.0
        } else if (pref_aspect_ratio == "11:9") {
            mult = 11 / 9.0
        } else if (pref_aspect_ratio == "16:9") {
            mult = 16 / 9.0
        }
        ar = ar / mult * dar
        when (mCurrentSize) {
            SURFACE_BEST_FIT -> if (ar > dar) {
                dh = (dw / ar).toInt()
            } else {
                dw = (dh * ar).toInt()
            }
            SURFACE_FILL -> {}
            SURFACE_ORIGINAL -> {
                dw = mVideoWidth
                dh = mVideoHeight
            }
            SURFACE_4_3 -> {
                ar = 4.0 / 3.0
                ar = ar / mult * dar
                if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
            }
            SURFACE_16_9 -> {
                ar = 16.0 / 9.0
                ar = ar / mult * dar
                if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
            }
            SURFACE_FIT_HORIZONTAL -> dh = (dw / ar).toInt()
            SURFACE_FIT_VERTICAL -> dw = (dh * ar).toInt()
            SURFACE_FROM_SETTINGS -> {
                if (pref_aspect_ratio_video == "default") {
                    if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
                }
                if (pref_aspect_ratio_video == "fullscreen") {
                }
                if (pref_aspect_ratio_video == "4:3") {
                    ar = 4.0 / 3.0
                    ar = ar / mult * dar
                    if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
                }
                if (pref_aspect_ratio_video == "16:9") {
                    ar = 16.0 / 9.0
                    ar = ar / mult * dar
                    if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
                }
            }
        }
    }


    override fun changeAudio(): String {
        return "Следующая дорожка"
    }


    override fun changeOrientation(): Int {
        changeSize()
        return 0
    }

    override fun end() {
        if (mTVController != null) {
            mTVController!!.end()
        }
        if (mVideoController != null) {
            mVideoController!!.end()
        }
        mHandler.removeMessages(SHOW_PROGRESS)
    }

    override fun changeSubtitle(): String {
        return ""
    }

    override val audioTracksCount: Int
        get() = 0
    override val spuTracksCount: Int
        get() = 0
    override val videoTracksCount: Int
        get() = 0

    companion object {
        private const val SHOW_PROGRESS = 2
        private const val SURFACE_SIZE = 3
        private const val SURFACE_BEST_FIT = 0
        private const val SURFACE_FIT_HORIZONTAL = 1
        private const val SURFACE_FIT_VERTICAL = 2
        private const val SURFACE_FILL = 3
        private const val SURFACE_16_9 = 4
        private const val SURFACE_4_3 = 5
        private const val SURFACE_ORIGINAL = 6
        private const val SURFACE_FROM_SETTINGS = 7
    }
}