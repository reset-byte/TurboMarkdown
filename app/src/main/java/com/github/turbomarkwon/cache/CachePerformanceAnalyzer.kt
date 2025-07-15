package com.github.turbomarkwon.cache

import com.github.turbomarkwon.util.AppLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * 缓存性能分析器 - 监控和优化缓存效果
 */
object CachePerformanceAnalyzer {

    // Entry 缓存统计
    private val entryCacheStats = ConcurrentHashMap<String, EntryCacheStats>()
    
    // 全局计数器
    private val totalRenderCount = AtomicLong(0)
    private val totalCacheHitCount = AtomicLong(0)
    private val totalRenderTimeMs = AtomicLong(0)
    
    // 内存监控
    private var lastMemoryCheck = 0L
    private const val memoryCheckInterval = 5000L // 5秒检查一次
    
    /**
     * Entry缓存统计信息
     */
    data class EntryCacheStats(
        val entryType: String,
        val hitCount: AtomicInteger = AtomicInteger(0),
        val missCount: AtomicInteger = AtomicInteger(0),
        val totalRenderTimeMs: AtomicLong = AtomicLong(0),
        val cacheSize: AtomicInteger = AtomicInteger(0)
    ) {
        val hitRate: Double
            get() {
                val total = hitCount.get() + missCount.get()
                return if (total > 0) (hitCount.get().toDouble() / total) * 100 else 0.0
            }
            
        val averageRenderTime: Double
            get() {
                val total = hitCount.get() + missCount.get()
                return if (total > 0) totalRenderTimeMs.get().toDouble() / total else 0.0
            }
    }
    
    /**
     * 记录缓存命中
     */
    fun recordCacheHit(entryType: String) {
        val stats = entryCacheStats.getOrPut(entryType) { EntryCacheStats(entryType) }
        stats.hitCount.incrementAndGet()
        totalCacheHitCount.incrementAndGet()
        
        AppLog.d("CachePerformanceAnalyzer: $entryType 缓存命中，命中率: ${stats.hitRate.toInt()}%")
    }
    
    /**
     * 记录缓存未命中
     */
    fun recordCacheMiss(entryType: String, renderTimeMs: Long = 0) {
        val stats = entryCacheStats.getOrPut(entryType) { EntryCacheStats(entryType) }
        stats.missCount.incrementAndGet()
        stats.totalRenderTimeMs.addAndGet(renderTimeMs)
        totalRenderCount.incrementAndGet()
        totalRenderTimeMs.addAndGet(renderTimeMs)
        
        AppLog.d("CachePerformanceAnalyzer: $entryType 缓存未命中，渲染耗时: ${renderTimeMs}ms")
    }
    
    /**
     * 更新缓存大小
     */
    fun updateCacheSize(entryType: String, size: Int) {
        val stats = entryCacheStats.getOrPut(entryType) { EntryCacheStats(entryType) }
        stats.cacheSize.set(size)
    }
    
    /**
     * 生成性能报告
     */
    fun generatePerformanceReport(): String {
        val report = StringBuilder()
        report.append("=== Entry缓存性能报告 ===\n")
        report.append("总渲染次数: ${totalRenderCount.get()}\n")
        report.append("总缓存命中次数: ${totalCacheHitCount.get()}\n")
        
        val globalHitRate = if (totalRenderCount.get() > 0) {
            (totalCacheHitCount.get().toDouble() / (totalRenderCount.get() + totalCacheHitCount.get())) * 100
        } else 0.0
        report.append("全局缓存命中率: ${globalHitRate.toInt()}%\n")
        
        val averageRenderTime = if (totalRenderCount.get() > 0) {
            totalRenderTimeMs.get().toDouble() / totalRenderCount.get()
        } else 0.0
        report.append("平均渲染时间: ${averageRenderTime.toInt()}ms\n\n")
        
        entryCacheStats.values.forEach { stats ->
            report.append("--- ${stats.entryType} ---\n")
            report.append("命中率: ${stats.hitRate.toInt()}%\n")
            report.append("命中次数: ${stats.hitCount.get()}\n")
            report.append("未命中次数: ${stats.missCount.get()}\n")
            report.append("缓存大小: ${stats.cacheSize.get()}\n")
            report.append("平均渲染时间: ${stats.averageRenderTime.toInt()}ms\n\n")
        }
        
        return report.toString()
    }
    
