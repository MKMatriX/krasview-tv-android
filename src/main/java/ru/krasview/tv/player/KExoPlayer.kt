package ru.krasview.tv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.media3.common.*
import androidx.media3.common.C.*
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.krasview.tv.R


// todo: https://developer.android.com/codelabs/exoplayer-intro#0
@SuppressLint("ViewConstructor")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class KExoPlayer(context: Context?, var StyledPlayerView: PlayerView) : SurfaceView(context),
    VideoInterface, Player.Listener {
    private var mSurface: SurfaceView? = null
    var player: ExoPlayer? = null
    var dataSourceFactory: DefaultHttpDataSource.Factory? = null
    var trackSelector: DefaultTrackSelector? = null
    var mTVController: TVController? = null
    var mVideoController: VideoController? = null
    var mMap: Map<String, Any?>? = null
    var subs: List<String>? = emptyList()
    var is_playing = false
    var pref_aspect_ratio: String? = "default"
    var pref_aspect_ratio_video: String? = "default"
    var url: String = "http://kadu.ru"

    init {
        init()
    }

    @UnstableApi
    private fun init() {
        mSurface = this

        // 1. Create a default TrackSelector
        trackSelector = DefaultTrackSelector(context)
        trackSelector!!.setParameters(
            trackSelector!!.buildUponParameters()
                .setTunnelingEnabled(true)
                .setPreferredAudioLanguage("au1")
        )

        // 3. Create the player
        player = ExoPlayer.Builder(context).setTrackSelector(trackSelector!!).build()
        player?.playWhenReady = true

        StyledPlayerView.requestFocus()
        StyledPlayerView.player = player

        //DefaultHttpDataSourceFactory http
        dataSourceFactory = DefaultHttpDataSource.Factory()
        dataSourceFactory?.setDefaultRequestProperties(mapOf("Referer" to "https://krasview.ru"))
    }

    private fun setSize() {
        when (pref_aspect_ratio_video) {
            "default" -> {
                StyledPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            "fullscreen" -> {
                StyledPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            else -> {
                StyledPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }
        calcSize()
    }

    private fun calcSize() {
        // get screen size
        var w = (this.context as Activity).window.decorView.width
        var h = (this.context as Activity).window.decorView.height
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        // sanity check
        if (w * h == 0) {
            Log.e(TAG, "Invalid surface size")
            return
        }
        if (w > h && isPortrait || w < h && !isPortrait) {
            val d = w
            w = h
            h = d
        }
        var ar = 1.0
        val dar = w.toDouble() / h.toDouble()
        //double mult = dar;
        if (pref_aspect_ratio_video == "4:3") {
            ar = 4.0 / 3.0
            //ar = ar/mult*dar;
            if (dar < ar) h = (w / ar).toInt() else w = (h * ar).toInt()
        }
        if (pref_aspect_ratio_video == "16:9") {
            ar = 16.0 / 9.0
            //ar = ar/mult*dar;
            if (dar < ar) h = (w / ar).toInt() else w = (h * ar).toInt()
        }
        holder.setFixedSize(w, h)
        forceLayout()
        invalidate()
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
            Log.d(TAG, "aspect ratio: $pref_aspect_ratio_video")
        }

    private fun displaySpeedSelector(context: Context) {
        val speed: Float = player?.playbackParameters?.speed ?: 1.0f
        val id = ((speed - 0.5f) / 0.25).toInt()

        AlertDialog.Builder(context)
            .setTitle(R.string.select_speed)
            .setIcon(R.drawable.ic_baseline_speed_24)
            .setSingleChoiceItems(arrayOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x"), id, speedListener)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(R.string.normal_speed, normalSpeedListener)
            .create()
            .show()
    }

    private val normalSpeedListener: (DialogInterface, Int) -> Unit = { a: DialogInterface, id: Int ->
        player?.setPlaybackSpeed(1.0f)
    }

    private val speedListener: (DialogInterface, Int) -> Unit = { a: DialogInterface, id: Int ->
        val newSpeed = 0.5f + (0.25f * id)
        player?.setPlaybackSpeed(newSpeed)
    }

    private fun displayTrackSelector(context: Context, trackType: Int = TRACK_TYPE_AUDIO) {
        val title = when (trackType) {
            TRACK_TYPE_TEXT -> "Субтитры"
            TRACK_TYPE_AUDIO -> "Звуковая дорожка"
            else -> "Прочее"
        }

        TrackSelectionDialogBuilder(
            context,
            title,
            player!!,
            trackType)
            .setAllowAdaptiveSelections(false)
            .setShowDisableOption(false)
            .build()
            .show()
    }

    override fun onCues(cueGroup: CueGroup) {
        super.onCues(cueGroup)

        for (cue in cueGroup.cues) {// .setText(cues.get(0).text);
            Log.e(TAG, "${cue.text}")
        }
    }

//    private fun createDashMediaSource(
//        defaultDataSourceFactory: DefaultDataSource.Factory,
//        uri: Uri,
//    ): MediaSource {
//        val chunkSourceFactory = DefaultDashChunkSource.Factory(defaultDataSourceFactory)
//        val factory = DashMediaSource.Factory(chunkSourceFactory, defaultDataSourceFactory)
//        val mediaItem = MediaItem.fromUri(uri)
//        //        mediaSource.addEventListener(handler, mediaSourceEventListener)
//        return try {
//            factory.createMediaSource(mediaItem)
//        } catch (e: Exception) {
//            Log.e("KExoPlayer", e.localizedMessage ?: e.message ?: "unknown error", e)
//
//            ProgressiveMediaSource.Factory(dataSourceFactory!!)
//                .createMediaSource(MediaItem.fromUri(uri))
//        }
//    }

    override fun setVideoAndStart(address: String?, subtitles: List<String>?) {
        Log.d("ExoPlayer", "setVideoAndStart: $address")
        val uri = Uri.parse(address)

//        val mediaSourceFactory: MediaSource.Factory = if (address?.endsWith("mpd") == true) {
//
//            DashMediaSource.Factory(dataSourceFactory!!)
//        } else {
//            val extractorsFactory = DefaultExtractorsFactory()
//            if ((address?.indexOf("t.kraslan.ru") ?: 0) > 0) {
//                extractorsFactory.setTsExtractorFlags(
//                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
//                )
//            }
//            ProgressiveMediaSource.Factory(dataSourceFactory!!, extractorsFactory)
//        }

        if (player == null) return  // sanity check

       val items = subtitles?.map {

            MediaItem.SubtitleConfiguration.Builder(Uri.parse(it))
                .setMimeType(if (it.endsWith("srt")) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_SSA)
                .setLabel("Субтитр №${subtitles.indexOf(it) + 1}")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }?.plus(

           MediaItem.SubtitleConfiguration.Builder(Uri.parse(""))
               .setMimeType(MimeTypes.TEXT_SSA)
               .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
               .setLabel("Отключить")
               .build()
        ) ?: emptyList()


        val mediaItem = MediaItem.Builder().setUri(uri).setSubtitleConfigurations(items).build()

//        val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
//        val mediaSource = if (items.isNotEmpty()) {
//            val subtitleSource: MediaSource = SingleSampleMediaSource
//                .Factory(dataSourceFactory!!)
//                .createMediaSource(items.first(), C.TIME_UNSET)
//            MergingMediaSource(videoSource, subtitleSource)
//        } else
//            videoSource

        player?.setMediaItem(mediaItem)

        StyledPlayerView.subtitleView!!.isVisible = items.isNotEmpty()
        StyledPlayerView.setShowSubtitleButton(items.isNotEmpty())
        StyledPlayerView.subtitleView!!.alpha = 0.8f

        player?.prepare()

        player!!.playWhenReady = true
        Log.d(TAG, "after play")
        player!!.addListener(this)

        // todo subtitles: https://github.com/google/ExoPlayer/issues/1183
    }

//    fun getId(view: View): String? {
//        return if (view.id == NO_ID) "no-id" else view.resources.getResourceName(view.id)
//    }

    override fun stop() {
        if (player != null) player!!.playWhenReady = false
    }

    override fun pause() {
        if (player != null) player!!.playWhenReady = false
    }

    override fun play() {
        if (player != null) player!!.playWhenReady = true
        Log.d(TAG, "play")
    }


    override val isPlaying_: Boolean
        get() = if (player == null) false else is_playing

    override fun setTVController(tc: TVController?) {
        mTVController = tc
        mTVController!!.setVideo(this)
    }

    override fun setVideoController(vc: VideoController?) {
        mVideoController = vc
        mVideoController!!.setVideo(this)
    }

    override fun setMap(map: Map<String, Any?>?) {
        Log.d(TAG, "setMap")
        mMap = map
        mTVController?.setMap(mMap)
        mVideoController?.setMap(mMap)
        prefs
    }

    override fun showOverlay(): Boolean {
        mVideoController?.showProgress()
        return true
    }

    override fun hideOverlay(): Boolean {
        return false
    }

    override val progress: Int
        get() = player?.currentPosition?.toInt() ?: 0
    override val leight: Int
        get() = player?.duration?.toInt() ?: 0
    override var time: Int
        get() = player?.currentPosition?.toInt() ?: 0
        set(time) {

            var pos: Int
            if (time >= 100 || time < 0) {
                pos = player!!.currentPosition.toInt() + time
                if (pos < 0) pos = 0
            } else {
                pos = player!!.duration.toInt() * time / 100
            }
            if (player != null) player!!.seekTo(pos.toLong())
        }


    override fun setPosition(time: Int) {
        if (player != null) player!!.seekTo(time.toLong())
    }

    override fun changeSizeMode(): Int {
        setSize()
        return 0
    }

    override fun changeSubtitle(): String {
        showBottomSheetDialog()
        return ""
    }
    override fun changeAudio(): String {
        showBottomSheetDialog()
        return ""
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val audio: View? = bottomSheetDialog.findViewById(R.id.action_audio)
        val video: View? = bottomSheetDialog.findViewById(R.id.action_video)
        val subtitles: View? = bottomSheetDialog.findViewById(R.id.action_subtitles)
        val speed: View? = bottomSheetDialog.findViewById(R.id.action_speed)

        audio?.setOnClickListener { displayTrackSelector(context as VideoActivity, TRACK_TYPE_AUDIO); bottomSheetDialog.dismiss() }
        video?.setOnClickListener { displayTrackSelector(context as VideoActivity, TRACK_TYPE_VIDEO); bottomSheetDialog.dismiss() }
        subtitles?.setOnClickListener { displayTrackSelector(context as VideoActivity, TRACK_TYPE_TEXT); bottomSheetDialog.dismiss() }
        speed?.setOnClickListener { displaySpeedSelector(context as VideoActivity); bottomSheetDialog.dismiss() }

        audio?.isGone = trackCount(TRACK_TYPE_AUDIO) < 2
        video?.isGone = trackCount(TRACK_TYPE_VIDEO) < 2
        subtitles?.isGone = trackCount(TRACK_TYPE_TEXT) < 2

        if (audio?.isGone == false ||
            video?.isGone == false ||
            subtitles?.isGone == false ||
            speed?.isGone == false)
             bottomSheetDialog.show()
    }


    override val audioTracksCount: Int
        get() {
            var tracks = 0
            if (trackSelector == null || player == null) return tracks
            for (i in 0 until player!!.currentTrackGroups.length) {
                val format = player!!.currentTrackGroups[i].getFormat(0).sampleMimeType
                if (format!!.contains("audio")) {
                    tracks++
                } else {
                    Log.e("track", format ?: "NULL")
                }
            }
            return tracks
        }

    override val spuTracksCount: Int
        get() {
            var tracks = 0
            if (trackSelector == null || player == null) return tracks
            for (i in 0 until player!!.currentTrackGroups.length) {
                val format = player!!.currentTrackGroups[i].getFormat(0).sampleMimeType
                if (format!!.contains(MimeTypes.TEXT_SSA) || format.contains(MimeTypes.APPLICATION_SUBRIP)) {
                    tracks++
                } else {
                    Log.e("track", format ?: "NULL")
                }
            }
            return tracks
        }

    override val videoTracksCount: Int
        get() = trackCount(TRACK_TYPE_VIDEO)

    override fun setOnCompletionListener(listener: OnCompletionListener?) {
    }

    override fun setOnErrorListener(l: MediaPlayer.OnErrorListener?) {
    }


    override fun onIsPlayingChanged(isPlaying: Boolean) {
        is_playing = isPlaying
    }

    override fun changeOrientation(): Int {
        setSize()
        return 0
    }

    override fun end() {
        Log.d(TAG, "end")

        player?.stop()
        player?.release()
        player = null

        mTVController?.end()
        mVideoController?.end()
    }


    // ExoPlayer.EventListener implementation
    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "isLoading: $isLoading")
        //Log.d(TAG, "duration " + player.getDuration());
        if (isLoading) {
            setSize()
            mVideoController?.showProgress()
        }
        // Do nothing.
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        Log.d(TAG, "playbackState: $playbackState")
        //Log.d(TAG, "duration " + player.getDuration());
        if (playbackState == 3 && player != null) {
            if (mVideoController != null) {
                Log.i("Debug", "Проверить число треков")
                mVideoController!!.checkTrack()
            }
        }
        if (playbackState == 4 && player != null) {
            if (mVideoController != null) mVideoController!!.next()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val errorString: String = error.localizedMessage ?: error.message ?: "Unknown error"
        Toast.makeText(context, errorString, Toast.LENGTH_LONG).show()
    }

    //    override fun onPositionDiscontinuity(reason: Int) {}
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

        private fun trackCount(type: Int): Int {
            return player?.currentTracks?.groups?.count { it.type == type } ?: 0
        }


    override fun onRepeatModeChanged(repeatMode: Int) {}
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        //Toast.makeText(getContext(), "Нажата клавиша: " + event.getKeyCode(), Toast.LENGTH_LONG).show();
        if (event.action == KeyEvent.ACTION_DOWN && event.isLongPress && event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showBottomSheetDialog()
//            displayTrackSelector(context as VideoActivity, TRACK_TYPE_TEXT)
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU
        ) {
            showBottomSheetDialog()
//            showPopupMenu(StyledPlayerView.rootView)
//            displayTrackSelector(context as VideoActivity, TRACK_TYPE_AUDIO)
            return true
        }
        if (mTVController != null) {
            return mTVController!!.dispatchKeyEvent(event) || StyledPlayerView.dispatchKeyEvent(
                event
            )
        }
        return if (mVideoController != null) {
            mVideoController!!.dispatchKeyEvent(event) || StyledPlayerView.dispatchKeyEvent(event)
        } else true
    }

    companion object {
        const val TAG = "Krasview/KExoPlayer"
    }
}