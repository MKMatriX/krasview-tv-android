package ru.krasview.tv.player

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.media3.ui.PlayerView
import com.example.kvlib.R
import org.videolan1.vlc.Util
import ru.krasview.kvlib.indep.ListAccount

class VideoActivity : Activity() {
    private var mFrame: RelativeLayout? = null
    private var mVideoSurface: VideoInterface? = null
    private var mOverlayFrame: RelativeLayout? = null
    private var simpleExoPlayerView: PlayerView? = null
    var current = 0
    private var mLocation: String? = null
    private var mType: String? = null
    private var mTitle_value: CharSequence? = null
    private var mIcon_value: Bitmap? = null
    private var mInfo: TextView? = null
    private var mTitle: TextView? = null
    private var mIcon: ImageView? = null
    private val mHandler: Handler = VideoPlayerHandler(this)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(
            ru.krasview.tv.R.anim.anim_enter_right,
            ru.krasview.tv.R.anim.anim_leave_left
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kv_b_activity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mFrame = findViewById<View>(R.id.player_surface_frame) as RelativeLayout
        mOverlayFrame = findViewById<View>(R.id.overlay_frame) as RelativeLayout
        mInfo = findViewById<View>(R.id.player_overlay_info) as TextView
        mTitle = findViewById<View>(R.id.player_overlay_title) as TextView
        mIcon = findViewById<View>(R.id.player_overlay_icon) as ImageView
        simpleExoPlayerView = findViewById<View>(R.id.player_view) as PlayerView
        initLayout()
    }

