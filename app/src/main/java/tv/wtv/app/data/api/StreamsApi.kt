package tv.wtv.app.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface StreamsApi {
    @POST("api/v1/streams/{streamId}/join")
    suspend fun joinStream(
        @Path("streamId") streamId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}
