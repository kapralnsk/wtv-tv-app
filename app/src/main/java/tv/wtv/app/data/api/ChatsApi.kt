package tv.wtv.app.data.api

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import tv.wtv.app.data.model.ChatBacklogResponse
import tv.wtv.app.data.model.ChatJoinResponse

interface ChatsApi {
    @POST("api/v1/chats/{chatId}/join")
    suspend fun joinChat(
        @Path("chatId") chatId: String,
        @Body body: RequestBody,
    ): ChatJoinResponse

    @GET("api/v1/chats/{chatId}/messages")
    suspend fun getBacklog(@Path("chatId") chatId: String): ChatBacklogResponse
}