    /**
     * 处理低内存情况
     */
    fun handleLowMemory() {
        AppLog.d("CachePerformanceAnalyzer: 处理低内存情况")
        
        // 记录当前状态
        logPerformanceDetails()
        
        // 智能缓存清理
        performSmartCacheCleanup()
        
        // 清理语法高亮缓存
        com.github.turbomarkwon.views.CodeDisplayView.clearSyntaxCache()
        
        // 清理 Mermaid 渲染缓存
        MermaidRenderCache.smartCleanup()
        
        AppLog.d("CachePerformanceAnalyzer: 低内存清理完成")
    }
    
    /**
     * 修整缓存
     */
    fun trimCaches() {
        AppLog.d("CachePerformanceAnalyzer: 执行缓存修整")
        
        // 清理低命中率的缓存
        performSmartCacheCleanup()
        
        // 部分清理其他缓存
        MermaidRenderCache.trimCache()
    }
    
    /**
     * 清除所有统计数据
     */
    fun clearAll() {
        entryCacheStats.clear()
        totalRenderCount.set(0)
        totalCacheHitCount.set(0)
        totalRenderTimeMs.set(0)
        
        // 清理所有缓存
        MermaidRenderCache.clearAll()
        com.github.turbomarkwon.views.CodeDisplayView.clearSyntaxCache()
        
        AppLog.d("CachePerformanceAnalyzer: 已清除所有缓存和统计数据")
    }
    
    /**
     * 智能缓存清理 - 清理低效的缓存
     */
    private fun performSmartCacheCleanup() {
        AppLog.d("CachePerformanceAnalyzer: 开始智能缓存清理")
        
        entryCacheStats.values.forEach { stats ->
            // 如果命中率低于30%，建议清理
            if (stats.hitRate < 30.0 && stats.cacheSize.get() > 10) {
                AppLog.d("CachePerformanceAnalyzer: ${stats.entryType} 缓存命中率过低(${stats.hitRate.toInt()}%)，建议清理")
            }
            
            // 如果缓存过大，建议清理
            if (stats.cacheSize.get() > 100) {
                AppLog.d("CachePerformanceAnalyzer: ${stats.entryType} 缓存过大(${stats.cacheSize.get()})，建议清理")
            }
                 }
    }
    
    /**
     * 记录性能详情
     */
    fun logPerformanceDetails() {
        val report = generatePerformanceReport()
        AppLog.d("CachePerformanceAnalyzer Performance Report:\n$report")
        
        // 检查内存使用
        checkMemoryUsage()
    }
    
    /**
     * 检查内存使用
     */
    private fun checkMemoryUsage() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMemoryCheck < memoryCheckInterval) {
            return
        }
        lastMemoryCheck = currentTime
        
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory) * 100
        
        AppLog.d("CachePerformanceAnalyzer: 内存使用率: ${memoryUsagePercent.toInt()}%")
        AppLog.d("CachePerformanceAnalyzer: 已用内存: ${usedMemory / 1024 / 1024}MB")
        AppLog.d("CachePerformanceAnalyzer: 最大内存: ${maxMemory / 1024 / 1024}MB")
        
        // 如果内存使用率超过80%，触发智能清理
        if (memoryUsagePercent > 80) {
            AppLog.d("CachePerformanceAnalyzer: 内存使用率过高(${memoryUsagePercent.toInt()}%)，触发智能清理")
            performSmartCacheCleanup()
        }
    }
    
    // 保留原有的其他方法以确保兼容性
    
    private var lastParseTime = 0L
    private var parseTimeSum = 0L
    private var parseCount = 0
    private var lastMemorySnapshot = 0L
    
    /**
     * 记录解析时间
     */
    fun recordParseTime(timeMs: Long) {
        lastParseTime = timeMs
        parseTimeSum += timeMs
        parseCount++
    }
    
    /**
     * 拍摄内存快照
     */
    fun takeMemorySnapshot() {
        val runtime = Runtime.getRuntime()
        lastMemorySnapshot = runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "totalRenderCount" to totalRenderCount.get(),
            "totalCacheHitCount" to totalCacheHitCount.get(),
            "globalHitRate" to if (totalRenderCount.get() > 0) {
                (totalCacheHitCount.get().toDouble() / (totalRenderCount.get() + totalCacheHitCount.get())) * 100
            } else 0.0,
            "averageParseTime" to if (parseCount > 0) parseTimeSum / parseCount else 0L,
            "lastMemoryUsage" to lastMemorySnapshot / 1024 / 1024, // MB
            "entryCacheCount" to entryCacheStats.size
        )
    }
} 