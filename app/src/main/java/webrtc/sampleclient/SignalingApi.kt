package webrtc.sampleclient

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import okhttp3.*
import okio.ByteString
import webrtc.sampleclient.proto.Proto.*

/**
 * @author Silvestr Predko.
 */
class SignalingApi private constructor(
  private val request: Request,
  private val client: OkHttpClient,
  private val listener: SignalListener
) {

  companion object {
    fun initialize(request: Request, client: OkHttpClient, listener: SignalListener): SignalingApi {
      return SignalingApi(request, client, listener)
    }
  }

  private var webSocket: WebSocket? = null
  private val handlerThread = HandlerThread("SignalApiHandlerThread")
  private lateinit var handler: Handler
  private val uiHandler = Handler(Looper.getMainLooper())

  private val webSocketListener = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket?, response: Response?) {
      this@SignalingApi.webSocket = webSocket
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
      uiHandler.post({
        listener.onClosed(webSocket, t?.message)
      })
      close()
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
      try {
        bytes?.let(this@SignalingApi::parseMessage)
      } catch (e: Exception) {
        Log.e("SignalingApi", "Failed to parse message")
      }
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
      uiHandler.post({
        listener.onClosed(webSocket, reason)
      })
      close()
    }
  }

  private fun parseMessage(bytes: ByteString) {
    val messageContainer = MessageContainer.parseFrom(bytes.asByteBuffer())
    uiHandler.post({
      when (messageContainer.messageType) {
        MessageContainer.MessageType.CallRequest -> {
        }
        MessageContainer.MessageType.CallRequestError -> {
          listener.onCallRequestError(CallRequestError.parseFrom(messageContainer.message))
        }
        MessageContainer.MessageType.Peer -> {
          Log.d("CallService", "parseMessage: ")
          listener.onPeer(Peer.parseFrom(messageContainer.message))
        }
        MessageContainer.MessageType.Session -> {
          val session = Session.parseFrom(messageContainer.message)
          if (session.sessionType == Session.Type.ANSWER) {
            listener.onAnswer(session)
          } else {
            listener.onOffer(session)
          }
        }
        MessageContainer.MessageType.IceServers -> {
          listener.onIceServers(IceServers.parseFrom(messageContainer.message))
        }
        else -> {
          throw RuntimeException("Unrecognized Message")
        }
      }
    })
  }

  fun connect() {
    client.newWebSocket(request, webSocketListener)
    handlerThread.start()
    handler = Handler(handlerThread.looper)
  }

  fun sendMessage(message: MessageContainer) {
    handler.post({
      while (webSocket == null) {
      }
      webSocket?.send(ByteString.of(message.toByteArray(), 0, message.toByteArray().size))
    })
  }

  fun close() {
    handlerThread.quitSafely()
  }

  interface SignalListener {
    fun onClosed(webSocket: WebSocket?, error: String? = null)
    fun onPeer(peer: Peer)
    fun onOffer(offer: Session)
    fun onAnswer(answer: Session)
    fun onIceServers(servers: IceServers)
    fun onCallRequestError(callRequestError: CallRequestError)
  }
}