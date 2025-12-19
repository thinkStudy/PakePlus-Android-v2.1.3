package com.app.pakeplus.ui.home

import android.app.Activity
import android.graphics.Color
import android.os.Build
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

class WebAppInterface(private val activity: Activity, private val webView: WebView) 
 {

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
            val window = activity.window
            var flags = window.decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isLight) {
                    // 浅色背景 -> 使用深色图标（黑色）
                   flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    // 深色背景 -> 使用浅色图标（白色）
                   flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                window.decorView.systemUiVisibility = flags
            }
        }
    }
     private val mainHandler = Handler(Looper.getMainLooper())

        /**
         * 通用权限检查方法
         */
        @JavascriptInterface
        fun checkPermission(permissionType: String): String {
            return try {
                val permission = when (permissionType) {
                    "microphone" -> Manifest.permission.RECORD_AUDIO
                    "camera" -> Manifest.permission.CAMERA
                    "storage" -> Manifest.permission.READ_EXTERNAL_STORAGE
                    "folder" -> READ_MEDIA_VISUAL_USER_SELECTED
                    else -> return createResponse(false, "未知权限类型")
                }

                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                createResponse(granted, if (granted) "权限已授予" else "权限未授予")
            } catch (e: Exception) {
                createResponse(false, "检查权限时出错: ${e.message}")
            }
        }

        /**
         * 请求录音权限（示例）
         * 网页调用：window.AndroidBridge.requestRecordPermission(callback)
         */
        @JavascriptInterface
        fun requestRecordPermission(callbackFunc: String) {
            requestPermission(
                Manifest.permission.RECORD_AUDIO,
                PERMISSION_REQUEST_RECORD_AUDIO,
                callbackFunc,
                "录音"
            )
        }

        /**
         * 请求相机权限（示例）
         */
        @JavascriptInterface
        fun requestCameraPermission(callbackFunc: String) {
            requestPermission(
                Manifest.permission.CAMERA,
                PERMISSION_REQUEST_CAMERA,
                callbackFunc,
                "相机"
            )
        }
        /**
         * 请求文件夹权限（示例）
         */
        @JavascriptInterface
        fun requestFolderPermission(callbackFunc: String) {
            requestPermission(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                PERMISSION_REQUEST_READ_MEDIA_VISUAL_USER_SELECTED,
                callbackFunc,
                "文件夹"
            )
        }

        /**
         * 通用的权限请求方法（内部）
         */
        private fun requestPermission(permission: String, requestCode: Int, callbackFunc: String, permissionName: String) {
            mainHandler.post {
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    // 已有权限，直接回调成功
                    callJsCallback(callbackFunc, true, "$permissionName 权限已存在")
                } else {
                    // 没有权限，需要申请。这里简化处理，实际应保存callback并关联requestCode
                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(context as androidx.appcompat.app.AppCompatActivity, permission)
                    if (shouldShowRationale) {
                        // 可以解释为什么需要权限，然后申请
                        callJsCallback(callbackFunc, false, "需要$permissionName权限来完成操作，请授权")
                    }
                    // 发起系统权限申请
                    ActivityCompat.requestPermissions(
                        context as androidx.appcompat.app.AppCompatActivity,
                        arrayOf(permission),
                        requestCode
                    )
                    // 注意：真实的授权结果需要在 onRequestPermissionsResult 中捕获，
                    // 并通过某种方式（如广播、接口回调）关联并执行 callbackFunc。
                    // 此处为简化示例，实际需要更复杂的状态管理。
                }
            }
        }

       

        /**
         * 创建一个标准的 JSON 响应
         */
        private fun createResponse(success: Boolean, message: String): String {
            return JSONObject().apply {
                put("success", success)
                put("message", message)
                // 可以添加更多字段，如 data, code 等
            }.toString()
        }

        /**
         * 在 WebView 主线程中调用 JavaScript 回调函数
         */
        private fun callJsCallback(callbackFunc: String, success: Boolean, message: String) {
            mainHandler.post {
                // 确保回调函数名是安全的
                val safeCallbackFunc = callbackFunc.replace(Regex("[^a-zA-Z0-9_\$]"), "")
                if (safeCallbackFunc.isNotEmpty()) {
                    val jsonResponse = createResponse(success, message)
                    // 注意：JSON字符串需要转义后嵌入JS代码
                    val escapedJson = jsonResponse.replace("'", "\\'").replace("\n", "\\n")
                    val jsCode = "if (window.$safeCallbackFunc) { window.$safeCallbackFunc('$escapedJson'); }"
                    webView.evaluateJavascript(jsCode, null)
                }
            }
        }
}