package ru.krasview.tv.player

import android.media.MediaPlayer

interface VideoInterface {
    fun stop()
    fun pause()
    fun play()
    fun setTVController(tc: TVController?)
    fun setVideoController(vc: VideoController?)
    fun setMap(map: Map<String, Any?>?)
    val isPlaying_: Boolean
    fun showOverlay(): Boolean
    fun hideOverlay(): Boolean
    val progress: Int
    val leight: Int
    var time: Int
    fun setPosition(time: Int)
    fun changeSizeMode(): Int
    fun changeAudio(): String?
    fun changeSubtitle(): String?
    val audioTracksCount: Int
    val spuTracksCount: Int
    val videoTracksCount: Int
    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?)
    fun setOnErrorListener(l: MediaPlayer.OnErrorListener?)
    fun changeOrientation(): Int
    fun end()
    fun setVideoAndStart(address: String?, subtitles: List<String>?)
}