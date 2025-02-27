package cat.dam.andy.music_compose.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer

import cat.dam.andy.music_compose.R
import cat.dam.andy.music_compose.model.MediaElement
import cat.dam.andy.music_compose.MainActivity.Companion.PAGER_VISIBLE_SONG_NUMBER_LANDSCAPE
import cat.dam.andy.music_compose.MainActivity.Companion.PAGER_VISIBLE_SONG_NUMBER_PORTRAIT
import cat.dam.andy.music_compose.MainActivity.Companion.VERT_PERCENT_DETAILS_LANDSCAPE
import cat.dam.andy.music_compose.MainActivity.Companion.VERT_PERCENT_DETAILS_PORTRAIT
import cat.dam.andy.music_compose.MainActivity.Companion.VERT_PERCENT_PAGER_LANDSCAPE
import cat.dam.andy.music_compose.MainActivity.Companion.VERT_PERCENT_PAGER_PORTRAIT

import cat.dam.andy.music_compose.viewmodel.MyViewModel
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt


var itemNumber by mutableIntStateOf(0)

@OptIn(ExperimentalFoundationApi::class)
lateinit var pagerState: PagerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(context: Context, playList: List<MediaElement>) {
    val viewModel: MyViewModel = viewModel(MyViewModel::class.java)
    val player = viewModel.playerState.value ?: return
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    itemNumber = playList.size
    pagerState = rememberPagerState(pageCount = { playList.size })

    // Si hi ha un canvi de cançó, s'ha de posar el pager a la cançó que s'ha seleccionat
    LaunchedEffect(viewModel.currentItemIndex) {
        pagerState.animateScrollToPage(viewModel.currentItemIndex)
    }

    // Si hi ha un canvi de posició de la cançó, s'ha d'actualitzar la durada i el temps restant
    LaunchedEffect(viewModel.currentItemPosition) {
        viewModel.currentItemDuration = player.duration
        viewModel.currentItemRemainingTime =
            viewModel.currentItemDuration - viewModel.currentItemPosition
    }

    // Si hi ha un error, s'ha de mostrar un missatge
    LaunchedEffect(viewModel.errorMessage) {
        if (viewModel.errorMessage.value != null) {
            val mediaItemNumber = viewModel.currentItemIndex
            val mediaElement = playList[mediaItemNumber]
            val toastMessage = "$viewModel.errorMessage: ${mediaElement.name})"
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            // clear the error message
            viewModel.clearErrorMessage()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SongDetails(
            isLandscape,
            playList[viewModel.currentItemIndex],
            if (isLandscape) VERT_PERCENT_DETAILS_LANDSCAPE else VERT_PERCENT_DETAILS_PORTRAIT
        )
        val pagerVisibleSongNumber: Float = if (isLandscape) {
            PAGER_VISIBLE_SONG_NUMBER_LANDSCAPE
        } else {
            PAGER_VISIBLE_SONG_NUMBER_PORTRAIT
        }
        val pagerIndicatorNumber =
            ceil(itemNumber.toFloat() / pagerVisibleSongNumber.toInt()).roundToInt()

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isLandscape) VERT_PERCENT_PAGER_LANDSCAPE else VERT_PERCENT_PAGER_PORTRAIT),
            state = pagerState,
            // Repartim l'espai de la pantalla entre les cançons
            pageSize = PageSize.Fixed((configuration.screenWidthDp / (pagerVisibleSongNumber)).dp),
            contentPadding = PaddingValues(horizontal = 50.dp)
        ) { page ->
            AlbumCover(
                isSongPlaying = viewModel.isPlaying,
                currentCover = page,
                painter = painterResource(id = playList[page].cover),
                viewModel = viewModel,
                onSongClick = {
                    if (viewModel.currentItemIndex != page) {
                        viewModel.playItemIndex(page)
                    } else {
                        togglePlayPause(player, viewModel.currentItemIndex, viewModel)
                    }
                })
        }
        HorizontalPagerIndicator(
            pageCount = pagerIndicatorNumber,
            currentPage = pagerState.currentPage,
            targetPage = pagerState.targetPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction
        )
        PlayerControls(player, viewModel.currentItemIndex, viewModel)
    }
}

