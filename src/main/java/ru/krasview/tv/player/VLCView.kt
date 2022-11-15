package ru.krasview.tv.player

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.RelativeLayout
import org.videolan1.libvlc.EventHandler
import org.videolan1.libvlc.IVideoPlayer
import org.videolan1.libvlc.LibVLC
import org.videolan1.libvlc.LibVlcException
import org.videolan1.vlc.Util
import ru.krasview.tv.R

//import android.util.Log;
class VLCView : SurfaceView, IVideoPlayer, VideoInterface {
    var mTVController: TVController? = null
    var mVideoController: VideoController? = null
    var mMap: Map<String, Any?>? = emptyMap()
    var parentLayout: RelativeLayout? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    var mLibVLC: LibVLC? = null
    var mVideoHeight = 0
    private var mVideoWidth = 0
    private var mVideoVisibleHeight = 0
    private var mVideoVisibleWidth = 0
    var mSarNum = 0
    var mSarDen = 0
    private var mCurrentSize = SURFACE_FROM_SETTINGS
    var pref_aspect_ratio: String? = "default"
    var pref_aspect_ratio_video: String? = "default"
    private var mAudioTracksList: Map<Int, String>? = null
    var mOnCompletionListener: MediaPlayer.OnCompletionListener? = null
    var mOnErrorListener: MediaPlayer.OnErrorListener? = null

