package com.app.pakeplus

import org.json.JSONObject

/**
 * 菜单配置数据模型
 */
data class MenuItem(
    val name: String,
    val label: String,
    val url: String,
    val needReload: Boolean = false,
    val needBlock: Boolean = false,
    val blockHeight: Int = 0,
    val needModal: Boolean = false,
    val modalTitle: String = "",
    val modalMsg: String = "",
    val replace: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("label", label)
            put("url", url)
            put("needReload", needReload)
            put("needBlock", needBlock)
            put("blockHeight", blockHeight)
            put("needModal", needModal)
            put("modalTitle", modalTitle)
            put("modalMsg", modalMsg)
            put("replace", replace)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): MenuItem {
            return MenuItem(
                name = json.optString("name", ""),
                label = json.optString("label", ""),
                url = json.optString("url", ""),
                needReload = json.optBoolean("needReload", false),
                needBlock = json.optBoolean("needBlock", false),
                blockHeight = json.optInt("blockHeight", 0),
                needModal = json.optBoolean("needModal", false),
                modalTitle = json.optString("modalTitle", ""),
                modalMsg = json.optString("modalMsg", ""),
                replace = json.optBoolean("replace", false)
            )
        }
    }
}

data class AppConfig(
    val downloadUrl: String = "",
    val menuItems: List<MenuItem> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("downloadUrl", downloadUrl)
            put("menuItems", org.json.JSONArray().apply {
                menuItems.forEach { put(it.toJson()) }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppConfig {
            val items = mutableListOf<MenuItem>()
            val arr = json.optJSONArray("menuItems")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    items.add(MenuItem.fromJson(arr.getJSONObject(i)))
                }
            }
            return AppConfig(
                downloadUrl = json.optString("downloadUrl", ""),
                menuItems = items
            )
        }
    }
}
