@file:OptIn(ExperimentalTvMaterial3Api::class)

package tv.wtv.app.ui.player

import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.wtv.app.data.model.ChatMessage

@Composable
fun PlayerScreen(
    channelName: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val playerFocus = remember { FocusRequester() }
    val isQualityPickerVisible = (uiState as? PlayerUiState.Playing)?.isQualityPickerVisible ?: false
    LaunchedEffect(isQualityPickerVisible) {
        if (!isQualityPickerVisible) runCatching { playerFocus.requestFocus() }
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(channelName) {
        viewModel.load(channelName)
    }

    val playbackUrl = (uiState as? PlayerUiState.Playing)?.streamInfo?.playbackUrl
    LaunchedEffect(playbackUrl) {
        if (playbackUrl != null) {
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(playbackUrl)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            )
            player.prepare()
        }
    }

    var tracks by remember { mutableStateOf<Tracks?>(null) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
                val qualities = buildList {
                    add("Auto")
                    for (group in newTracks.groups) {
                        if (group.type == C.TRACK_TYPE_VIDEO && group.isSupported) {
                            for (i in 0 until group.length) {
                                val f = group.getTrackFormat(i)
                                if (f.height > 0) {
                                    val fps = f.frameRate.roundToInt()
                                    add(if (fps > 0) "${f.height}p$fps" else "${f.height}p")
                                }
                            }
                        }
                    }
                }.distinct()
                viewModel.setAvailableQualities(qualities)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val selectedQuality = (uiState as? PlayerUiState.Playing)?.selectedQuality
    LaunchedEffect(selectedQuality, tracks) {
        val t = tracks ?: return@LaunchedEffect
        if (selectedQuality == null || selectedQuality == "Auto") {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
            return@LaunchedEffect
        }
        outer@ for (group in t.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO && group.isSupported) {
                for (i in 0 until group.length) {
                    val f = group.getTrackFormat(i)
                    val fps = f.frameRate.roundToInt()
                    val label = if (fps > 0) "${f.height}p$fps" else "${f.height}p"
                    if (label == selectedQuality) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                            .build()
                        break@outer
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    viewModel.showMetadata()
                    true
                } else false
            }
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Text(
                    text = "Loading…",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is PlayerUiState.Playing -> {
                if (state.isQualityPickerVisible) {
                    BackHandler { viewModel.toggleQualityPicker() }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(if (state.isChatVisible) 0.7f else 1f)
                            .fillMaxHeight()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (state.isMetadataVisible) {
                            StreamInfoBar(
                                state = state,
                                onQualityClick = viewModel::toggleQualityPicker,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                            )
                        }

                        if (state.isQualityPickerVisible) {
                            QualityPickerOverlay(
                                qualities = state.availableQualities,
                                selected = state.selectedQuality,
                                onSelect = viewModel::selectQuality,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 24.dp),
                            )
                        }
                    }

                    if (state.isChatVisible) {
                        ChatPanel(
                            messages = state.chatMessages,
                            modifier = Modifier
                                .weight(0.3f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }

            is PlayerUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            }
        }
    }
}

@Composable
private fun StreamInfoBar(
    state: PlayerUiState.Playing,
    onQualityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uptimeText by remember { mutableStateOf("") }
    LaunchedEffect(state.streamInfo.startedAtMs) {
        while (true) {
            val elapsed = System.currentTimeMillis() - state.streamInfo.startedAtMs
            val totalSeconds = elapsed / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            uptimeText = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
                         else "%d:%02d".format(minutes, seconds)
            delay(1_000)
        }
    }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = state.streamInfo.channelName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        if (state.streamInfo.title.isNotEmpty()) {
            Text(
                text = state.streamInfo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "● LIVE $uptimeText",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
            )
            Button(onClick = onQualityClick) {
                Text(text = state.selectedQuality)
            }
        }
    }
}

@Composable
private fun QualityPickerOverlay(
    qualities: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedFocus = remember { FocusRequester() }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        qualities.forEach { quality ->
            val isSelected = quality == selected
            Button(
                onClick = { onSelect(quality) },
                modifier = if (isSelected) Modifier.fillMaxWidth().focusRequester(selectedFocus)
                           else Modifier.fillMaxWidth(),
                colors = if (isSelected) ButtonDefaults.colors()
                         else ButtonDefaults.colors(
                             containerColor = Color.Transparent,
                             focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                         ),
            ) {
                Text(text = quality)
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { selectedFocus.requestFocus() }
    }
}

@Composable
private fun ChatPanel(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(messages, key = { it.id }) { msg ->
            ChatMessageRow(msg)
        }
    }
}

private val chatUsernameColors = listOf(
    Color(0xFFFF4500), Color(0xFF2E8B57), Color(0xFF1E90FF), Color(0xFFDAA520),
    Color(0xFFFF69B4), Color(0xFF9ACD32), Color(0xFFD2691E), Color(0xFF5F9EA0),
    Color(0xFFB22222), Color(0xFF00FA9A), Color(0xFF9B59B6), Color(0xFFFF7F50),
)

private fun nicknameColor(nickname: String): Color =
    chatUsernameColors[nickname.hashCode().and(0x7FFFFFFF) % chatUsernameColors.size]

@Composable
private fun ChatMessageRow(message: ChatMessage) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = nicknameColor(message.nickname))) {
                append(message.nickname)
            }
            append(": ")
            append(message.content)
        },
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
    )
}
