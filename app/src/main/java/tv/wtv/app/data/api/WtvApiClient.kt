package tv.wtv.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

val emptyJsonBody: RequestBody = "{}".toRequestBody("application/json".toMediaType())

val wsOkHttpClient: OkHttpClient = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(30, TimeUnit.SECONDS)
    .build()

private val httpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Chromecast with Google TV) AppleWebKit/537.36")
                .header("Origin", "https://w.tv")
                .build()
        )
    }
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    .build()

private fun retrofit(baseUrl: String) = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(httpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

val profilesApi: ProfilesApi =
    retrofit("https://profiles-service.w.tv/").create(ProfilesApi::class.java)

val streamsSearchApi: StreamsSearchApi =
    retrofit("https://streams-search-service.w.tv/").create(StreamsSearchApi::class.java)

val streamsApi: StreamsApi =
    retrofit("https://streams-service.w.tv/").create(StreamsApi::class.java)

val chatsApi: ChatsApi =
    retrofit("https://chats-service.w.tv/").create(ChatsApi::class.java)
