package cat.dam.andy.music_compose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import cat.dam.andy.music_compose.model.MediaElement
import cat.dam.andy.music_compose.ui.screens.MainScreen
import cat.dam.andy.music_compose.ui.theme.Music_composeTheme
import cat.dam.andy.music_compose.viewmodel.MyViewModel


class MainActivity : ComponentActivity() {

    companion object {
        const val VERT_PERCENT_DETAILS_PORTRAIT = 0.2f
        const val VERT_PERCENT_PAGER_PORTRAIT = 0.5f
        const val PAGER_VISIBLE_SONG_NUMBER_PORTRAIT = 1.5f

        const val VERT_PERCENT_DETAILS_LANDSCAPE = 0.2f
        const val VERT_PERCENT_PAGER_LANDSCAPE = 0.4f
        const val PAGER_VISIBLE_SONG_NUMBER_LANDSCAPE = 4.0f

    }
    private val viewModel by viewModels<MyViewModel>()

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("playerState", viewModel.savePlayerState())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.restorePlayerState(savedInstanceState?.getBundle("playerState"))
        if (!viewModel.isPlayerInitialized) {
            viewModel.initializePlayer(this, getMediaItems(playList()))
        }
        setContent {
            Music_composeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp(this)
                }
            }
        }
    }

    @Composable
    fun MyApp(context: Context) {
        val playerState by viewModel.playerState.collectAsState()
        playerState?.let {
            MainScreen(context, playList())
        }
    }

    private fun getMediaItems(playList: List<MediaElement>): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        playList.forEach { element ->
            val mediaItem = MediaItem.Builder()
                .setUri("android.resource://" + this.packageName + "/" + element.music)
                .setMimeType(MimeTypes.AUDIO_MPEG)
                .build()
            mediaItems.add(mediaItem)
        }
        return mediaItems
    }

    private fun playList(): List<MediaElement> = listOf(
        MediaElement(
            name = "Tocatta and Fugue in D Minor",
            artist = "Bach",
            cover = R.drawable.bach,
            music = R.raw.bach_toccata_and_fugue_in_d_minor
        ),
        MediaElement(
            name = "Piano Sonatta 15 in D Major",
            artist = "Beethoven",
            cover = R.drawable.beethoven,
            music = R.raw.beethoven_piano_sonata_15_in_d_major
        ),
        MediaElement(
            name = "Largo",
            artist = "Handel",
            cover = R.drawable.handel,
            music = R.raw.handel_largo_xerxes
        ),
        MediaElement(
            name = "Piano Sonatta in B Major",
            artist = "Mozart",
            cover = R.drawable.mozart,
            music = R.raw.mozart_piano_sonata_in_b_flat_major
        ),
        MediaElement(
            name = "Concerto for Mandolin and Strings",
            artist = "Vivaldi",
            cover = R.drawable.vivaldi,
            music = R.raw.vivaldi_concerto_for_mandolin_and_strings
        ),
    )

}







