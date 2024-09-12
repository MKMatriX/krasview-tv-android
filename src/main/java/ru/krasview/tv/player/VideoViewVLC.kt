package ru.krasview.tv.player

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.Toast
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.libvlc.interfaces.IVLCVout.OnNewVideoLayoutListener
import java.lang.ref.WeakReference
import java.net.URL

class VideoViewVLC(private val video_context: Context) : SurfaceView(
    video_context
), IVLCVout.Callback, OnNewVideoLayoutListener, VideoInterface {
    private var mSurface: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var libvlc: LibVLC? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    var mTVController: TVController? = null
    var mVideoController: VideoController? = null
    var mMap: Map<String, Any?>? = null
    private var mOnLayoutChangeListener: OnLayoutChangeListener? = null
    var stopped = false
    private val mCurrentSize = SURFACE_FROM_SETTINGS
    var pref_aspect_ratio: String? = "default"
    var pref_aspect_ratio_video: String? = "default"
    private fun init() {
        mSurface = this
        surfaceHolder = (mSurface as VideoViewVLC).holder
        this.isFocusable = false
        this.isClickable = false
    }

    override fun setVideoAndStart(address: String?, subtitles: List<String>?) {
        createPlayer(address)
    }

    override fun stop() {
        mMediaPlayer?.stop()
        stopped = true
    }

    override fun pause() {
        mMediaPlayer?.pause()
    }

    override fun play() {
        mMediaPlayer?.play()
    }

    override fun setTVController(tc: TVController?) {
        mTVController = tc
        mTVController!!.setVideo(this)
    }

    override fun setVideoController(vc: VideoController?) {
        mVideoController = vc
        mVideoController!!.setVideo(this)
    }

    override fun setMap(map: Map<String, Any?>?) {
        mMap = map
        mTVController?.setMap(mMap)
        mVideoController?.setMap(mMap)
        prefs
        stopped = false
    }

    override val isPlaying_: Boolean
        get() = mMediaPlayer!!.isPlaying

    override fun showOverlay(): Boolean {
        mHandler!!.removeMessages(SHOW_PROGRESS)
        mHandler!!.sendEmptyMessage(SHOW_PROGRESS)
        return true
    }

    override fun hideOverlay(): Boolean {
        return false
    }

    override val progress = mMediaPlayer?.time?.toInt() ?: 0
    override val leight = mMediaPlayer?.length?.toInt() ?: 0
    override var time: Int
        get() = mMediaPlayer?.time?.toInt() ?: 0
        set(time) { mMediaPlayer?.time = time.toLong() }

    override fun setPosition(time: Int) {
        this.time = time
    }

    override fun changeSizeMode(): Int {
        return 0
    }

    override fun changeAudio(): String? {
        return null
    }

    override fun changeSubtitle(): String? {
        return null
    }

    override val audioTracksCount = 0
    override val spuTracksCount = 0
    override val videoTracksCount = 0

    override fun setOnCompletionListener(listener: android.media.MediaPlayer.OnCompletionListener?) {}
    override fun setOnErrorListener(l: android.media.MediaPlayer.OnErrorListener?) {}
    override fun changeOrientation(): Int {
        setSize()
        return 0
    }

    override fun end() {
        mTVController?.end()
        mVideoController?.end()
        mHandler?.removeMessages(SHOW_PROGRESS)
        releasePlayer()
    }


    /*************
     * Player
     */
    private fun createPlayer(media: String?) {
        Log.d("MyVLC", "CreatePlayer $media")
        //releasePlayer();
        try {
            /*if (media.toString().length() > 0) {
                Toast toast = Toast.makeText(getContext(), media.toString(), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }*/
            if (mMediaPlayer == null) {
                // Create LibVLC
                val options = ArrayList<String>()
                //options.add("--subsdec-encoding <encoding>");
                options.add("--aout=opensles")
                options.add("--audio-time-stretch") // time stretching
                //options.add("-vvv"); // verbosity
                libvlc = LibVLC(video_context, options)
                surfaceHolder!!.setKeepScreenOn(true)
            } else {
                releaseMedia()
            }

            // Create media player
            mMediaPlayer = MediaPlayer(libvlc)
            mMediaPlayer!!.setEventListener(mPlayerListener)

            // Set up video output
            val vout = mMediaPlayer!!.vlcVout
            vout.setVideoView(mSurface)
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this)
            vout.attachViews()
            val url = URL(media)
            val uri = Uri.parse(media)
            Log.d("MyVLC", "uri scheme " + uri.scheme)
            val m = Media(libvlc, uri)
            mMediaPlayer!!.media = m
            m.release()
            mMediaPlayer!!.play()
            if (mOnLayoutChangeListener == null) {
                mOnLayoutChangeListener = object : OnLayoutChangeListener {
                    private val mRunnable = Runnable { setSize() }
                    override fun onLayoutChange(
                        v: View, left: Int, top: Int, right: Int,
                        bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                    ) {
                        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                            mHandler!!.removeCallbacks(mRunnable)
                            mHandler!!.post(mRunnable)
                        }
                    }
                }
                mSurface!!.addOnLayoutChangeListener(mOnLayoutChangeListener)
            } else setSize()
        } catch (e: Exception) {
            Toast.makeText(context, "Error creating player! " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun releaseMedia() {
        mMediaPlayer!!.stop()
        val vout = mMediaPlayer!!.vlcVout
        vout.removeCallback(this)
        vout.detachViews()
        mMediaPlayer!!.release()
        mMediaPlayer = null
    }

    fun releasePlayer() {
        if (libvlc == null) return
        if (mOnLayoutChangeListener != null) {
            mSurface!!.removeOnLayoutChangeListener(mOnLayoutChangeListener)
            mOnLayoutChangeListener = null
        }
        releaseMedia()
        surfaceHolder = null
        libvlc!!.release()
        libvlc = null
        mVideoWidth = 0
        mVideoHeight = 0
    }

    fun setSize() {
        if (mMediaPlayer == null) return
        // get screen size
        var w = (this.context as Activity).window.decorView.width
        var h = (this.context as Activity).window.decorView.height
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        // sanity check
        if (w * h == 0) {
            Log.e(TAG, "Invalid surface size")
            return
        }
        mMediaPlayer!!.vlcVout.setWindowSize(w, h)
        if (mVideoWidth * mVideoHeight == 0) {
            val lp = mSurface!!.layoutParams
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */lp.width =
                ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            mSurface!!.layoutParams = lp
            if (pref_aspect_ratio_video == "default") {
                mMediaPlayer!!.aspectRatio = null
            } else if (pref_aspect_ratio_video == "fullscreen") {
                Log.d("test", "isPortrait + $isPortrait")
                mMediaPlayer!!.aspectRatio = if (isPortrait) "$h:$w" else "$w:$h"
            } else {
                mMediaPlayer!!.aspectRatio = pref_aspect_ratio_video
            }
            mMediaPlayer!!.scale = 0f
            //            changeMediaPlayerLayout(w, h);
            return
        }
        if (surfaceHolder == null || mSurface == null) return

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        if (w > h && isPortrait || w < h && !isPortrait) {
            val i = w
            w = h
            h = i
        }
        val videoAR = mVideoWidth.toFloat() / mVideoHeight.toFloat()
        val screenAR = w.toFloat() / h.toFloat()
        if (screenAR < videoAR) h = (w / videoAR).toInt() else w = (h * videoAR).toInt()

        // force surface buffer size
        surfaceHolder!!.setFixedSize(mVideoWidth, mVideoHeight)

        // set display size
        val lp = mSurface!!.layoutParams
        lp.width = w
        lp.height = h
        mSurface!!.layoutParams = lp
        mSurface!!.invalidate()
    }

    override fun onNewVideoLayout(
        vlcVout: IVLCVout,
        width: Int,
        height: Int,
        visibleWidth: Int,
        visibleHeight: Int,
        sarNum: Int,
        sarDen: Int
    ) {
        if (width * height == 0) return

        // store video size
        mVideoWidth = width
        mVideoHeight = height
        setSize()
    }

    override fun onSurfacesCreated(vlcVout: IVLCVout) {}
    override fun onSurfacesDestroyed(vlcVout: IVLCVout) {}
    private val mPlayerListener: MediaPlayer.EventListener = MyPlayerListener(this)

    private class MyPlayerListener(owner: VideoViewVLC) : MediaPlayer.EventListener {
        private val mOwner: WeakReference<VideoViewVLC>

        init {
            mOwner = WeakReference(owner)
        }

        override fun onEvent(event: MediaPlayer.Event) {
            val player = mOwner.get()
            when (event.type) {
                MediaPlayer.Event.EndReached -> {
                    Log.d(TAG, "MediaPlayerEndReached")
                    player!!.releasePlayer()
                }
                MediaPlayer.Event.Playing -> {}
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> {}
                else -> {}
            }
        }
    }

    private val prefs: Unit
        get() {
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            pref_aspect_ratio = prefs.getString("aspect_ratio", "default")
            pref_aspect_ratio_video = if (mMap!!["type"] == "video") {
                prefs.getString("aspect_ratio_video", "default")
            } else {
                prefs.getString("aspect_ratio_tv", "default")
            }
        }

    var mHandler: Handler? = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SURFACE_SIZE -> {}
                SHOW_PROGRESS -> setOverlayProgress()
            }
        }
    }

    init {
        init()
    }

    private fun setOverlayProgress() {
        if (mVideoController != null) {
            mVideoController!!.showProgress()
        }
        mHandler!!.removeMessages(SHOW_PROGRESS)
        val msg = mHandler!!.obtainMessage(SHOW_PROGRESS)
        mHandler!!.sendMessageDelayed(msg, 1000)
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

    companion object {
        const val TAG = "Krasview/VideoViewVLC"
        private const val SHOW_PROGRESS = 2
        private const val SURFACE_SIZE = 3
        private const val SURFACE_FROM_SETTINGS = 7
    }
}