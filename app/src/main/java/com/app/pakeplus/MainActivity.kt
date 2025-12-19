package com.app.pakeplus

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.PermissionRequest
import androidx.activity.enableEdgeToEdge
// import android.view.Menu
// import android.view.WindowInsets
// import com.google.android.material.snackbar.Snackbar
// import com.google.android.material.navigation.NavigationView
// import androidx.navigation.findNavController
// import androidx.navigation.ui.AppBarConfiguration
// import androidx.navigation.ui.navigateUp
// import androidx.navigation.ui.setupActionBarWithNavController
// import androidx.navigation.ui.setupWithNavController
// import androidx.drawerlayout.widget.DrawerLayout
// import com.app.pakeplus.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.pakeplus.ui.home.WebAppInterface

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

//    private lateinit var appBarConfiguration: AppBarConfiguration
//    private lateinit var binding: ActivityMainBinding

    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetectorCompat

        // 定义一个权限请求码
    private companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            // 根据你的目标版本选择：
            Manifest.permission.READ_EXTERNAL_STORAGE, // API < 33
            // 或
            Manifest.permission.READ_MEDIA_IMAGES // API >= 33
        )
    }
     private fun checkAndRequestPermissions(): Boolean {
        // 检查权限是否已授予
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsToRequest.isNotEmpty()) {
            // 申请尚未授予的权限
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            false // 权限尚未获得
        } else {
            true // 所有权限已授予
        }
    }
    // 处理权限申请结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                // 权限全部授予，可以重新初始化WebView或通知用户
                //Toast.makeText(this, "文件访问权限已开启", Toast.LENGTH_SHORT).show()
            } else {
                // 有权限被拒绝，可以解释为什么需要此权限
                Toast.makeText(
                    this,
                    "文件访问权限被拒绝，可能无法使用上传功能",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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

        webView = findViewById<WebView>(R.id.webview)

        webView.settings.apply {
            javaScriptEnabled = true       // 启用JS
            domStorageEnabled = true       // 启用DOM存储（Vue 需要）
            allowFileAccess = true         // 允许文件访问
            setSupportMultipleWindows(true)
        }

        // webView.settings.userAgentString = ""

        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(false)

        // clear cache
        webView.clearCache(true)

        // 2. 注入 JavaScript 桥接对象，并命名为 "AndroidBridge"
        // 注意：第三个参数是 JS 中访问对象的名称
        webView.addJavascriptInterface(WebAppInterface(this，webView), "AndroidBridge")

        // inject js
        webView.webViewClient = MyWebViewClient()

        // get web load progress
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

                    // Only handle horizontal swipes
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                            if (diffX > 0) {
                                // Swipe right - go back
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                    return true
                                }
                            } else {
                                // Swipe left - go forward
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

        // Set touch listener for WebView
        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        webView.loadUrl("https://juejin.cn/")
        // webView.loadUrl("file:///android_asset/index.html")

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(R.layout.single_main)

//        setSupportActionBar(binding.appBarMain.toolbar)

//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.fab).show()
//        }

//        val drawerLayout: DrawerLayout = binding.drawerLayout
//        val navView: NavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
//            ), drawerLayout
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    private var mUploadCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1002


    inner class MyWebViewClient : WebViewClient() {

        // vConsole debug
        private var debug = true

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
                // vConsole
                val vConsole = assets.open("vConsole.js").bufferedReader().use { it.readText() }
                val openDebug = """var vConsole = new window.VConsole()"""
                view?.evaluateJavascript(vConsole + openDebug, null)
            }
            // inject js
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
        // 处理文件选择 (Android 5.0+)
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            // 1. 保存回调，在onActivityResult中使用
            mUploadCallback?.onReceiveValue(null) // 取消任何未完成的请求
            mUploadCallback = filePathCallback

            // 2. 检查权限
            if (!checkAndRequestPermissions()) {
                // 如果权限未授予，等待权限申请结果
                // 这里可以存储回调，在权限授予后再启动选择器
                Toast.makeText(this@MainActivity, "请先授予文件访问权限", Toast.LENGTH_SHORT).show()
                // 注意：这里需要更复杂的逻辑来延迟启动选择器，简单起见可以先返回false
                return false
            }

            // 3. 权限已授予，创建并启动文件选择Intent
            val intent = fileChooserParams?.createIntent()
            try {
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                mUploadCallback = null
                Toast.makeText(this@MainActivity, "未找到文件管理器", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }
        override fun onPermissionRequest(request: PermissionRequest?) {
            request ?: return // 如果 request 为空，则直接返回

            // 1. 获取请求的权限和来源（可用于日志或逻辑判断）
            val requestedResources = request.resources // 例如: [RESOURCE_VIDEO_CAPTURE, RESOURCE_AUDIO_CAPTURE]
            val origin = request.origin.toString()

            // 2. 【策略1】简单策略：直接授予所有请求的权限
            // request.grant(request.resources)

            // 3. 【策略2】选择性授权：检查并只授予你同意的权限
            val grantedResources = mutableListOf<String>()
            for (resource in requestedResources) {
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        // 可以在这里加入额外的条件，例如检查是否已获得Android系统摄像头权限
                        grantedResources.add(resource)
                    }
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        // 检查Android系统麦克风权限等
                        grantedResources.add(resource)
                    }
                    PermissionRequest.RESOURCE_MIDI_SYSEX -> {
                        // 例如，我们可能不想授予MIDI系统独占权限
                        // 选择不添加到 grantedResources 列表中
                        println("拒绝授予权限: $resource")
                    }
                    // ... 可以处理其他资源类型
                }
            }

            // 4. 根据筛选结果进行授权或拒绝
            if (grantedResources.isNotEmpty()) {
                // 授予同意的权限列表
                request.grant(grantedResources.toTypedArray())
            } else {
                // 如果没有一个权限被同意，则整体拒绝
                request.deny()
            }

            // 注意：如果既不调用 grant() 也不调用 deny()，请求将一直被挂起。
        }
        // 接收文件选择结果
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == FILE_CHOOSER_REQUEST_CODE && mUploadCallback != null) {
                var results: Array<Uri>? = null

                if (resultCode == RESULT_OK && data != null) {
                    // 处理单选或多选文件
                    if (data.data != null) {
                        // 单选文件
                        results = arrayOf(data.data!!)
                    } else if (data.clipData != null) {
                        // 多选文件
                        val clipData = data.clipData!!
                        val uris = ArrayList<Uri>(clipData.itemCount)
                        for (i in 0 until clipData.itemCount) {
                            val item = clipData.getItemAt(i)
                            item.uri?.let { uris.add(it) }
                        }
                        results = uris.toTypedArray()
                    }
                }

                // 必须调用此回调通知WebView结果
                mUploadCallback?.onReceiveValue(results)
                mUploadCallback = null // 清空回调
            }
        }
    }
}