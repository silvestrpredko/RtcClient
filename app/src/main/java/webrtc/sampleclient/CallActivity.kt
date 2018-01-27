package webrtc.sampleclient

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * @author Silvestr Predko.
 */
class CallActivity : AppCompatActivity() {

  lateinit var remoteRender: SurfaceViewRenderer
  lateinit var localRender: SurfaceViewRenderer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)

    remoteRender = findViewById(R.id.remoteRenderer)
    localRender = findViewById(R.id.localRenderer)


    bindService(Intent(this, CallService::class.java), object : ServiceConnection {
      override fun onServiceDisconnected(name: ComponentName?) {}

      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null) {
          serviceConnected((service as CallService.CallBinder).callService)
        }
      }

    }, 0)
  }

  private fun serviceConnected(callService: CallService) {
    remoteRender.init(callService.baseContext.eglBaseContext, SimpleRendererEvents())
    localRender.init(callService.baseContext.eglBaseContext, SimpleRendererEvents())

    callService.setRemoteRenderTarget(remoteRender)
    callService.setLocalRenderTarget(localRender)
  }


  class SimpleRendererEvents : RendererCommon.RendererEvents {
    override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {}

    override fun onFirstFrameRendered() {
    }
  }
}