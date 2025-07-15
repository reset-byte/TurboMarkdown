package com.github.turbomarkwon.cache

import android.util.LruCache
import com.github.turbomarkwon.util.AppLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局 Mermaid 渲染缓存管理器
 * 用于缓存已渲染的 Mermaid 图表内容和渲染状态
 */
object MermaidRenderCache {
    
    // 渲染状态缓存
    private val renderStateCache = ConcurrentHashMap<String, MermaidRenderState>()
    
    // 渲染内容缓存 - 使用 LruCache 自动管理内存
    private val renderContentCache = LruCache<String, String>(50) // 最多缓存 50 个渲染结果
    
    // 统计信息
    private var cacheHits = 0
    private var cacheMisses = 0
    private var totalRequests = 0
    
    /**
     * Mermaid 渲染状态
     */
    enum class MermaidRenderState {
        NONE,           // 未开始渲染
        RENDERING,      // 正在渲染
        SUCCESS,        // 渲染成功
        ERROR          // 渲染失败
    }
    
    /**
     * 生成缓存键
     */
    fun generateCacheKey(content: String): String {
        return "mermaid_${content.hashCode()}"
    }
    
    /**
     * 检查是否已缓存渲染状态
     */
    fun isRendered(cacheKey: String): Boolean {
        totalRequests++
        return renderStateCache[cacheKey] == MermaidRenderState.SUCCESS
    }
    
    /**
     * 获取渲染状态
     */
    fun getRenderState(cacheKey: String): MermaidRenderState {
        return renderStateCache[cacheKey] ?: MermaidRenderState.NONE
    }
    
    /**
     * 设置渲染状态
     */
    fun setRenderState(cacheKey: String, state: MermaidRenderState) {
        renderStateCache[cacheKey] = state
        AppLog.d("MermaidRenderCache: Set render state for $cacheKey to $state")
    }
    
    /**
     * 缓存渲染结果
     */
    fun cacheRenderResult(cacheKey: String, renderedContent: String) {
        renderContentCache.put(cacheKey, renderedContent)
        renderStateCache[cacheKey] = MermaidRenderState.SUCCESS
        AppLog.d("MermaidRenderCache: Cached render result for $cacheKey")
    }
    
    /**
     * 获取缓存的渲染结果
     */
    fun getCachedRenderResult(cacheKey: String): String? {
        val result = renderContentCache.get(cacheKey)
        if (result != null) {
            cacheHits++
            AppLog.d("MermaidRenderCache: Cache hit for $cacheKey")
        } else {
            cacheMisses++
            AppLog.d("MermaidRenderCache: Cache miss for $cacheKey")
        }
        return result
    }
    
    /**
     * 标记渲染开始
     */
    fun markRenderingStart(cacheKey: String) {
        renderStateCache[cacheKey] = MermaidRenderState.RENDERING
        AppLog.d("MermaidRenderCache: Started rendering for $cacheKey")
    }
    
    /**
     * 标记渲染成功
     */
    fun markRenderingSuccess(cacheKey: String) {
        renderStateCache[cacheKey] = MermaidRenderState.SUCCESS
        AppLog.d("MermaidRenderCache: Rendering success for $cacheKey")
    }
    
    /**
     * 标记渲染失败
     */
    fun markRenderingError(cacheKey: String) {
        renderStateCache[cacheKey] = MermaidRenderState.ERROR
        AppLog.d("MermaidRenderCache: Rendering error for $cacheKey")
    }
    
    /**
     * 检查是否正在渲染
     */
    fun isRendering(cacheKey: String): Boolean {
        return renderStateCache[cacheKey] == MermaidRenderState.RENDERING
    }
    
    /**
     * 移除缓存项
     */
    fun removeCache(cacheKey: String) {
        renderStateCache.remove(cacheKey)
        renderContentCache.remove(cacheKey)
        AppLog.d("MermaidRenderCache: Removed cache for $cacheKey")
    }
    
    /**
     * 清理所有缓存
     */
    fun clearAll() {
        val stateSize = renderStateCache.size
        val contentSize = renderContentCache.size()
        
        renderStateCache.clear()
        renderContentCache.evictAll()
        
        AppLog.d("MermaidRenderCache: Cleared all cache - states: $stateSize, content: $contentSize")
    }
    
    /**
     * 智能清理缓存 - 保留最近使用的缓存
     */
    fun smartCleanup() {
        val beforeStateSize = renderStateCache.size
        val beforeContentSize = renderContentCache.size()
        
        // 移除渲染失败的状态缓存
        val failedKeys = renderStateCache.entries
            .filter { it.value == MermaidRenderState.ERROR }
            .map { it.key }
        
        failedKeys.forEach { key ->
            renderStateCache.remove(key)
            renderContentCache.remove(key)
        }
        
        // LruCache 会自动清理最少使用的内容
        renderContentCache.trimToSize(renderContentCache.maxSize() / 2)
        
        val afterStateSize = renderStateCache.size
        val afterContentSize = renderContentCache.size()
        
        AppLog.d("MermaidRenderCache: Smart cleanup - " +
                "states: $beforeStateSize -> $afterStateSize, " +
                "content: $beforeContentSize -> $afterContentSize")
    }
    
    /**
     * 修整缓存 - 清理部分缓存以释放内存
     */
    fun trimCache() {
        val beforeStateSize = renderStateCache.size
        val beforeContentSize = renderContentCache.size()
        
        // 清理部分状态缓存，保留最重要的
        renderContentCache.trimToSize(renderContentCache.maxSize() / 3)
        
        // 清理一些错误状态的缓存
        val errorKeys = renderStateCache.entries
            .filter { it.value == MermaidRenderState.ERROR }
            .map { it.key }
            .take(renderStateCache.size / 2)
        
        errorKeys.forEach { key ->
            renderStateCache.remove(key)
        }
        
        val afterStateSize = renderStateCache.size
        val afterContentSize = renderContentCache.size()
        
        AppLog.d("MermaidRenderCache: Trim cache - " +
                "states: $beforeStateSize -> $afterStateSize, " +
                "content: $beforeContentSize -> $afterContentSize")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        val hitRate = if (totalRequests > 0) {
            (cacheHits * 100.0 / totalRequests).toInt()
        } else {
            0
        }
        
        return mapOf(
            "cache_hits" to cacheHits,
            "cache_misses" to cacheMisses,
            "total_requests" to totalRequests,
            "hit_rate" to "$hitRate%",
            "state_cache_size" to renderStateCache.size,
            "content_cache_size" to renderContentCache.size(),
            "content_cache_max_size" to renderContentCache.maxSize()
        )
    }
    
    /**
     * 重置统计信息
     */
    fun resetStats() {
        cacheHits = 0
        cacheMisses = 0
        totalRequests = 0
        AppLog.d("MermaidRenderCache: Reset statistics")
    }
    
    /**
     * 记录缓存性能日志
     */
    fun logCachePerformance() {
        val stats = getCacheStats()
        AppLog.d("MermaidRenderCache Performance:")
        AppLog.d("  - Cache hits: ${stats["cache_hits"]}")
        AppLog.d("  - Cache misses: ${stats["cache_misses"]}")
        AppLog.d("  - Hit rate: ${stats["hit_rate"]}")
        AppLog.d("  - State cache size: ${stats["state_cache_size"]}")
        AppLog.d("  - Content cache size: ${stats["content_cache_size"]}")
    }
} 