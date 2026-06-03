package tv.wtv.app.data.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import tv.wtv.app.data.api.wsOkHttpClient
import tv.wtv.app.data.model.ChatMessage

private const val IVS_CHAT_URL = "wss://edge.ivschat.eu-central-1.amazonaws.com/"

internal fun parseIvsFrame(text: String): ChatMessage? = try {
    val obj = Json.parseToJsonElement(text).jsonObject
    if (obj["Type"]?.jsonPrimitive?.content != "EVENT") return null
    if (obj["EventName"]?.jsonPrimitive?.content != "MESSAGE") return null
    val dataStr = obj["Attributes"]?.jsonObject?.get("data")?.jsonPrimitive?.content ?: return null
    val inner = Json.parseToJsonElement(dataStr).jsonObject
    val sender = inner["sender"]?.jsonObject ?: return null
    ChatMessage(
        id = inner["messageId"]?.jsonPrimitive?.content ?: return null,
        nickname = sender["nickname"]?.jsonPrimitive?.content ?: return null,
        content = inner["content"]?.jsonPrimitive?.content ?: return null,
    )
} catch (_: Exception) { null }

class ChatWebSocket(
    private val scope: CoroutineScope,
    private val token: String,
    private val onMessage: suspend (ChatMessage) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder()
            .url(IVS_CHAT_URL)
            .header("Sec-WebSocket-Protocol", token)
            .header("Origin", "https://w.tv")
            .build()
        webSocket = wsOkHttpClient.newWebSocket(request, Listener())
    }

    fun close() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    private inner class Listener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val msg = parseIvsFrame(text) ?: return
            scope.launch { onMessage(msg) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onError(t)
        }
    }
}
