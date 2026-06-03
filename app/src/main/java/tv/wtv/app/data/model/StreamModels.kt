package tv.wtv.app.data.model

import kotlinx.serialization.Serializable

// ── API response models ────────────────────────────────────────────────────────

@Serializable
data class ProfileResponse(val profile: ProfileData)

@Serializable
data class ProfileData(
    val userId: String,
    val nickname: String,
    val nicknameColor: String = "",
)

@Serializable
data class ChannelDetailResponse(val channel: ChannelDetailData)

@Serializable
data class ChannelDetailData(
    val channelId: String,
    val name: String,
    val followers: Int = 0,
    val live: Boolean = false,
    val liveStreamId: String = "",
    val verified: Boolean = false,
    val liveStream: LiveStreamData? = null,
)

@Serializable
data class LiveStreamData(
    val streamId: String,
    val title: String = "",
    val state: String = "",
    val startedAt: String = "",
    val playbackUrl: String,
    val viewers: Int = 0,
    val bitrate: Int = 0,
)

@Serializable
data class ChatJoinResponse(
    val token: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ChatBacklogResponse(val messages: List<BacklogMessage>)

@Serializable
data class BacklogMessage(
    val messageId: String,
    val type: String = "",
    val content: String,
    val sender: MessageSender,
)

@Serializable
data class MessageSender(
    val userId: String,
    val nickname: String,
)

// ── Domain models ──────────────────────────────────────────────────────────────

data class StreamInfo(
    val channelId: String,
    val streamId: String,
    val playbackUrl: String,
    val title: String,
    val startedAtMs: Long,
    val channelName: String,
)

data class ChatMessage(
    val id: String,
    val nickname: String,
    val content: String,
)