fun togglePlayPause(player: ExoPlayer, itemIndex: Int, viewModel: MyViewModel) {
    viewModel.isPlaying = !viewModel.isPlaying
    if (viewModel.isPlaying) {
        playItemIndex(player, itemIndex, viewModel)
    } else {
        player.pause()
    }
}

fun playItemIndex(player: ExoPlayer, itemIndex: Int, viewModel: MyViewModel) {
    if (viewModel.currentItemIndex != itemIndex) {
        // Si canvia de cançó, s'ha de posar la posició de la cançó a 0
        viewModel.currentItemIndex = itemIndex
        viewModel.currentItemPosition = 0
        viewModel.coverRotation = 0f
        player.seekTo(itemIndex, 0)  // Posiciona la cançó al principi
    }
    player.play()
    viewModel.isPlaying = true
    CoroutineScope(Dispatchers.Main).launch {
        while (viewModel.isPlaying) {
            delay(1000) // Espera cada segon
            viewModel.currentItemPosition = player.currentPosition
            viewModel.currentItemRemainingTime =
                viewModel.currentItemDuration - viewModel.currentItemPosition
        }
    }
}


@Composable
private fun HorizontalPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    targetPage: Int,
    currentPageOffsetFraction: Float,
    modifier: Modifier = Modifier,
    indicatorColor: Color = Color.Black,
    unselectedIndicatorSize: Dp = 10.dp,
    selectedIndicatorSize: Dp = 12.dp,
    indicatorCornerRadius: Dp = 2.dp,
    indicatorPadding: Dp = 2.dp
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
            .height(selectedIndicatorSize + indicatorPadding * 2)
    ) {

        // draw an indicator for each page
        repeat(pageCount) { page ->
            // calculate color and size of the indicator
            val (color, size) =
                if (currentPage == page || targetPage == page) {
                    // calculate page offset
                    val pageOffset =
                        ((currentPage - page) + currentPageOffsetFraction).absoluteValue
                    // calculate offset percentage between 0.0 and 1.0
                    val offsetPercentage = 1f - pageOffset.coerceIn(0f, 1f)
                    val size =
                        unselectedIndicatorSize + ((selectedIndicatorSize - unselectedIndicatorSize) * offsetPercentage)
                    indicatorColor.copy(
                        alpha = offsetPercentage
                    ) to size
                } else {
                    indicatorColor.copy(alpha = 0.5f) to unselectedIndicatorSize
                }

            Box(
                modifier = Modifier
                    .padding(
                        // apply horizontal padding, so that each indicator is same width
                        horizontal = ((selectedIndicatorSize + indicatorPadding * 2) - size) / 2,
                        vertical = size / 4
                    )
                    .clip(RoundedCornerShape(indicatorCornerRadius))
                    .background(color)
                    .width(size)
                    .height(size / 2)
            )
        }
    }
}

