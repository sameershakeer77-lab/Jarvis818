package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)

object AppDiscoveryHelper {
    private const val TAG = "AppDiscoveryHelper"

    // Comprehensive directory of common Android applications and package IDs
    val KNOWN_APPS = listOf(
        // Messaging & Social
        InstalledAppInfo("WhatsApp", "com.whatsapp"),
        InstalledAppInfo("WhatsApp Business", "com.whatsapp.w4b"),
        InstalledAppInfo("Telegram", "org.telegram.messenger"),
        InstalledAppInfo("Instagram", "com.instagram.android"),
        InstalledAppInfo("Snapchat", "com.snapchat.android"),
        InstalledAppInfo("Twitter / X", "com.twitter.android"),
        InstalledAppInfo("Facebook", "com.facebook.katana"),
        InstalledAppInfo("Messenger", "com.facebook.orca"),
        InstalledAppInfo("LinkedIn", "com.linkedin.android"),
        InstalledAppInfo("Reddit", "com.reddit.frontpage"),
        InstalledAppInfo("Discord", "com.discord"),

        // Media & Video
        InstalledAppInfo("YouTube", "com.google.android.youtube"),
        InstalledAppInfo("YouTube Music", "com.google.android.apps.youtube.music"),
        InstalledAppInfo("Spotify", "com.spotify.music"),
        InstalledAppInfo("Netflix", "com.netflix.mediaclient"),
        InstalledAppInfo("Prime Video", "com.amazon.avod.thirdpartyclient"),
        InstalledAppInfo("Hotstar", "in.startv.hotstar"),

        // Google Core & System
        InstalledAppInfo("Google Chrome", "com.android.chrome"),
        InstalledAppInfo("Google Play Store", "com.android.vending"),
        InstalledAppInfo("Gmail", "com.google.android.gm"),
        InstalledAppInfo("Google Maps", "com.google.android.apps.maps"),
        InstalledAppInfo("Google Photos", "com.google.android.apps.photos"),
        InstalledAppInfo("Google Drive", "com.google.android.apps.docs"),
        InstalledAppInfo("Google Keep Notes", "com.google.android.keep"),
        InstalledAppInfo("Files by Google", "com.google.android.apps.nbu.files"),

        // Device Utilities & OEM Apps (Vivo / Samsung / Pixel / Xiaomi)
        InstalledAppInfo("Camera", "com.android.camera"),
        InstalledAppInfo("Vivo Camera", "com.vivo.camera"),
        InstalledAppInfo("Google Camera", "com.google.android.GoogleCamera"),
        InstalledAppInfo("Calculator", "com.google.android.calculator"),
        InstalledAppInfo("Vivo Calculator", "com.vivo.calculator"),
        InstalledAppInfo("AOSP Calculator", "com.android.calculator2"),
        InstalledAppInfo("Clock / Alarm", "com.google.android.deskclock"),
        InstalledAppInfo("Vivo Clock", "com.vivo.deskclock"),
        InstalledAppInfo("Phone / Dialer", "com.google.android.dialer"),
        InstalledAppInfo("Vivo Dialer", "com.vivo.dialer"),
        InstalledAppInfo("Settings", "com.android.settings"),
        InstalledAppInfo("Messages / SMS", "com.google.android.apps.messaging"),

        // Shopping & Payments
        InstalledAppInfo("Amazon", "in.amazon.mShop.android.shopping"),
        InstalledAppInfo("Flipkart", "com.flipkart.android"),
        InstalledAppInfo("Google Pay", "com.google.android.apps.nbu.paisa.user"),
        InstalledAppInfo("PhonePe", "com.phonepe.app"),
        InstalledAppInfo("Paytm", "net.one97.paytm"),
        InstalledAppInfo("Swiggy", "in.swiggy.android"),
        InstalledAppInfo("Zomato", "com.application.zomato")
    )

    private var cachedInstalledApps: List<InstalledAppInfo>? = null

    /**
     * Scans all physically installed launchable apps on the Android device.
     */
    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        cachedInstalledApps?.let { return it }

