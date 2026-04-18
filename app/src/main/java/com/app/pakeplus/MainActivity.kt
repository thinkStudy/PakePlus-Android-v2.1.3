package com.app.pakeplus

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var blockView: View

    private var appConfig: AppConfig? = null
    private val CONFIG_URL = "http://down.wangwang59874.xyz/config.json"
    private val PREFS_NAME = "menu_config_prefs"
    private val KEY_CONFIG_JSON = "config_json"

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.single_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ConstraintLayout))
        { view, insets ->
            val systemBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBar.left, systemBar.top, systemBar.right, systemBar.bottom)
            insets
        }

        webView = findViewById(R.id.webview)
        fabMenu = findViewById(R.id.fabMenu)
        blockView = findViewById(R.id.blockView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportMultipleWindows(true)
        }

        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(false)

        webView.clearCache(true)

        webView.webViewClient = MyWebViewClient()
        webView.webChromeClient = MyChromeClient()

        // Setup gesture detector
        gestureDetector =
            GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                            if (diffX > 0) {
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                    return true
                                }
                            } else {
                                if (webView.canGoForward()) {
                                    webView.goForward()
                                    return true
                                }
                            }
                        }
                    }
                    return false
                }
            })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // FAB click -> show popup menu
        fabMenu.setOnClickListener {
            showFloatingMenu()
        }

        // Load config and initial page
        loadConfig()
        webView.loadUrl("https://juejin.cn/")
    }

    /**
     * Fetch menu config from remote, fallback to cached version
     */
    private fun loadConfig() {
        thread {
            try {
                val url = URL(CONFIG_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val json = JSONObject(response.toString())
                    val config = AppConfig.fromJson(json)

                    // Cache config
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString(KEY_CONFIG_JSON, response.toString())
                        .apply()

                    runOnUiThread {
                        appConfig = config
                    }
                } else {
                    loadCachedConfig()
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                loadCachedConfig()
            }
        }
    }

    /**
     * Load config from SharedPreferences cache
     */
    private fun loadCachedConfig() {
        try {
            val cached = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_CONFIG_JSON, null)
            if (cached != null) {
                val json = JSONObject(cached)
                val config = AppConfig.fromJson(json)
                runOnUiThread {
                    appConfig = config
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Show popup menu anchored to FAB
     */
    private fun showFloatingMenu() {
        val config = appConfig
        if (config == null || config.menuItems.isEmpty()) {
            Toast.makeText(this, "菜单加载中，请稍候...", Toast.LENGTH_SHORT).show()
            loadConfig()
            return
        }

        val popup = PopupMenu(this, fabMenu, Gravity.END)
        config.menuItems.forEachIndexed { index, item ->
            popup.menu.add(0, index, index, item.label)
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val idx = menuItem.itemId
            if (idx >= 0 && idx < config.menuItems.size) {
                handleMenuClick(config.menuItems[idx])
            }
            true
        }

        popup.show()
    }

    /**
     * Handle menu item click
     */
    private fun handleMenuClick(item: MenuItem) {
        if (item.needModal) {
            // Show confirmation dialog first
            AlertDialog.Builder(this)
                .setTitle(item.modalTitle)
                .setMessage(item.modalMsg)
                .setPositiveButton("确定") { _, _ ->
                    navigateToMenuItem(item)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            navigateToMenuItem(item)
        }
    }

    /**
     * Navigate to the menu item's URL
     */
    private fun navigateToMenuItem(item: MenuItem) {
        // Handle block view
        if (item.needBlock && item.blockHeight > 0) {
            val heightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                item.blockHeight.toFloat(),
                resources.displayMetrics
            ).toInt()
            blockView.layoutParams.height = heightPx
            blockView.visibility = View.VISIBLE

            // Also inject JS to hide top elements as fallback
            injectBlockJs(item.blockHeight)
        } else {
            blockView.visibility = View.GONE
        }

        // Load URL
        if (item.needReload || item.replace) {
            webView.loadUrl(item.url)
        } else {
            webView.loadUrl(item.url)
        }
    }

    /**
     * Inject JS to hide top portion of web page
     */
    private fun injectBlockJs(blockHeightDp: Int) {
        val js = """
            (function() {
                var blocker = document.getElementById('__native_blocker');
                if (!blocker) {
                    blocker = document.createElement('div');
                    blocker.id = '__native_blocker';
                    blocker.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:${blockHeightDp}dp;background:#fff;z-index:999999;pointer-events:auto;';
                    document.body.appendChild(blocker);
                } else {
                    blocker.style.display = 'block';
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * Remove block overlay
     */
    private fun removeBlock() {
        blockView.visibility = View.GONE
        val js = """
            (function() {
                var blocker = document.getElementById('__native_blocker');
                if (blocker) blocker.style.display = 'none';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class MyWebViewClient : WebViewClient() {

        private var debug = false

        @Deprecated("Deprecated in Java", ReplaceWith("false"))
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return false
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            println("webView onReceivedError: ${error?.description}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (debug) {
                val vConsole = assets.open("vConsole.js").bufferedReader().use { it.readText() }
                val openDebug = """var vConsole = new window.VConsole()"""
                view?.evaluateJavascript(vConsole + openDebug, null)
            }
            val injectJs = assets.open("custom.js").bufferedReader().use { it.readText() }
            view?.evaluateJavascript(injectJs, null)
        }
    }

    inner class MyChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            val url = view?.url
            println("wev view url:$url")
        }
    }
}