@Composable
fun SongDetails(
    isLandscape: Boolean,
    song: MediaElement,
    heightPercent: Float = VERT_PERCENT_DETAILS_PORTRAIT
) {
    Column(
        modifier = Modifier.fillMaxHeight(heightPercent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Aquesta animació fa que quan canvia, el text de la cançó i l'artista es mostri amb un efecte
        // de fade in, scale in, fade out i scale out
        val animationTime = 1000 // Temps d'animació en mil·lisegons
        val animationSpec: FiniteAnimationSpec<Float> = tween(durationMillis = animationTime)
        AnimatedContent(targetState = song.name, transitionSpec = {
            (scaleIn(animationSpec) + fadeIn(animationSpec)) togetherWith
                    (scaleOut(animationSpec) + fadeOut(animationSpec))
        }, label = "") { text ->
            Text(
                text = text,
                fontSize = if (isLandscape) {
                    20.sp
                } else {
                    30.sp
                },
                color = Color.Black,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedContent(targetState = song.artist, transitionSpec = {
            (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
        }, label = "") { text ->
            Text(
                text = text,
                fontSize = if (isLandscape) {
                    15.sp
                } else {
                    25.sp
                },
                color = Color.Black,
                style = TextStyle(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlayerControls(
    player: ExoPlayer,
    currentItemIndex: Int,
    viewModel: MyViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            value = viewModel.currentItemPosition.toFloat(),
            onValueChange = {
                viewModel.currentItemPosition = it.toLong()
                player.seekTo(viewModel.currentItemPosition)
                viewModel.currentItemDuration = player.duration
                viewModel.currentItemRemainingTime =
                    viewModel.currentItemDuration - viewModel.currentItemPosition
            },
            onValueChangeFinished = {
                player.seekTo(viewModel.currentItemPosition)
                viewModel.currentItemDuration = player.duration
                viewModel.currentItemRemainingTime =
                    viewModel.currentItemDuration - viewModel.currentItemPosition
            },
            valueRange = 0f..(viewModel.currentItemDuration.toFloat().coerceAtLeast(0f)),
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.DarkGray,
                inactiveTrackColor = Color.Gray,
            )
        )
        Text(
            modifier = Modifier.weight(0.5f),
            text = "${viewModel.currentItemPosition.convertToTime()} / ${viewModel.currentItemRemainingTime.convertToTime()}",
            color = Color.Black,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = R.drawable.ic_volume_minus,
                size = 50.dp,
                color = if (player.volume > 0f) Color.Black else Color.Gray,
            ) { player.volume = (player.volume - 0.1f).coerceAtLeast(0f) }
            ControlButton(
                icon = R.drawable.ic_volume_plus,
                size = 50.dp,
                color = if (player.volume < 1f) Color.Black else Color.Gray,
            ) { player.volume = (player.volume + 0.1f).coerceAtMost(1f) }
            Slider(
                modifier = Modifier.weight(0.5f),
                value = player.volume,
                onValueChange = { player.volume = it },
                valueRange = 0f..1f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Black,
                    activeTrackColor = Color.DarkGray,
                    inactiveTrackColor = Color.Gray,
                ),
            )
            ControlButton(
                icon = if (player.volume > 0) R.drawable.ic_volume_high else R.drawable.ic_volume_off,
                size = 50.dp,
                color = Color.Black
            ) { if (player.volume > 0) player.volume = 0f else player.volume = 1f }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = R.drawable.ic_skip_previous_circle_outline,
                size = 80.dp,
                color = if (player.hasPreviousMediaItem()) Color.Black else Color.Gray,
            ) { if (currentItemIndex > 0) playItemIndex(player, currentItemIndex - 1, viewModel) }
            ControlButton(
                icon = if (viewModel.isPlaying) R.drawable.ic_pause_circle_outline else R.drawable.ic_play_circle_outline,
                size = 80.dp,
                color = Color.Black
            ) { togglePlayPause(player, currentItemIndex, viewModel) }
            ControlButton(
                icon = R.drawable.ic_skip_next_circle_outline,
                size = 80.dp,
                color = if (player.hasNextMediaItem()) Color.Black else Color.Gray,
            ) {
                if (currentItemIndex < itemNumber - 1) playItemIndex(
                    player,
                    currentItemIndex + 1,
                    viewModel
                )
            }
        }
    }
}


@Composable
fun ControlButton(icon: Int, size: Dp, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(size),
            painter = painterResource(id = icon),
            tint = color,
            contentDescription = null
        )
    }
}

@Composable
fun AlbumCover(
    isSongPlaying: Boolean,
    currentCover: Int,
    painter: Painter,
    viewModel: MyViewModel,
    onSongClick: () -> Unit
) {


    LaunchedEffect(isSongPlaying) {
        if (isSongPlaying) {
            while (true) {
                delay(16)
                viewModel.coverRotation = (viewModel.coverRotation + 2f) % 360
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            modifier = Modifier
                .rotate(if (viewModel.currentItemIndex == currentCover) viewModel.coverRotation else 0f)
                .aspectRatio(1.0f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .clickable {
                    onSongClick()
                },
            painter = painter,
            contentDescription = "Song cover"
        )
    }
}

private fun Long.convertToTime(): String {
    val positiveValue = abs(this)
    val seconds = positiveValue / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return String.format(Locale.getDefault(),"%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60)
}