        val apps = mutableListOf<InstalledAppInfo>()
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                val appLabel = ri.loadLabel(pm).toString()
                apps.add(InstalledAppInfo(appLabel, pkgName))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying installed apps", e)
        }

        if (apps.isEmpty()) {
            apps.addAll(KNOWN_APPS)
        }

        cachedInstalledApps = apps
        return apps
    }

    /**
     * Resolves app by name or keyword with priority:
     * 1. Exact or partial match on device installed apps
     * 2. Known apps list match
     * 3. Fallback to package heuristic
     */
    fun resolveApp(context: Context, query: String): InstalledAppInfo {
        val cleanQuery = query.lowercase().trim()
            .replace("open ", "")
            .replace("launch ", "")
            .replace("start ", "")
            .trim()

        val installed = getInstalledApps(context)

        // 1. Direct keyword mapping
        when {
            cleanQuery.contains("whatsapp") || cleanQuery.contains("whats") || cleanQuery.contains("wp") -> {
                val found = installed.firstOrNull { it.packageName.contains("whatsapp") }
                return found ?: InstalledAppInfo("WhatsApp", "com.whatsapp")
            }
            cleanQuery.contains("youtube") || cleanQuery.contains("yt") -> {
                val found = installed.firstOrNull { it.packageName.contains("youtube") }
                return found ?: InstalledAppInfo("YouTube", "com.google.android.youtube")
            }
            cleanQuery.contains("telegram") || cleanQuery.contains("tele") -> {
                val found = installed.firstOrNull { it.packageName.contains("telegram") }
                return found ?: InstalledAppInfo("Telegram", "org.telegram.messenger")
            }
            cleanQuery.contains("instagram") || cleanQuery.contains("insta") -> {
                val found = installed.firstOrNull { it.packageName.contains("instagram") }
                return found ?: InstalledAppInfo("Instagram", "com.instagram.android")
            }
            cleanQuery.contains("calc") -> {
                val found = installed.firstOrNull { it.packageName.contains("calculator") }
                return found ?: InstalledAppInfo("Calculator", "com.google.android.calculator")
            }
            cleanQuery.contains("camera") || cleanQuery.contains("cam") || cleanQuery.contains("photo") -> {
                val found = installed.firstOrNull { it.packageName.contains("camera") }
                return found ?: InstalledAppInfo("Camera", "com.android.camera")
            }
            cleanQuery.contains("setting") -> {
                val found = installed.firstOrNull { it.packageName.contains("settings") }
                return found ?: InstalledAppInfo("Settings", "com.android.settings")
            }
            cleanQuery.contains("phone") || cleanQuery.contains("dialer") || cleanQuery.contains("call") -> {
                val found = installed.firstOrNull { it.packageName.contains("dialer") }
                return found ?: InstalledAppInfo("Phone", "com.google.android.dialer")
            }
            cleanQuery.contains("chrome") || cleanQuery.contains("browser") -> {
                val found = installed.firstOrNull { it.packageName.contains("chrome") }
                return found ?: InstalledAppInfo("Google Chrome", "com.android.chrome")
            }
            cleanQuery.contains("download") || cleanQuery.contains("install") || cleanQuery.contains("play store") || cleanQuery.contains("playstore") || (cleanQuery.contains("store") && !cleanQuery.contains("apple")) -> {
                val found = installed.firstOrNull { it.packageName.contains("vending") }
                return found ?: InstalledAppInfo("Google Play Store", "com.android.vending")
            }
            cleanQuery.contains("spotify") || cleanQuery.contains("music") -> {
                val found = installed.firstOrNull { it.packageName.contains("spotify") }
                return found ?: InstalledAppInfo("Spotify", "com.spotify.music")
            }
            cleanQuery.contains("clock") || cleanQuery.contains("alarm") -> {
                val found = installed.firstOrNull { it.packageName.contains("clock") }
                return found ?: InstalledAppInfo("Clock", "com.google.android.deskclock")
            }
            cleanQuery.contains("gmail") || cleanQuery.contains("mail") || cleanQuery.contains("email") -> {
                val found = installed.firstOrNull { it.packageName.contains("gm") }
                return found ?: InstalledAppInfo("Gmail", "com.google.android.gm")
            }
            cleanQuery.contains("map") -> {
                val found = installed.firstOrNull { it.packageName.contains("maps") }
                return found ?: InstalledAppInfo("Google Maps", "com.google.android.apps.maps")
            }
        }

        // 2. Check installed apps on device for matching label
        val matchedInstalled = installed.firstOrNull {
            it.appName.lowercase().contains(cleanQuery) || cleanQuery.contains(it.appName.lowercase())
        }
        if (matchedInstalled != null) {
            return matchedInstalled
        }

        // 3. Check known apps
        val matchedKnown = KNOWN_APPS.firstOrNull {
            it.appName.lowercase().contains(cleanQuery) || cleanQuery.contains(it.appName.lowercase())
        }
        if (matchedKnown != null) {
            return matchedKnown
        }

        // 4. Default: Return generic app using query as name
        val capitalName = cleanQuery.replaceFirstChar { it.uppercase() }
        return InstalledAppInfo(capitalName, "com.android.$cleanQuery")
    }
}
