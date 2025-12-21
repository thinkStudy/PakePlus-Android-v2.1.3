package com.app.pakeplus.ui.home

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import androidx.core.view.WindowCompat

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import android.Manifest
import android.content.pm.PackageManager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

class WebAppInterface(private val activity: Activity, private val webView: WebView) 
 {
    companion object {
        private const val TAG = "WebAppInterface"
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1003
        private const val PERMISSION_REQUEST_CAMERA = 1004
        private const val PERMISSION_REQUEST_STORAGE = 1005
    }

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun requestAudioPermission(): Boolean {
        return checkAndRequestPermission(
            Manifest.permission.RECORD_AUDIO,
            "需要麦克风权限",
            PERMISSION_REQUEST_RECORD_AUDIO
        )
    }

    @JavascriptInterface
    fun requestCameraPermission(): Boolean {
        return checkAndRequestPermission(
            Manifest.permission.CAMERA,
            "需要相机权限",
            PERMISSION_REQUEST_CAMERA
        )
    }

    @JavascriptInterface
    fun requestStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 使用更细粒度的权限
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                // Android 13
                Manifest.permission.READ_MEDIA_IMAGES
            }
        } else {
            // Android 12及以下
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        return checkAndRequestPermission(
            permission,
            "需要文件访问权限",
            PERMISSION_REQUEST_STORAGE
        )
    }

    @JavascriptInterface
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == 
               PackageManager.PERMISSION_GRANTED
    }

    @JavascriptInterface
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivity(intent)
    }

    // 关键注解：@JavascriptInterface 暴露方法给 JS
    @JavascriptInterface
    fun setStatusBarColor(colorHex: String) {
        // 必须在主线程（UI线程）中运行UI操作
        activity.runOnUiThread {
            val color = Color.parseColor(colorHex) // 将 #RRGGBB 字符串转为颜色值
            // 方法一：使用原生API（推荐，简洁）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.window.statusBarColor = color
            }
            
            // 方法二：使用AndroidX库（更强大，兼容性更好）
            // WindowCompat.setStatusBarColor(activity.window, color)

            // 根据颜色亮度，自动调整状态栏图标（黑白）
            //setStatusBarLightIcon(Color.luminance(color) > 0.5)
        }
    }

    @JavascriptInterface
    fun setStatusBarLightIcon(isLight: Boolean) {
        activity.runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.decorView.windowInsetsController
                if (isLight) {
                    controller?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            } else {
                var flags = window.decorView.systemUiVisibility
                flags = if (isLight) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                window.decorView.systemUiVisibility = flags
            }
            
        }
    }
     private val mainHandler = Handler(Looper.getMainLooper())

       
 private fun checkAndRequestPermission(
        permission: String, 
        rationale: String,
        requestCode: Int
    ): Boolean {
        return if (ContextCompat.checkSelfPermission(activity, permission) == 
                  PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                Toast.makeText(activity, rationale, Toast.LENGTH_LONG).show()
            }
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestCode
            )
            false
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """
            {
                "platform": "Android",
                "sdkVersion": ${Build.VERSION.SDK_INT},
                "manufacturer": "${Build.MANUFACTURER}",
                "model": "${Build.MODEL}",
                "product": "${Build.PRODUCT}"
            }
        """.trimIndent()
    }

    @JavascriptInterface
    fun callJavaScriptFunction(jsFunction: String) {
        activity.runOnUiThread {
            webView.evaluateJavascript(jsFunction, null)
        }
    }
}