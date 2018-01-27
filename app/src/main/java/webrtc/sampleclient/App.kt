package webrtc.sampleclient

import android.app.Application
import okhttp3.OkHttpClient

/**
 * @author Silvestr Predko.
 */
class App : Application() {

  lateinit var client: OkHttpClient

  override fun onCreate() {
    super.onCreate()
    initialize()
  }

  private fun initialize() {
    client = OkHttpClient.Builder()
//        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

  }
}