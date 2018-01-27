package webrtc.sampleclient

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * @author Silvestr Predko.
 */
abstract class SimpleSdpObserver : SdpObserver {
  override fun onSetFailure(p0: String?) {}

  override fun onSetSuccess() {}

  override fun onCreateSuccess(p0: SessionDescription?) {}

  override fun onCreateFailure(p0: String?) {}
}