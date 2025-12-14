package com.app.pakeplus.ui.home

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import androidx.core.view.WindowCompat

class WebAppInterface(private val activity: Activity) {

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
}