    constructor(context: Context?) : super(context) {
        initVideoView()
    }

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
        initVideoView()
    }

    constructor(context: Context?, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs)
        initVideoView()
    }

    private fun init(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.VideoView)
        a.recycle()
    }

    private fun initVideoView() {
        try {
            mLibVLC = Util.getLibVlcInstance()
        } catch (e: LibVlcException) {
            //Log.d(TAG, "LibVLC initialisation failed");
            mLibVLC = null
            return
        }
        mVideoWidth = 1
        mVideoHeight = 1
        holder.setFormat(PixelFormat.RGBX_8888)
        holder.addCallback(mSHCallback)
        //Log.i(TAG, "Инициализация прошла успешно");
        val em = EventHandler.getInstance()
        em.addHandler(mNativeHandler)
    }

    private val mSHCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (format == PixelFormat.RGBX_8888) {
                //  Log.d(TAG, "Pixel format is RGBX_8888");
            } else if (format == PixelFormat.RGB_565) {
                //  Log.d(TAG, "Pixel format is RGB_565");
            } else if (format == ImageFormat.YV12) {
                //  Log.d(TAG, "Pixel format is YV12");
            } else  //  Log.d(TAG, "Pixel format is other/unknown");
                if (mLibVLC == null) {
                    return
                }
            mLibVLC!!.attachSurface(holder.surface, this@VLCView, width, height)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            //Log.i("Debug", "surface created " + holder);
            //Log.i("Debug", "surface created 1" + getHolder());
            mSurfaceHolder = holder
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (mLibVLC != null) {
                mLibVLC!!.detachSurface()
            }
        }
    }

    fun readMedia(str: String?) {
        if (mLibVLC == null) {
            return
        }
        mLibVLC!!.readMedia(str, false)
    }

    override fun setSurfaceSize(
        width: Int, height: Int, visible_width: Int,
        visible_height: Int, sar_num: Int, sar_den: Int
    ) {
        if (width * height == 0) return
        mVideoHeight = height
        mVideoWidth = width
        mSarNum = sar_num
        mSarDen = sar_den
        mVideoVisibleHeight = visible_height
        mVideoVisibleWidth = visible_width
        val msg = mHandler.obtainMessage(SURFACE_SIZE)
        mHandler.sendMessage(msg)
    }

    var mNativeHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            // Log.i("Debug", "" + msg);
            when (msg.data.getInt("event")) {
                EventHandler.MediaPlayerPlaying -> {}
                EventHandler.MediaPlayerPaused -> {}
                EventHandler.MediaPlayerStopped -> {}
                EventHandler.MediaPlayerEndReached -> if (mOnCompletionListener != null) {
                    mOnCompletionListener!!.onCompletion(null)
                }
                EventHandler.MediaPlayerVout -> {}
                EventHandler.MediaPlayerPositionChanged -> setOverlayProgress()
                EventHandler.MediaPlayerEncounteredError -> if (mOnErrorListener != null) {
                    mOnErrorListener!!.onError(null, 0, 0)
                }
                else -> {}
            }
        }
    }
    var mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SURFACE_SIZE -> changeSurfaceSize()
            }
        }
    }

    fun changeSurfaceSize(w: Int, h: Int) {}
    private fun changeSurfaceSize() {
        //Log.i("Debug", "changeSurfaceSize " + mCurrentSize);

        // get screen size
        var dw = (context as Activity).window.decorView.width
        var dh = (context as Activity).window.decorView.height

        // getWindow().getDecorView() doesn't always take orientation into account, we have to correct the values
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        if (dw > dh && isPortrait || dw < dh && !isPortrait) {
            val d = dw
            dw = dh
            dh = d
        }

        // sanity check
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            //Log.e("Debug", "Invalid surface size " + dw + " " + dh);
            clear()
            return
        }

        // compute the aspect ratio
        var ar: Double
        val vw: Double
        val density = mSarNum.toDouble() / mSarDen.toDouble()
        if (density == 1.0) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth.toDouble()
            ar = mVideoVisibleWidth.toDouble() / mVideoVisibleHeight.toDouble()
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * density
            ar = vw / mVideoVisibleHeight
        }

        //
        //(displayHeight/displayWidth)*(arWidth/arHeight)

        //

        // compute the display aspect ratio
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
            SURFACE_BEST_FIT -> if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
            SURFACE_FIT_HORIZONTAL -> dh = (dw / ar).toInt()
            SURFACE_FIT_VERTICAL -> dw = (dh * ar).toInt()
            SURFACE_FILL -> {}
            SURFACE_16_9 -> {
                ar = 16.0 / 9.0
                ar = ar / mult * dar
                if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
            }
            SURFACE_4_3 -> {
                ar = 4.0 / 3.0
                ar = ar / mult * dar
                if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
            }
            SURFACE_ORIGINAL -> {
                dh = mVideoVisibleHeight
                dw = vw.toInt()
            }
            SURFACE_FROM_SETTINGS -> {
                //	Log.i("Debug", "SURFACE_FROM_SETTINGS");
                if (pref_aspect_ratio_video == "default") {
                    if (dar < ar) dh = (dw / ar).toInt() else dw = (dh * ar).toInt()
                }
//                if (pref_aspect_ratio_video == "fullscreen") {
//                }
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

        // force surface buffer size
        if (mSurfaceHolder != null) {
            mSurfaceHolder!!.setFixedSize(mVideoWidth, mVideoHeight)
        } else if (holder != null) {
            holder.setFixedSize(mVideoWidth, mVideoHeight)
        }

        // set display size
        val lp = layoutParams as RelativeLayout.LayoutParams
        lp.width = dw * mVideoWidth / mVideoVisibleWidth
        lp.height = dh * mVideoHeight / mVideoVisibleHeight
        layoutParams = lp

        // set frame size (crop if necessary)
        /* lp = mSurfaceFrame.getLayoutParams();
		 lp.width = dw;
		 lp.height = dh;
		 mSurfaceFrame.setLayoutParams(lp);*/invalidate()
    }

    private fun setOverlayProgress() {
        if (mLibVLC == null) {
            return
        }
        if (mVideoController != null) {
            mVideoController!!.showProgress()
        }
        return
    }

    override val isPlaying_: Boolean
        get() = if (mLibVLC == null) {
            false
        } else mLibVLC!!.isPlaying

    override fun play() {
        if (mLibVLC == null) {
            return
        }
        mLibVLC!!.play()
    }

    override fun pause() {
        if (mLibVLC == null) {
            return
        }
        mLibVLC!!.pause()
    }

    override fun stop() {
        if (mLibVLC == null) {
            return
        }
        mLibVLC!!.stop()
    }

    fun finalize() {
        mHandler.removeMessages(SURFACE_SIZE)
        if (mLibVLC == null) {
            return
        }
        mLibVLC!!.finalize()
    }

    fun clear() {
        setSurfaceSize(1, 1, 1, 1, 0, 0)
    }

    override fun setVideoAndStart(address: String?, subtitles: List<String>?) {
        clear()
        if (mLibVLC == null) {
            //Log.e("Debug", "Должно появиться сообщение");
            (context as VideoActivity).showInfo("Архитектура не поддерживается \nПопробуйте другой плеер")
            (context as VideoActivity).showOverlay(false)
            return
        }
        mLibVLC!!.readMedia(address)
        if (mVideoController != null) {
            Log.i("Debug", "Проверить число треков")
            mVideoController!!.checkTrack()
        }
    }

    override fun setTVController(tc: TVController?) {
        mTVController = tc
        mTVController!!.setVideo(this)
    }

    override fun setMap(map: Map<String, Any?>?) {
        //Log.i("Debug", "setMap");
        mMap = map
        if (mTVController != null) {
            mTVController!!.setMap(mMap)
        }
        if (mVideoController != null) {
            mVideoController!!.setMap(mMap)
        }
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        pref_aspect_ratio = prefs?.getString("aspect_ratio", "default")
        pref_aspect_ratio_video = if (mMap!!["type"] == "video") {
            // Log.i("Debug", "video");
            prefs?.getString("aspect_ratio_video", "default")
        } else {
            prefs?.getString("aspect_ratio_tv", "default")
            //Log.i("Debug", "channel" );
        }
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (mTVController != null) {
            return mTVController!!.dispatchKeyEvent(event)
        }
        return if (mVideoController != null) {
            mVideoController!!.dispatchKeyEvent(event)
        } else true
    }

    override fun setVideoController(vc: VideoController?) {
        // TODO Auto-generated method stub
        mVideoController = vc
        mVideoController!!.setVideo(this)
    }

    override fun showOverlay(): Boolean {
        setOverlayProgress()
        return true
    }

    override fun hideOverlay(): Boolean {
        return false
    }

    override val progress: Int
        get() = if (mLibVLC == null) {
            0
        } else mLibVLC!!.time.toInt()

    // TODO Auto-generated method stub
    override val leight: Int
        get() =// TODO Auto-generated method stub
            if (mLibVLC == null) {
                0
            } else mLibVLC!!.length.toInt()

    // TODO Auto-generated method stub
    override var time: Int
        get() =// TODO Auto-generated method stub
            if (mLibVLC == null) {
                0
            } else mLibVLC!!.time.toInt()
        set(time) {
            if (mLibVLC == null) {
                return
            }
            mLibVLC!!.time = time.toLong()
        }

    override fun setPosition(time: Int) {
        this.time = time
    }

    override fun changeSizeMode(): Int {
        mCurrentSize++
        if (mCurrentSize > SURFACE_FROM_SETTINGS) {
            mCurrentSize = 0
        }
        setSurfaceSize(
            mVideoWidth, mVideoHeight,
            mVideoVisibleWidth, mVideoVisibleHeight,
            mSarNum, mSarDen
        )
        return mCurrentSize
    }

    override val audioTracksCount: Int
        get() {
            mAudioTracksList = mLibVLC!!.audioTrackDescription
            return if (mLibVLC == null) {
                0
            } else mLibVLC!!.audioTracksCount
        }

    override fun changeAudio(): String? {
        if (mLibVLC == null) {
            return null
        }
        var i = 0
        var listPosition = 0
        val list = ArrayList<Map.Entry<Int, String>>()

        //  Log.i("Debug", "size " + mAudioTracksList.entrySet().size());
        for (entry in mAudioTracksList!!.entries) {
            list.add(entry)
            if (entry.key == mLibVLC!!.audioTrack) {
                listPosition = i
            }
            i++
        }
        // Log.i("Debug", "listPosition " + listPosition);
        var nextPosition = listPosition + 1
        if (nextPosition >= mAudioTracksList!!.entries.size) {
            nextPosition = 0
        }
        mLibVLC!!.audioTrack = list[nextPosition].key
        return list[nextPosition].value
    }

    override val spuTracksCount: Int
        get() = mLibVLC?.spuTracksCount ?: 0

    override val videoTracksCount: Int
        get() = mLibVLC?.videoTracksCount ?: 0

    override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) {
        mOnCompletionListener = listener
    }

    fun videoComplete() {
        if (mOnCompletionListener != null) {
            mOnCompletionListener!!.onCompletion(null)
        }
    }

    override fun changeOrientation(): Int {
        //Log.i("Debug", "AVideoView configuration change");
        this.clear()
        return 0
    }

    override fun end() {
        //mLibVLC.destroy();
        if (mTVController != null) {
            mTVController!!.end()
        }
        if (mVideoController != null) {
            mVideoController!!.end()
        }
        val em = EventHandler.getInstance()
        em.removeHandler(mNativeHandler)
    }


    override fun setOnErrorListener(l: MediaPlayer.OnErrorListener?) {
        mOnErrorListener = l
    }

    override fun changeSubtitle(): String? {
        return "Субтитры"
    }

    companion object {
        const val TAG = "ru.krasview.tv.VideoViewVLC"
        private const val SURFACE_BEST_FIT = 0
        private const val SURFACE_FIT_HORIZONTAL = 1
        private const val SURFACE_FIT_VERTICAL = 2
        private const val SURFACE_FILL = 3
        private const val SURFACE_16_9 = 4
        private const val SURFACE_4_3 = 5
        private const val SURFACE_ORIGINAL = 6
        private const val SURFACE_FROM_SETTINGS = 7
        private const val SURFACE_SIZE = 3
    }
}