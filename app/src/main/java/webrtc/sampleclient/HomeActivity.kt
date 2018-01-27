package webrtc.sampleclient

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_home)

    val callServiceIntent = Intent(applicationContext, CallService::class.java)

    ActivityCompat.requestPermissions(this,
      arrayOf(Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.RECORD_AUDIO),
      5
    )

    btnCall.setOnClickListener {
      callServiceIntent.putExtra(CallService.CONNECT_TYPE, CallService.ConnectType.CREATE.toString())
      callServiceIntent.putExtra(CallService.CALL_ID, etCallId.text.toString())
      ContextCompat.startForegroundService(applicationContext, callServiceIntent)
      startActivity(Intent(applicationContext, CallActivity::class.java))
    }

    btnConnect.setOnClickListener {
      callServiceIntent.putExtra(CallService.CONNECT_TYPE, CallService.ConnectType.CONNECT.toString())
      callServiceIntent.putExtra(CallService.CALL_ID, etCallId.text.toString())
      ContextCompat.startForegroundService(applicationContext, callServiceIntent)
      startActivity(Intent(applicationContext, CallActivity::class.java))
    }
  }
}
