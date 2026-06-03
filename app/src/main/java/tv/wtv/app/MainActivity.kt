package tv.wtv.app

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                // Allows fetch() and WebSocket from file:// origin
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/index.html")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else finish()
            }
        })

        setContentView(webView)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        if (webView.dispatchKeyEvent(event)) true
        else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (webView.dispatchKeyEvent(event)) true
        else super.onKeyUp(keyCode, event)

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
