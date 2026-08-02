package com.example.zaloauto.service.accessibility

import android.content.Context
import android.content.pm.PackageManager

/**
 * Maps Zalo version to UI element identifiers.
 * Element IDs change between Zalo versions; this mapping allows graceful degradation.
 */
object ZaloElementIds {

    data class Ids(
        val searchIconDesc: String,
        val searchInputHint: String,
        val chatInputHint: String,
        val sendIconDesc: String,
        val searchInputResourceId: String? = null,
        val chatInputResourceId: String? = null,
        val sendButtonResourceId: String? = null
    )

    // Zalo version -> element IDs.
    // Extend this map as new Zalo versions are tested.
    private val versionMap = mapOf(
        "8.0" to Ids(
            searchIconDesc = "T\u00ecm ki\u1ebfm",  // "Tim kiem" in Vietnamese
            searchInputHint = "T\u00ecm ki\u1ebfm",
            chatInputHint = "Nh\u1eadp tin nh\u1eafn",
            sendIconDesc = "G\u1eedi",
            searchInputResourceId = "com.zing.zalo:id/edtSearch",
            chatInputResourceId = "com.zing.zalo:id/chat_input",
            sendButtonResourceId = "com.zing.zalo:id/btn_send"
        )
    )

    /**
     * Returns element IDs for the currently installed Zalo version.
     * Falls back to the first entry in the map if version is unknown.
     */
    fun forInstalledVersion(context: Context): Ids {
        return try {
            val info = context.packageManager.getPackageInfo(
                "com.zing.zalo", 0
            )
            val version = info.versionName ?: "unknown"
            val majorMinor = version.split(".").take(2).joinToString(".")
            versionMap[majorMinor] ?: versionMap.values.first()
        } catch (e: PackageManager.NameNotFoundException) {
            versionMap.values.first()
        }
    }
}
