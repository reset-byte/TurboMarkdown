package com.github.turbomarkwon.util

import android.util.Log

object AppLog {
    const val TAG = "yangtianbin"

    fun d(message: String) {
        try {
            Log.d(TAG, message)
        } catch (e: RuntimeException) {
            // 在单元测试环境中，使用println代替Log
            println("D/$TAG: $message")
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        try {
            Log.e(TAG, message, throwable)
        } catch (e: RuntimeException) {
            // 在单元测试环境中，使用println代替Log
            println("E/$TAG: $message")
            throwable?.printStackTrace()
        }
    }

    /**
     * 缓存统计日志
     */
    fun cache(message: String) {
        try {
            Log.d("$TAG-CACHE", message)
        } catch (e: RuntimeException) {
            // 在单元测试环境中，使用println代替Log
            println("D/$TAG-CACHE: $message")
        }
    }
} 