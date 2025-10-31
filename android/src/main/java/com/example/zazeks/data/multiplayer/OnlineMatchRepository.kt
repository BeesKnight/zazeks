package com.example.zazeks.data.multiplayer

import com.example.zazeks.di.IoDispatcher
import com.example.zazeks.infra.config.BackendConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import org.json.JSONObject

@Singleton
class OnlineMatchRepository @Inject constructor(
    private val backendConfig: BackendConfig,
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val eventsFlow = MutableSharedFlow<OnlineMatchEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<OnlineMatchEvent> = eventsFlow

    private var webSocket: WebSocket? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            emit(OnlineMatchEvent.Connected)
            sendSimple("join")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = runCatching { JSONObject(text) }.getOrElse {
                emit(OnlineMatchEvent.Error("Некорректный ответ сервера"))
                return
            }
            val type = json.optString(TYPE_FIELD)
            val payload = json.optJSONObject(PAYLOAD_FIELD)
            when (type) {
                MESSAGE_MATCH_FOUND -> emit(payload.toMatchFound())
                MESSAGE_BATTLE_START -> emit(payload.toBattleStart())
                MESSAGE_BLACKOUT -> emit(payload.toBlackout())
                MESSAGE_BATTLE_END -> emit(payload.toBattleEnd())
                MESSAGE_REPLAY -> emit(payload.toReplay())
                else -> emit(OnlineMatchEvent.ServerMessage(type.orEmpty(), payload))
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            emit(OnlineMatchEvent.Error(t.localizedMessage ?: "Ошибка подключения"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            emit(OnlineMatchEvent.Closed(code, reason))
        }
    }

    fun connect() {
        if (webSocket != null) return
        val url = buildWebSocketUrl()
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    fun setReady(ready: Boolean) {
        sendSimple(if (ready) "ready" else "unready")
    }

    fun sendGesture(gesture: String?, lastValidGesture: String?) {
        val payload = JSONObject()
        payload.put("gesture", gesture ?: JSONObject.NULL)
        payload.put("lastValidGesture", lastValidGesture ?: JSONObject.NULL)
        val message = JSONObject()
        message.put(TYPE_FIELD, "gesture")
        message.put(PAYLOAD_FIELD, payload)
        send(message)
    }

    fun requestPlayAgain() {
        sendSimple("play_again")
    }

    suspend fun submitResult(request: BattleResultRequest): Result<Unit> = withContext(ioDispatcher) {
        val url = buildRestUrl("multiplayer", "result")
        val body = JSONObject().apply {
            put("sessionId", request.sessionId)
            put("result", request.result)
            put("playerGesture", request.playerGesture)
            put("opponentGesture", request.opponentGesture)
            put("playerScore", request.playerScore)
            put("opponentScore", request.opponentScore)
            put("round", request.round)
        }
        val requestBody = body.toString().toRequestBody(JSON_MEDIA_TYPE)
        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        return@withContext runCatching {
            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response ${'$'}{response.code}")
                }
            }
        }
    }

    private fun buildWebSocketUrl(): HttpUrl {
        val base = backendConfig.baseUrl.toHttpUrlOrNull()
            ?: throw IllegalStateException("Invalid base URL: ${'$'}{backendConfig.baseUrl}")
        val scheme = when (base.scheme) {
            "https" -> "wss"
            "http" -> "ws"
            else -> base.scheme
        }
        return base.newBuilder()
            .scheme(scheme)
            .addPathSegments("ws/multiplayer")
            .build()
    }

    private fun buildRestUrl(vararg segments: String): HttpUrl {
        val base = backendConfig.baseUrl.toHttpUrlOrNull()
            ?: throw IllegalStateException("Invalid base URL: ${'$'}{backendConfig.baseUrl}")
        val builder = base.newBuilder()
        segments.forEach { segment -> builder.addPathSegment(segment) }
        return builder.build()
    }

    private fun sendSimple(type: String) {
        val json = JSONObject().apply { put(TYPE_FIELD, type) }
        send(json)
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    private fun emit(event: OnlineMatchEvent) {
        eventsFlow.tryEmit(event)
    }

    private fun JSONObject?.toMatchFound(): OnlineMatchEvent {
        val sessionId = this?.optString("sessionId")
        val opponent = this?.optString("opponent") ?: this?.optString("opponentName")
        val playerReady = this?.optBoolean("playerReady") ?: false
        val opponentReady = this?.optBoolean("opponentReady") ?: false
        val round = this?.optInt("round", 1) ?: 1
        val playerScore = this?.optInt("playerScore", 0) ?: 0
        val opponentScore = this?.optInt("opponentScore", 0) ?: 0
        return OnlineMatchEvent.MatchFound(
            sessionId = sessionId,
            opponentName = opponent,
            playerReady = playerReady,
            opponentReady = opponentReady,
            round = round,
            playerScore = playerScore,
            opponentScore = opponentScore,
        )
    }

    private fun JSONObject?.toBattleStart(): OnlineMatchEvent {
        val round = this?.optInt("round", 1) ?: 1
        val millis = max(0L, this?.optLong("remainingMillis") ?: this?.optLong("timerMillis") ?: 0L)
        val blackout = this?.optBoolean("blackout", false) ?: false
        return OnlineMatchEvent.BattleStart(round, millis, blackout)
    }

    private fun JSONObject?.toBlackout(): OnlineMatchEvent {
        val active = this?.optBoolean("active", true) ?: true
        val reason = this?.optString("reason")
        return OnlineMatchEvent.Blackout(active, reason)
    }

    private fun JSONObject?.toBattleEnd(): OnlineMatchEvent {
        val result = this?.optString("result")
        val playerGesture = this?.optString("playerGesture")
        val opponentGesture = this?.optString("opponentGesture")
        val playerScore = this?.optInt("playerScore", 0) ?: 0
        val opponentScore = this?.optInt("opponentScore", 0) ?: 0
        val round = this?.optInt("round", 1) ?: 1
        return OnlineMatchEvent.BattleEnd(
            result = result,
            playerGesture = playerGesture,
            opponentGesture = opponentGesture,
            playerScore = playerScore,
            opponentScore = opponentScore,
            round = round,
        )
    }

    private fun JSONObject?.toReplay(): OnlineMatchEvent {
        val round = this?.optInt("round", 1) ?: 1
        val playerScore = this?.optInt("playerScore", 0) ?: 0
        val opponentScore = this?.optInt("opponentScore", 0) ?: 0
        return OnlineMatchEvent.Replay(round, playerScore, opponentScore)
    }

    companion object {
        private const val TYPE_FIELD = "type"
        private const val PAYLOAD_FIELD = "payload"
        private const val MESSAGE_MATCH_FOUND = "match_found"
        private const val MESSAGE_BATTLE_START = "battle_start"
        private const val MESSAGE_BLACKOUT = "blackout"
        private const val MESSAGE_BATTLE_END = "battle_end"
        private const val MESSAGE_REPLAY = "replay"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class BattleResultRequest(
    val sessionId: String,
    val result: String?,
    val playerGesture: String?,
    val opponentGesture: String?,
    val playerScore: Int,
    val opponentScore: Int,
    val round: Int,
)

sealed class OnlineMatchEvent {
    object Connected : OnlineMatchEvent()
    data class MatchFound(
        val sessionId: String?,
        val opponentName: String?,
        val playerReady: Boolean,
        val opponentReady: Boolean,
        val round: Int,
        val playerScore: Int,
        val opponentScore: Int,
    ) : OnlineMatchEvent()

    data class BattleStart(
        val round: Int,
        val remainingMillis: Long,
        val blackout: Boolean,
    ) : OnlineMatchEvent()

    data class Blackout(val active: Boolean, val reason: String?) : OnlineMatchEvent()

    data class BattleEnd(
        val result: String?,
        val playerGesture: String?,
        val opponentGesture: String?,
        val playerScore: Int,
        val opponentScore: Int,
        val round: Int,
    ) : OnlineMatchEvent()

    data class Replay(
        val round: Int,
        val playerScore: Int,
        val opponentScore: Int,
    ) : OnlineMatchEvent()

    data class ServerMessage(val type: String, val payload: JSONObject?) : OnlineMatchEvent()

    data class Error(val message: String) : OnlineMatchEvent()

    data class Closed(val code: Int, val reason: String) : OnlineMatchEvent()
}
