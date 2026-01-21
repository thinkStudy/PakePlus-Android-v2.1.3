package com.app.pakeplus

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.pakeplus.ui.home.WebAppInterface

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.content.Context
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var gestureDetector: GestureDetectorCompat
    private var mUploadCallback: ValueCallback<Array<Uri>>? = null
    private var mFileChooserParams: WebChromeClient.FileChooserParams? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1002

    private companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            // 如果目标是API 33+，可以使用：
            // Manifest.permission.READ_MEDIA_IMAGES
        )
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.READ_MEDIA_IMAGES
        }

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.READ_MEDIA_IMAGES }
            if (allGranted) {
                // 权限已授予，可以处理文件选择
                if (mUploadCallback != null && mFileChooserParams != null) {
                    startFileChooser()
                }
            } else {
                mUploadCallback?.onReceiveValue(null)
                mUploadCallback = null
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
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportMultipleWindows(true)
        }

        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(false)

        webView.clearCache(true)

        // 修复：使用英文逗号
        webView.addJavascriptInterface(WebAppInterface(this, webView), "AndroidBridge")

        webView.webViewClient = MyWebViewClient()
        webView.webChromeClient = MyChromeClient()

        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
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

        webView.loadUrl("https://juejin.cn/")
    }
// 1. 在类级别定义 ActivityResultLauncher
// private lateinit var someActivityLauncher: ActivityResultLauncher<Intent>
    private fun startFileChooser(): Boolean {
        val intent = mFileChooserParams?.createIntent()
        if (intent == null) {
            mUploadCallback?.onReceiveValue(null)
            mUploadCallback = null
            mFileChooserParams = null
            Toast.makeText(this, "无法创建文件选择器", Toast.LENGTH_SHORT).show()
            return false
        }
        
        try {
            //  someActivityLauncher = registerForActivityResult(
            //     ActivityResultContracts.StartActivityForResult()
            // ) { result ->
            //     if (result.resultCode == RESULT_OK) {
            //          var results: Array<Uri>? = null
            //         val data: Intent? = result.data
            //          if (data.data != null) {
            //             results = arrayOf(data.data!!)
            //         } else if (data.clipData != null) {
            //             val clipData = data.clipData!!
            //             val uris = ArrayList<Uri>(clipData.itemCount)
            //             for (i in 0 until clipData.itemCount) {
            //                 val item = clipData.getItemAt(i)
            //                 item.uri?.let { uris.add(it) }
            //             }
            //             results = uris.toTypedArray()
            //         }
            //     }

            //     mUploadCallback?.onReceiveValue(results)
            //     mUploadCallback = null
            //     mFileChooserParams = null
               
            // }
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
            return true
        } catch (e: ActivityNotFoundException) {
            mUploadCallback?.onReceiveValue(null)
            mUploadCallback = null
            mFileChooserParams = null
            Toast.makeText(this, "未找到文件管理器", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE && mUploadCallback != null) {
            var results: Array<Uri>? = null

            if (resultCode == RESULT_OK && data != null) {
                if (data.data != null) {
                    results = arrayOf(data.data!!)
                } else if (data.clipData != null) {
                    val clipData = data.clipData!!
                    val uris = ArrayList<Uri>(clipData.itemCount)
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        item.uri?.let { uris.add(it) }
                    }
                    results = uris.toTypedArray()
                }
            }

            mUploadCallback?.onReceiveValue(results)
            mUploadCallback = null
            mFileChooserParams = null
        }
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
            println("web view url:$url")
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            mUploadCallback?.onReceiveValue(null)
            mUploadCallback = filePathCallback
            mFileChooserParams = fileChooserParams

            if (!checkAndRequestPermissions()) {
                Toast.makeText(this@MainActivity, "请先授予文件访问权限", Toast.LENGTH_SHORT).show()
                return true
            }

            return startFileChooser()
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request ?: return
            val requestedResources = request.resources
            val grantedResources = mutableListOf<String>()
            
            for (resource in requestedResources) {
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        grantedResources.add(resource)
                    }
                    else -> {
                        println("拒绝授予权限: $resource")
                    }
                }
            }

            if (grantedResources.isNotEmpty()) {
                request.grant(grantedResources.toTypedArray())
            } else {
                request.deny()
            }
        }
    }
}