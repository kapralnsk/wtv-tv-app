package tv.wtv.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import tv.wtv.app.data.model.ChannelDetailResponse

interface StreamsSearchApi {
    @GET("api/v1/channels/{channelId}")
    suspend fun getChannel(@Path("channelId") channelId: String): ChannelDetailResponse
}
