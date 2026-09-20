package com.wapo.flagship.features.notification

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.washingtonpost.android.notifications.test.R
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.ImageLoader
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import com.washingtonpost.android.volley.toolbox.Volley
import org.junit.Ignore
import rx.Observable

@Ignore("Not a test")
class NotificationTestActivity : AppCompatActivity(), AlertsActivityInterface, AlertManagerProvider, ImageLoaderProvider {

    lateinit var alertManager: AlertManager
    lateinit var _alertsSettings: AlertsSettings
    val clickedUrl = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common)
    }

    override fun getImageLoader(): AnimatedImageLoader {
        return AnimatedImageLoader(Volley.newRequestQueue(this), TestCache(), null)
    }

    override fun getAlertManager(): Observable<out AlertManager> {
        return Observable.just(alertManager)
    }

    override val alertsSettings: AlertsSettings
        get() = _alertsSettings

    var openAlertSettingsCalled = false

    override fun openAlertsSettings() {
        openAlertSettingsCalled = true
    }

    override fun openNotification(notificationUrl: String, notificationArticleType: String?, notificationTopic : String?) {
        clickedUrl.add(notificationUrl)
    }

    override fun logExtras(str: String) {
    }

    override fun sendException(t: Throwable) {
    }
}

class TestCache : ImageLoader.ImageCache {
    override fun getBitmap(url: String?): Bitmap? {
        return null
    }

    override fun putBitmap(url: String?, bitmap: Bitmap?) {
    }
}