package tv.wtv.app.data.repository

import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import tv.wtv.app.data.api.ChatsApi
import tv.wtv.app.data.api.ProfilesApi
import tv.wtv.app.data.api.StreamsApi
import tv.wtv.app.data.api.StreamsSearchApi
import tv.wtv.app.data.model.BacklogMessage
import tv.wtv.app.data.model.ChannelDetailData
import tv.wtv.app.data.model.ChannelDetailResponse
import tv.wtv.app.data.model.ChatBacklogResponse
import tv.wtv.app.data.model.ChatJoinResponse
import tv.wtv.app.data.model.ChatMessage
import tv.wtv.app.data.model.LiveStreamData
import tv.wtv.app.data.model.MessageSender
import tv.wtv.app.data.model.ProfileData
import tv.wtv.app.data.model.ProfileResponse

class StreamRepositoryTest {

    private fun liveStream() = LiveStreamData(
        streamId = "stream-456",
        playbackUrl = "https://example.com/stream.m3u8",
        title = "Playing something",
        viewers = 42,
        startedAt = "2026-01-01T00:00:00Z",
    )

    private fun liveChannel() = ChannelDetailData(
        channelId = "ch-123",
        name = "TestChannel",
        live = true,
        liveStream = liveStream(),
    )

    private fun offlineChannel() = ChannelDetailData(
        channelId = "ch-123",
        name = "TestChannel",
        live = false,
        liveStream = null,
    )

    private fun repository(
        userId: String = "user-123",
        channel: ChannelDetailData = liveChannel(),
        chatToken: String = "tok-abc",
        backlog: List<BacklogMessage> = emptyList(),
    ) = StreamRepository(
        profiles = object : ProfilesApi {
            override suspend fun getProfileByNickname(slug: String) =
                ProfileResponse(ProfileData(userId, slug))
        },
        streamsSearch = object : StreamsSearchApi {
            override suspend fun getChannel(channelId: String) = ChannelDetailResponse(channel)
        },
        streams = object : StreamsApi {
            override suspend fun joinStream(streamId: String, body: RequestBody) =
                Response.success<ResponseBody>(null)
        },
        chats = object : ChatsApi {
            override suspend fun joinChat(chatId: String, body: RequestBody) = ChatJoinResponse(chatToken)
            override suspend fun getBacklog(chatId: String) = ChatBacklogResponse(backlog)
        },
    )

    @Test
    fun `loadStreamInfo maps profile and channel to StreamInfo`() = runTest {
        val info = repository(userId = "user-123").loadStreamInfo("dunduk")
        assertEquals("user-123", info.channelId)
        assertEquals("stream-456", info.streamId)
        assertEquals("https://example.com/stream.m3u8", info.playbackUrl)
        assertEquals("Playing something", info.title)
        assertEquals(1_767_225_600_000L, info.startedAtMs)
        assertEquals("TestChannel", info.channelName)
    }

    @Test
    fun `loadStreamInfo uses userId from profile not channel`() = runTest {
        val info = repository(userId = "resolved-user-id").loadStreamInfo("dunduk")
        assertEquals("resolved-user-id", info.channelId)
    }

    @Test
    fun `loadStreamInfo throws when channel is not live`() = runTest {
        try {
            repository(channel = offlineChannel()).loadStreamInfo("dunduk")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("dunduk is not live", e.message)
        }
    }

    @Test
    fun `getChatToken returns token from join response`() = runTest {
        assertEquals("tok-xyz", repository(chatToken = "tok-xyz").getChatToken("ch-123"))
    }

    @Test
    fun `getChatBacklog maps messages to ChatMessage`() = runTest {
        val backlog = listOf(
            BacklogMessage("m1", "MESSAGE", "hello world", MessageSender("u1", "Alice")),
            BacklogMessage("m2", "MESSAGE", "hey there", MessageSender("u2", "Bob")),
        )
        val result = repository(backlog = backlog).getChatBacklog("ch-123")
        assertEquals(listOf(
            ChatMessage("m1", "Alice", "hello world"),
            ChatMessage("m2", "Bob", "hey there"),
        ), result)
    }

    @Test
    fun `getChatBacklog returns empty list when no messages`() = runTest {
        assertEquals(emptyList<ChatMessage>(), repository().getChatBacklog("ch-123"))
    }

    @Test
    fun `joinStream does not throw when API fails`() = runTest {
        val repo = StreamRepository(
            profiles = object : ProfilesApi {
                override suspend fun getProfileByNickname(slug: String) =
                    ProfileResponse(ProfileData("u", slug))
            },
            streamsSearch = object : StreamsSearchApi {
                override suspend fun getChannel(channelId: String) =
                    ChannelDetailResponse(liveChannel())
            },
            streams = object : StreamsApi {
                override suspend fun joinStream(streamId: String, body: RequestBody): Response<ResponseBody> =
                    throw RuntimeException("network error")
            },
            chats = object : ChatsApi {
                override suspend fun joinChat(chatId: String, body: RequestBody) = ChatJoinResponse("tok")
                override suspend fun getBacklog(chatId: String) = ChatBacklogResponse(emptyList())
            },
        )
        repo.joinStream("stream-456")
    }
}
