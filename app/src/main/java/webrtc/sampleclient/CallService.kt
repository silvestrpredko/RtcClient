package webrtc.sampleclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.view.Display
import android.view.WindowManager
import okhttp3.WebSocket
import org.webrtc.*
import webrtc.sampleclient.proto.Proto

/**
 * @author Silvestr Predko.
 */
class CallService : Service(), SignalingApi.SignalListener {

  override fun onBind(intent: Intent?): IBinder = CallBinder(this)

  private lateinit var factory: PeerConnectionFactory
  private lateinit var localAudioTrack: AudioTrack
  private lateinit var localVideoTrack: VideoTrack
  private lateinit var videoCapturer: VideoCapturer
  private lateinit var mediaStreamLocal: MediaStream
  private lateinit var api: SignalingApi
  private lateinit var localProxyRender: ProxyRenderer
  private lateinit var remoteProxyRender: ProxyRenderer
  public lateinit var baseContext: EglBase

  private lateinit var callId: String
  private lateinit var clientId: String

  private var peerConnection: PeerConnection? = null

  companion object {
    const val LOCAL_AUDIO_TRACK = "local_audio_track"
    const val LOCAL_VIDEO_TRACK = "local_video_track"
    const val LOCAL_MEDIA_STREAM = "local_media_stream"
    const val CONNECT_TYPE = "connect_type"
    const val FPS = 30

    const val CALL_ID = "call_id"

    init {
      System.loadLibrary("jingle_peerconnection_so")
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val callId = intent?.getStringExtra(CALL_ID)
    val connectType = intent?.getStringExtra(CONNECT_TYPE)

    if (callId != null && connectType != null) {
      this.callId = callId

      when (connectType) {
        ConnectType.CONNECT.name -> {
          connectToCall(callId, "something")
        }
        ConnectType.CREATE.name -> {
          startCall(callId, "something")
        }
      }
    }

    return Service.START_NOT_STICKY
  }

  override fun onClosed(webSocket: WebSocket?, error: String?) {

  }

  override fun onPeer(peer: Proto.Peer) {
    Log.d("CallService", "onPeer - Factory: $factory")
    Log.d("CallService", "onPeer - Thread Name: ${Thread.currentThread().name}")
    peerConnection = createPeerConnection(factory, callId, peer.localClientId)

    Log.d("CallService", "onPeer: ${peer.localClientId}")

    Log.d("CallService", "onPeer: $peerConnection")

    peerConnection?.addStream(mediaStreamLocal)
    peerConnection?.createOffer(object : SimpleSdpObserver() {
      override fun onCreateSuccess(p0: SessionDescription?) {
        if (p0 == null) {
          throw RuntimeException("SessionDescription == null")
        }

        Log.d("CallService", "onCreateSuccess: Create Offer")

        peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
          override fun onSetSuccess() {
            api.sendMessage(createOfferSession(peer.callId, clientId, peer.localClientId, p0.description))
          }
        }, p0)
      }

      override fun onCreateFailure(p0: String?) {
        Log.d("CallService", "onCreateFailure: $p0")
      }
    }, MediaConstraints())
  }

  override fun onOffer(offer: Proto.Session) {
    Log.d("CallService", "onOffer: ")
    peerConnection = createPeerConnection(factory, callId, offer.localClientId)

    peerConnection?.addStream(mediaStreamLocal)

    peerConnection?.setRemoteDescription(
      object : SimpleSdpObserver() {}, SessionDescription(
        SessionDescription.Type.OFFER,
        offer.description
      )
    )

    peerConnection?.createAnswer(object : SimpleSdpObserver() {
      override fun onCreateSuccess(p0: SessionDescription?) {
        if (p0 == null) {
          return
        }

        Log.d("CallService", "onCreateSuccess: Answer Created successfully")

        peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
          override fun onSetSuccess() {
            api.sendMessage(createAnswerSession(offer.callId, clientId, offer.localClientId, p0.description))
          }
        }, p0)
      }
    }, MediaConstraints())
  }

  override fun onAnswer(answer: Proto.Session) {
    Log.d("CallService", "onAnswer: $peerConnection")
    Log.d("CallService", "onAnswer: Description")
    peerConnection?.setRemoteDescription(
      object : SimpleSdpObserver() {}, SessionDescription(
        SessionDescription.Type.ANSWER,
        answer.description
      )
    )
  }

  override fun onIceServers(servers: Proto.IceServers) {
    Log.d("CallService", "onIceServers: ")
    fromServers(servers).forEach {
      peerConnection?.addIceCandidate(it)
    }
  }

  override fun onCallRequestError(callRequestError: Proto.CallRequestError) {}

  private fun connectToCall(callId: String, callMessage: String) {
    api.connect()
    val androidId = Settings.Secure.getString(
      contentResolver,
      Settings.Secure.ANDROID_ID
    )
    clientId = androidId
    api.sendMessage(createPeerRequest(callId, callMessage, androidId))
  }

  private fun startCall(callId: String, callMessage: String) {
    api.connect()
    val androidId = Settings.Secure.getString(
      contentResolver,
      Settings.Secure.ANDROID_ID
    )
    clientId = androidId
    api.sendMessage(createCallRequest(callId, callMessage, androidId))
  }

  override fun onCreate() {
    val endCall = Intent(applicationContext, CallService::class.java)
    endCall.putExtra("CALL", 1)

    var channelID = "none"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      channelID = createChannel(this)
    }
    val notification = NotificationCompat.Builder(applicationContext, channelID)
      .setContentTitle("SimpleClientCall")
      .setContentText("Call")
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setAutoCancel(false)
      .setOngoing(true)
      .setSmallIcon(R.drawable.ic_call)
      .addAction(
        R.drawable.ic_call_end,
        "End Call",
        PendingIntent.getService(applicationContext, 5, endCall, PendingIntent.FLAG_UPDATE_CURRENT)
      )
      .build()
    startForeground(11, notification)
    initialize()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createChannel(context: Service): String {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelName = "Playback channel"
    val importance = NotificationManager.IMPORTANCE_LOW
    val notificationChannel = NotificationChannel("CallChannel", channelName, importance)
    notificationChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
    notificationManager.createNotificationChannel(notificationChannel)
    return "CallChannel"
  }

  private fun initialize() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    audioManager.isSpeakerphoneOn = true
    baseContext = `EglBase$$CC`.`create$$STATIC$$`()
    factory = getPeerFactory()
    factory.setVideoHwAccelerationOptions(baseContext.eglBaseContext, null)
    localAudioTrack = createAudioTrack(factory, MediaConstraints())
    videoCapturer = createVideoCapturer(true)
    localVideoTrack = createVideoTrack(factory, videoCapturer, windowManager.defaultDisplay)
    localProxyRender = ProxyRenderer()
    remoteProxyRender = ProxyRenderer()
    localVideoTrack.addRenderer(VideoRenderer(localProxyRender))
    mediaStreamLocal = factory.createLocalMediaStream(LOCAL_MEDIA_STREAM)
    mediaStreamLocal.addTrack(localAudioTrack)
    mediaStreamLocal.addTrack(localVideoTrack)
    api = SignalingApi.initialize(buildConnectRequest(), getApp(applicationContext).client, this)
    Log.d("CallService", "initialized: ")
  }


  private fun createVideoCapturer(isFrontCamera: Boolean): VideoCapturer {
    val enumerator = Camera2Enumerator(this)

    return if (isFrontCamera) {
      enumerator.createCapturer(enumerator.deviceNames[1], null)
    } else {
      enumerator.createCapturer(enumerator.deviceNames[0], null)
    }
  }

  private fun getPeerFactory(): PeerConnectionFactory {
    PeerConnectionFactory.initialize(
      PeerConnectionFactory.InitializationOptions
        .builder(applicationContext)
        .setEnableVideoHwAcceleration(true)
        .createInitializationOptions()
    )

    return PeerConnectionFactory(PeerConnectionFactory.Options().apply {
      disableEncryption = false
      disableNetworkMonitor = false
    })
  }

  private fun createPeerConnection(factory: PeerConnectionFactory, callId: String, clientId: String): PeerConnection {
    return factory.createPeerConnection(
      listOf(
        PeerConnection.IceServer.builder(stunServers.toList())
          .createIceServer()
      ),
      object : ObserverPeerConnection() {
        override fun onIceCandidate(p0: IceCandidate?) {
          p0?.let { api.sendMessage(createIceServers(listOf(createIceCandidate(it)), callId, clientId)) }
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

        override fun onAddStream(p0: MediaStream?) {
          Log.d("CallService", "onAddStream: ${p0?.videoTracks?.size}")
          val remoteVideoTrack = p0?.videoTracks?.get(0)
          remoteVideoTrack?.setEnabled(true)
          remoteVideoTrack?.addRenderer(VideoRenderer(remoteProxyRender))
        }
      }
    )
  }

  private fun createAudioTrack(
    peerConnectionFactory: PeerConnectionFactory,
    mediaConstraints: MediaConstraints
  ): AudioTrack {

    val audioTrack = peerConnectionFactory.createAudioTrack(
      LOCAL_AUDIO_TRACK,
      peerConnectionFactory.createAudioSource(mediaConstraints)
    )

    audioTrack.setEnabled(false)

    return audioTrack
  }

  private fun createVideoTrack(
    peerConnectionFactory: PeerConnectionFactory,
    videoCapture: VideoCapturer,
    display: Display
  ): VideoTrack {

    val point = Point()
    display.getRealSize(point)

    val videoSource = peerConnectionFactory.createVideoSource(videoCapture)

    videoSource.adaptOutputFormat(point.x, point.y, FPS)
    videoCapture.startCapture(point.x, point.y, FPS)
    val videoTrack = peerConnectionFactory.createVideoTrack(
      LOCAL_VIDEO_TRACK,
      videoSource
    )
    videoTrack.setEnabled(true)

    return videoTrack
  }

  fun setRemoteRenderTarget(target: SurfaceViewRenderer) {
    remoteProxyRender.setTarget(target)
  }

  fun setLocalRenderTarget(target: SurfaceViewRenderer) {
    localProxyRender.setTarget(target)
  }

  override fun onDestroy() {
    super.onDestroy()
    peerConnection?.close()
    peerConnection = null
  }

  class CallBinder(val callService: CallService) : Binder()

  enum class ConnectType {
    CONNECT, CREATE
  }
}