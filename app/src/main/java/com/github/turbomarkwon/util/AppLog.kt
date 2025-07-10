package com.github.turbomarkwon.util

import android.util.Log

object AppLog {
    const val TAG = "yangtianbin"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    /**
     * 缓存统计日志
     */
    fun cache(message: String) {
        Log.d("$TAG-CACHE", message)
    }
} 