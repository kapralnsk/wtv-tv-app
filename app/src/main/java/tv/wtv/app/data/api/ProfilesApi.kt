package tv.wtv.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import tv.wtv.app.data.model.ProfileResponse

interface ProfilesApi {
    @GET("api/v1/profiles/by-nickname/{slug}")
    suspend fun getProfileByNickname(@Path("slug") slug: String): ProfileResponse
}
