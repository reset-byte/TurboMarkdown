package com.github.turbomarkwon.cache

import android.text.Spanned
import java.util.concurrent.ConcurrentHashMap

/**
 * 轻量级Markdown缓存
 * 只缓存渲染结果，不缓存完整的Node对象，减少内存占用
 */
object LightweightMarkdownCache {
    
    // 直接缓存渲染后的Spanned对象，避免重复缓存
    private val cache: ConcurrentHashMap<String, CacheEntry> = ConcurrentHashMap()
    
    /**
     * 缓存条目
     */
    private data class CacheEntry(
        val spanned: Spanned,
        val itemType: String,
        val lastAccessed: Long = System.currentTimeMillis()
    )
    
    private var cacheHitCount: Int = 0
    private var cacheMissCount: Int = 0
    
    /**
     * 获取缓存的渲染结果
     */
    fun getSpanned(cacheKey: String): Spanned? {
        val entry = cache[cacheKey]
        return if (entry != null && !isExpired(entry)) {
            cache[cacheKey] = entry.copy(lastAccessed = System.currentTimeMillis())
            cacheHitCount++
            entry.spanned
        } else {
            cache.remove(cacheKey)
            cacheMissCount++
            null
        }
    }
    
    /**
     * 缓存渲染结果
     */
    fun putSpanned(cacheKey: String, spanned: Spanned, itemType: String) {
        cache[cacheKey] = CacheEntry(spanned, itemType)
        cleanupExpiredEntries()
    }
    
    /**
     * 生成缓存键（基于内容哈希 + 类型）
     */
    fun generateCacheKey(content: String, itemType: String): String {
        return "${content.hashCode()}_${itemType}"
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cache.clear()
        cacheHitCount = 0
        cacheMissCount = 0
    }
    
    /**
     * 清除指定缓存
     */
    fun clearCache(cacheKey: String) {
        cache.remove(cacheKey)
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val totalRequests = cacheHitCount + cacheMissCount
        val hitRate = if (totalRequests > 0) {
            (cacheHitCount.toFloat() / totalRequests.toFloat()) * 100f
        } else {
            0f
        }
        
        return CacheStats(
            cacheSize = cache.size,
            hitCount = cacheHitCount,
            missCount = cacheMissCount,
            hitRate = hitRate,
            memoryEstimate = estimateMemoryUsage()
        )
    }
    
    /**
     * 估算内存使用量
     */
    private fun estimateMemoryUsage(): Long {
        // 估算每个Spanned对象的平均大小
        val avgSpannedSize = 2048L // 2KB per Spanned object (estimated)
        return cache.size * avgSpannedSize
    }
    
    /**
     * 检查条目是否过期
     */
    private fun isExpired(entry: CacheEntry): Boolean {
        val now = System.currentTimeMillis()
        return (now - entry.lastAccessed) > CACHE_EXPIRY_TIME
    }
    
    /**
     * 清理过期条目
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val iterator = cache.entries.iterator()
        var cleanedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((now - entry.value.lastAccessed) > CACHE_EXPIRY_TIME) {
                iterator.remove()
                cleanedCount++
            }
        }
        
        // 如果缓存太大，清理最少使用的条目
        if (cache.size > MAX_CACHE_SIZE) {
            val sortedEntries = cache.entries.sortedBy { it.value.lastAccessed }
            val toRemove = cache.size - MAX_CACHE_SIZE
            sortedEntries.take(toRemove).forEach { 
                cache.remove(it.key) 
            }
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Int = cache.size
    
    /**
     * 获取缓存键列表
     */
    fun getCacheKeys(): Set<String> = cache.keys.toSet()
    
    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val cacheSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val hitRate: Float,
        val memoryEstimate: Long
    )
    
    // 缓存过期时间：10分钟（减少内存占用）
    private const val CACHE_EXPIRY_TIME: Long = 10 * 60 * 1000L
    
    // 最大缓存大小：50个条目
    private const val MAX_CACHE_SIZE = 50
} 