    private fun initLayout() {
        val map: Map<String, Any?>?
        current = if (intent.action != null && intent.action === ACTION_VIDEO_LIST) {
            intent.getIntExtra("index", 0)
        } else {
            0
        }
        if (ListAccount.adapterForActivity == null) {
            map = null
            return
        } else {
            map = ListAccount.adapterForActivity.getItem(current) as Map<String, Any?>
        }
        mType = map["type"] as String?
        prefs
        if (mType == "video") {
            pref_video_player = pref_video_player_serial
        } else if (mType == "channel" || mType == "tv_record") {
            pref_video_player = pref_video_player_tv
        }

        if (mVideoSurface == null) {
            mVideoSurface = if (pref_video_player == "VLC") {
                VideoViewVLC(this)
            } else {
                KExoPlayer(this, simpleExoPlayerView!!)
                //mVideoSurface = new AVideoView(this);
            }
//            mFrame?.addView(mVideoSurface as SurfaceView)
        }

        when (mType) {
            null -> return
            "channel" -> {
                val v = TVController(this)
                (mVideoSurface as VideoInterface).setTVController(v)
                mOverlayFrame!!.addView(v)
            }
            else -> {
                val v = VideoController(this)
                (mVideoSurface as VideoInterface).setVideoController(v)
                mOverlayFrame!!.addView(v)
            }
        }
        (mVideoSurface as VideoInterface).setOnCompletionListener {
            showOverlay(false)
            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
            dispatchKeyEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        if (intent.action != null && intent.action === ACTION_VIDEO_LIST) {
            current = intent.getIntExtra("index", 0)
        }
        prefs
        when (pref_orientation) {
            "default" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            "album" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            "book" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        if (intent != null) {
            start(intent.getBooleanExtra("request_time", false))
        }
    }

    override fun onStart() {
        super.onStart()
    }

    var pref_video_player_serial: String? = null
    var pref_video_player_tv: String? = null
    var pref_video_player: String? = null
    var pref_orientation: String? = null
    private val prefs: Unit
        get() {
            Companion.prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            pref_video_player_serial = Companion.prefs?.getString("video_player_serial", "std")
            pref_video_player_tv = Companion.prefs?.getString("video_player_tv", "std")
            pref_orientation = Companion.prefs?.getString("orientation", "default")
        }

    private fun start(request_time: Boolean) {
        hideInfo()
        val map = ListAccount.adapterForActivity.getItem(current) as MutableMap<String, Any?>
        //map.put("request_time", request_time);
        map["rt"] = request_time
        mLocation = map["uri"] as String?
        mTitle_value = map["name"] as CharSequence?
        mIcon_value = if (map["image"] != null && map["image"]?.javaClass == Bitmap::class.java) {
            map["image"] as Bitmap?
        } else {
            null
        }
        if (mLocation == null && mType == null) {
            return
        }
        mTitle!!.text = mTitle_value
        if (mIcon_value != null) {
            mIcon!!.setImageBitmap(mIcon_value)
            mIcon!!.visibility = View.VISIBLE
        }
        // Log.i("Debug", "VideoActivity mVideoSurface = " + mVideoSurface);
        (mVideoSurface as? VideoInterface)?.setMap(map)
        //((VideoInterface)mVideoSurface).setVideoAndStart(mLocation);
        showOverlay()
    }

    private class VideoPlayerHandler(var mActivity: VideoActivity) :
        Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                FADE_OUT -> {
                    mActivity.hideOverlay()
                    mActivity.hideInfo()
                }
                FADE_OUT_INFO -> mActivity.hideInfo()
            }
        }
    }

    public override fun onStop() {
        super.onStop()
        mVideoSurface?.stop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.d("Debug", "Клавиша нажата VideoActivity")
        showOverlay()

        /*if(event.getKeyCode()== KeyEvent.KEYCODE_DPAD_CENTER) {
			return true;
		}*/if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    onNext(true)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    onPrev()
                    return true
                }
            }
        }
        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    onBackPressed()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> if (mType == "channel") {
                    onBackPressed()
                    return true
                }
            }
        }
        return (mVideoSurface as? SurfaceView)?.dispatchKeyEvent(event) ?: false
        //return super.dispatchKeyEvent(event);
    }

    fun hideOverlay() {
        mOverlayFrame!!.visibility = View.INVISIBLE
        dimStatusBar(true)
    }

    private fun dimStatusBar(dim: Boolean) {
        //false - show
        if (!Util.isHoneycombOrLater() || !Util.hasNavBar()) return
        var layout = 0
        if (!Util.hasCombBar() && Util.isJellyBeanOrLater()) // layout = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            layout = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (mVideoSurface != null) {
            (mVideoSurface as? SurfaceView)?.systemUiVisibility =
                (if (dim) (if (Util.hasCombBar()) View.SYSTEM_UI_FLAG_LOW_PROFILE else View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) else View.SYSTEM_UI_FLAG_VISIBLE) or layout
        }
    }

    @JvmOverloads
    fun showOverlay(hide: Boolean = true) {
        mOverlayFrame!!.visibility = View.VISIBLE
        if (mVideoSurface != null) {
            (mVideoSurface as VideoInterface).showOverlay()
        }
        dimStatusBar(false)
        mHandler.removeMessages(FADE_OUT)
        if (hide) {
            mHandler.sendEmptyMessageDelayed(FADE_OUT, 3000)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        showOverlay()
        return super.dispatchTouchEvent(ev)
    }

    public override fun onDestroy() {
        super.onDestroy()
        (mVideoSurface as VideoInterface?)!!.end()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("index", current)
        setResult(RESULT_OK, intent)
        super.onBackPressed()
        overridePendingTransition(
            ru.krasview.tv.R.anim.anim_enter_left,
            ru.krasview.tv.R.anim.anim_leave_right
        )
        (mVideoSurface as VideoInterface?)!!.end()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (mVideoSurface as VideoInterface?)!!.changeOrientation()
    }

    fun showInfo(msg: String?, i: Int) {
        mInfo!!.visibility = View.VISIBLE
        mInfo!!.text = msg
        mHandler.removeMessages(FADE_OUT_INFO)
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, i.toLong())
    }

    fun showInfo(text: CharSequence?) {
        mInfo!!.visibility = View.VISIBLE
        mInfo!!.text = text
        mHandler.removeMessages(FADE_OUT_INFO)
        mHandler.removeMessages(FADE_OUT)
    }

    fun onNext(loop: Boolean) {
        if (current + 1 >= ListAccount.adapterForActivity.count) {
            current = if (loop) 0 else return
        } else current++
        start(false)
    }

    fun onPrev() {
        current--
        if (current < 0) {
            current = ListAccount.adapterForActivity.count - 1
        }
        start(false)
    }

    fun hideInfo() {
        mInfo!!.visibility = View.GONE
    }

    companion object {
        const val ACTION_VIDEO_LIST = "ru.krasview.tv.PLAY_VIDEO_LIST"
        private const val FADE_OUT = 1
        private const val FADE_OUT_INFO = 4
        var prefs: SharedPreferences? = null
    }
}