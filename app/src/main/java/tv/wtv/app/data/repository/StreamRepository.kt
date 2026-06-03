package tv.wtv.app.data.repository

import tv.wtv.app.data.api.ChatsApi
import tv.wtv.app.data.api.ProfilesApi
import tv.wtv.app.data.api.StreamsApi
import tv.wtv.app.data.api.StreamsSearchApi
import tv.wtv.app.data.api.chatsApi
import tv.wtv.app.data.api.emptyJsonBody
import tv.wtv.app.data.api.profilesApi
import tv.wtv.app.data.api.streamsApi
import tv.wtv.app.data.api.streamsSearchApi
import tv.wtv.app.data.model.ChatMessage
import tv.wtv.app.data.model.StreamInfo

class StreamRepository(
    private val profiles: ProfilesApi = profilesApi,
    private val streamsSearch: StreamsSearchApi = streamsSearchApi,
    private val streams: StreamsApi = streamsApi,
    private val chats: ChatsApi = chatsApi,
) {

    suspend fun loadStreamInfo(slug: String): StreamInfo {
        val channelId = profiles.getProfileByNickname(slug).profile.userId
        val channel = streamsSearch.getChannel(channelId).channel
        val stream = channel.liveStream ?: error("$slug is not live")
        val startedAtMs = runCatching {
            java.time.Instant.parse(stream.startedAt).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
        return StreamInfo(
            channelId = channelId,
            streamId = stream.streamId,
            playbackUrl = stream.playbackUrl,
            title = stream.title,
            startedAtMs = startedAtMs,
            channelName = channel.name,
        )
    }

    suspend fun joinStream(streamId: String) {
        runCatching { streams.joinStream(streamId, emptyJsonBody) }
    }

    suspend fun getChatToken(channelId: String): String =
        chats.joinChat(channelId, emptyJsonBody).token

    suspend fun getChatBacklog(channelId: String): List<ChatMessage> =
        chats.getBacklog(channelId).messages.map { msg ->
            ChatMessage(
                id = msg.messageId,
                nickname = msg.sender.nickname,
                content = msg.content,
            )
        }
}
