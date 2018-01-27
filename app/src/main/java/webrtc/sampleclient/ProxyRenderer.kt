package webrtc.sampleclient

import org.webrtc.VideoRenderer

/**
 * @author Silvestr Predko.
 */
class ProxyRenderer : VideoRenderer.Callbacks {

  private var callback: VideoRenderer.Callbacks? = null

  override fun renderFrame(frame: VideoRenderer.I420Frame?) {
    callback?.renderFrame(frame) ?: VideoRenderer.renderFrameDone(frame)
  }

  @Synchronized
  fun setTarget(callbacks: VideoRenderer.Callbacks) {
    this.callback = callbacks
  }
}