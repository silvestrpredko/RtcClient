package webrtc.sampleclient

import android.content.Context
import okhttp3.Request
import webrtc.sampleclient.proto.Proto.*

/**
 * @author Silvestr Predko.
 */

val stunServers = arrayOf(
  "stun:stun.l.google.com:19302",
  "stun:stun1.l.google.com:19302",
  "stun:stun2.l.google.com:19302",
  "stun:stun3.l.google.com:19302",
  "stun:stun4.l.google.com:19302",
  "stun:stun01.sipphone.com",
  "stun:stun.ekiga.net",
  "stun:stun.fwdnet.net",
  "stun:stun.voiparound.com",
  "stun:stun.voipbuster.com",
  "stun:stun.voipstunt.com"
)

fun getApp(context: Context): App {
  return context.applicationContext as App
}

fun buildConnectRequest(): Request {
  return Request.Builder()
    .url("https://web-rtc-signaling.herokuapp.com/webrtc")
    .build()
}

fun createCallRequest(callId: String, callMessage: String, clientId: String): MessageContainer {
  return MessageContainer.newBuilder()
    .setMessageType(MessageContainer.MessageType.CallRequest)
    .setMessage(
      CallRequest.newBuilder()
        .setCallId(callId)
        .setCallMessage(callMessage)
        .setLocalClientId(clientId)
        .build()
        .toByteString()
    ).build()
}

fun createPeerRequest(callId: String, callMessage: String, clientId: String): MessageContainer {
  return MessageContainer.newBuilder()
    .setMessageType(MessageContainer.MessageType.Peer)
    .setMessage(
      Peer.newBuilder()
        .setCallId(callId)
        .setCallMessage(callMessage)
        .setLocalClientId(clientId)
        .build()
        .toByteString()
    ).build()
}

fun createOfferSession(callId: String, localId: String, remoteId: String, description: String): MessageContainer {
  return MessageContainer.newBuilder()
    .setMessageType(MessageContainer.MessageType.Session)
    .setMessage(
      buildSession(callId, localId, remoteId, description)
        .setSessionType(Session.Type.OFFER)
        .build()
        .toByteString()
    )
    .build()
}

fun createAnswerSession(callId: String, localId: String, remoteId: String, description: String): MessageContainer {
  return MessageContainer.newBuilder()
    .setMessageType(MessageContainer.MessageType.Session)
    .setMessage(
      buildSession(callId, localId, remoteId, description)
        .setSessionType(Session.Type.ANSWER)
        .build()
        .toByteString()
    )
    .build()
}

fun buildSession(callId: String, localId: String, remoteId: String, description: String): Session.Builder {
  return Session.newBuilder()
    .setCallId(callId)
    .setRemoteClientId(remoteId)
    .setLocalClientId(localId)
    .setDescription(description)
    .setSessionType(Session.Type.OFFER)
}

fun createIceCandidate(iceCandidate: org.webrtc.IceCandidate): IceCandidate {
  return IceCandidate.newBuilder()
    .setSdp(iceCandidate.sdp)
    .setSdpMid(iceCandidate.sdpMid)
    .setServerUrl(iceCandidate.serverUrl)
    .setSpdMLineIndex(iceCandidate.sdpMLineIndex)
    .build()
}

fun fromServers(iceServers: IceServers): List<org.webrtc.IceCandidate> {
  return iceServers.iceServersList.map { ice ->
    org.webrtc.IceCandidate(ice.sdpMid, ice.spdMLineIndex, ice.sdp)
  }
}

fun createIceServers(iceCandidates: List<IceCandidate>, callId: String, clientId: String): MessageContainer {
  return MessageContainer.newBuilder()
    .setMessageType(MessageContainer.MessageType.IceServers)
    .setMessage(
      IceServers.newBuilder()
        .setCallId(callId)
        .setRemoteClientId(clientId)
        .addAllIceServers(iceCandidates)
        .build()
        .toByteString()
    )
    .build()
}

