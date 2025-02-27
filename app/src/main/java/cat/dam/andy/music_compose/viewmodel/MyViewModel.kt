package cat.dam.andy.music_compose.viewmodel

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {

    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState
    val errorMessage = MutableStateFlow<String?>(null)

    var currentItemIndex by mutableIntStateOf(0)
    var currentItemPosition by mutableLongStateOf(0L)
    var currentItemDuration by mutableLongStateOf(0L)
    var currentItemRemainingTime by mutableLongStateOf(0L)
    var coverRotation by mutableFloatStateOf(0f)
    var isPlaying by mutableStateOf(false)
    var isPlayerInitialized by mutableStateOf(false)

    fun initializePlayer(context: Context, mediaList: List<MediaItem>) {
        if (!isPlayerInitialized) {
            val player = ExoPlayer.Builder(context).build()
            player.setMediaItems(mediaList)
            player.addListener(playerListener())
            player.prepare()
            _playerState.value = player
            isPlayerInitialized = true
        }
    }

    fun clearErrorMessage() {
        errorMessage.value = null
    }

    fun pausePlayer() {
        _playerState.value?.pause()
        isPlaying = false
    }

    private fun releasePlayer() {
        _playerState.value?.release()
        _playerState.value = null
    }

    override fun onCleared() {
        releasePlayer()
    }

    fun playItemIndex(itemIndex: Int) {
        val player = _playerState.value ?: return
        if (currentItemIndex != itemIndex) {
            currentItemIndex = itemIndex
            currentItemPosition = 0
            coverRotation = 0f
            player.seekTo(currentItemIndex, 0)
        }
        player.play()
        isPlaying = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isPlaying) {
                kotlinx.coroutines.delay(1000)
                currentItemPosition = player.currentPosition
            }
        }
    }

    private fun playerListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d("ExoPlayer:", "changed state to $stateString")
            if (playbackState == Player.STATE_ENDED) {
                pausePlayer()
            }
        }

        @OptIn(UnstableApi::class) override fun onPlayerError(error: PlaybackException) {
            if (error.cause is androidx.media3.common.ParserException) {
                val errorMessage = "Error: Incorrect MIME type or malformed media file."
                Log.e("ExoPlayer:", errorMessage)
                this@MyViewModel.errorMessage.value = errorMessage
            } else {
                Log.e("ExoPlayer:", "Error: ${error.message}")
                errorMessage.value = "Error: ${error.message}"
            }
        }
    }

    fun savePlayerState(): Bundle {
        val bundle = Bundle()
        bundle.putInt("currentItemIndex", currentItemIndex)
        bundle.putLong("currentItemPosition", currentItemPosition)
        bundle.putBoolean("isPlaying", isPlaying)
        bundle.putBoolean("isPlayerInitialized", isPlayerInitialized)
        return bundle
    }

    fun restorePlayerState(bundle: Bundle?) {
        bundle?.let {
            currentItemIndex = it.getInt("currentItemIndex")
            currentItemPosition = it.getLong("currentItemPosition")
            isPlaying = it.getBoolean("isPlaying")
            isPlayerInitialized = it.getBoolean("isPlayerInitialized", false)
        }
    }

}