package webrtc.sampleclient

import org.webrtc.*

/**
 * @author Silvestr Predko.
 */
abstract class ObserverPeerConnection : PeerConnection.Observer {

  override fun onDataChannel(p0: DataChannel?) {}

  override fun onIceConnectionReceivingChange(p0: Boolean) {}

  override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}

  override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}

  override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

  override fun onRemoveStream(p0: MediaStream?) {}

  override fun onRenegotiationNeeded() {}

  override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
}