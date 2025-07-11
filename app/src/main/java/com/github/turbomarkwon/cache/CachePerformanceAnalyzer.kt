package com.github.turbomarkwon.cache

import com.github.turbomarkwon.util.AppLog
import kotlin.system.measureTimeMillis

/**
 * 缓存性能分析器
 * 用于监控和分析缓存系统的性能影响
 */
object CachePerformanceAnalyzer {
    
    private var totalParseTime: Long = 0
    private var totalRenderTime: Long = 0
    private var parseOperations: Int = 0
    private var renderOperations: Int = 0
    private var memorySnapshots = mutableListOf<MemorySnapshot>()
    
    /**
     * 内存快照
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemory: Long,
        val cacheSize: Int,
        val cacheMemoryEstimate: Long
    )
    
    /**
     * 性能报告
     */
    data class PerformanceReport(
        val avgParseTime: Long,
        val avgRenderTime: Long,
        val totalOperations: Int,
        val memoryEfficiency: Float,
        val cacheEffectiveness: Float,
        val recommendations: List<String>
    )
    
    /**
     * 测量解析性能
     */
    fun <T> measureParseTime(operation: () -> T): T {
        val result: T
        val time = measureTimeMillis {
            result = operation()
        }
        
        totalParseTime += time
        parseOperations++
        
        AppLog.d("Parse time: ${time}ms")
        return result
    }
    
    /**
     * 记录解析时间（用于suspend函数）
     */
    fun recordParseTime(timeMs: Long) {
        totalParseTime += timeMs
        parseOperations++
        AppLog.d("Parse time: ${timeMs}ms")
    }
    
    /**
     * 测量渲染性能
     */
    fun <T> measureRenderTime(operation: () -> T): T {
        val result: T
        val time = measureTimeMillis {
            result = operation()
        }
        
        totalRenderTime += time
        renderOperations++
        
        AppLog.d("Render time: ${time}ms")
        return result
    }
    
    /**
     * 拍摄内存快照
     */
    fun takeMemorySnapshot() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val lightweightCacheStats = LightweightMarkdownCache.getCacheStats()
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            cacheSize = lightweightCacheStats.cacheSize,
            cacheMemoryEstimate = lightweightCacheStats.memoryEstimate
        )
        
        memorySnapshots.add(snapshot)
        
        // 只保留最近的10个快照
        if (memorySnapshots.size > 10) {
            memorySnapshots.removeAt(0)
        }
        
        AppLog.d("Memory snapshot: Used=${usedMemory/1024/1024}MB, Cache=${snapshot.cacheSize}")
    }
    
    /**
     * 生成性能报告
     */
    fun generateReport(): PerformanceReport {
        val avgParseTime = if (parseOperations > 0) totalParseTime / parseOperations else 0
        val avgRenderTime = if (renderOperations > 0) totalRenderTime / renderOperations else 0
        
        val lightweightCacheStats = LightweightMarkdownCache.getCacheStats()
        
        // 计算内存效率
        val memoryEfficiency = if (memorySnapshots.isNotEmpty()) {
            val latestSnapshot = memorySnapshots.last()
            val cacheMemoryRatio = latestSnapshot.cacheMemoryEstimate.toFloat() / latestSnapshot.usedMemory.toFloat()
            (1.0f - cacheMemoryRatio) * 100f
        } else {
            0f
        }
        
        // 计算缓存效果（只使用轻量级缓存）
        val cacheEffectiveness = lightweightCacheStats.hitRate
        
        // 生成建议
        val recommendations = generateRecommendations(
            avgParseTime,
            avgRenderTime,
            memoryEfficiency,
            cacheEffectiveness,
            lightweightCacheStats
        )
        
        return PerformanceReport(
            avgParseTime = avgParseTime,
            avgRenderTime = avgRenderTime,
            totalOperations = parseOperations + renderOperations,
            memoryEfficiency = memoryEfficiency,
            cacheEffectiveness = cacheEffectiveness,
            recommendations = recommendations
        )
    }
    
    /**
     * 生成优化建议
     */
    private fun generateRecommendations(
        avgParseTime: Long,
        avgRenderTime: Long,
        memoryEfficiency: Float,
        cacheEffectiveness: Float,
        lightweightCacheStats: LightweightMarkdownCache.CacheStats
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 解析性能建议
        if (avgParseTime > 100) {
            recommendations.add("解析时间较长(${avgParseTime}ms)，建议优化解析逻辑或使用更多缓存")
        }
        
        // 渲染性能建议
        if (avgRenderTime > 50) {
            recommendations.add("渲染时间较长(${avgRenderTime}ms)，建议优化渲染逻辑")
        }
        
        // 内存效率建议
        if (memoryEfficiency < 70) {
            recommendations.add("内存效率较低(${String.format("%.1f", memoryEfficiency)}%)，建议清理缓存或减少缓存大小")
        }
        
        // 缓存效果建议
        if (cacheEffectiveness < 60) {
            recommendations.add("缓存命中率较低(${String.format("%.1f", cacheEffectiveness)}%)，建议调整缓存策略")
        }
        
        // 缓存大小建议
        if (lightweightCacheStats.cacheSize > 50) {
            recommendations.add("缓存过大(${lightweightCacheStats.cacheSize}项)，建议减少缓存大小或增加清理频率")
        }
        
        // 内存使用建议
        if (lightweightCacheStats.memoryEstimate > 10 * 1024 * 1024) { // 10MB
            recommendations.add("缓存内存占用过高(${lightweightCacheStats.memoryEstimate/1024/1024}MB)，建议清理缓存")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("性能良好，无需特别优化")
        }
        
        return recommendations
    }
    
    /**
     * 重置统计信息
     */
    fun reset() {
        totalParseTime = 0
        totalRenderTime = 0
        parseOperations = 0
        renderOperations = 0
        memorySnapshots.clear()
    }
    
    /**
     * 输出详细的性能日志
     */
    fun logPerformanceDetails() {
        val report = generateReport()
        
        AppLog.d("""
            📊 缓存性能分析报告:
            ============================
            平均解析时间: ${report.avgParseTime}ms
            平均渲染时间: ${report.avgRenderTime}ms
            总操作次数: ${report.totalOperations}
            内存效率: ${String.format("%.1f", report.memoryEfficiency)}%
            缓存效果: ${String.format("%.1f", report.cacheEffectiveness)}%
            
            📝 优化建议:
            ${report.recommendations.joinToString("\n") { "• $it" }}
            
            📈 缓存统计:
            轻量级缓存: ${LightweightMarkdownCache.getCacheStats()}
            
            💾 内存快照:
            ${memorySnapshots.takeLast(3).joinToString("\n") { 
                "时间: ${it.timestamp}, 已用: ${it.usedMemory/1024/1024}MB, 缓存: ${it.cacheSize}项"
            }}
        """.trimIndent())
    }
    
    /**
     * 检查是否需要清理缓存
     */
    fun checkCacheCleanupNeeded(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / runtime.maxMemory().toFloat()) * 100f
        
        return memoryUsagePercent > 80f // 内存使用超过80%时建议清理
    }
    
    /**
     * 执行智能缓存清理
     */
    fun performSmartCacheCleanup() {
        if (checkCacheCleanupNeeded()) {
            // 获取缓存统计信息
            val lightweightStats = LightweightMarkdownCache.getCacheStats()
            
            // 清理命中率低的缓存
            if (lightweightStats.hitRate < 30f) {
                LightweightMarkdownCache.clearAll()
                AppLog.d("已清理轻量级缓存（命中率低: ${lightweightStats.hitRate}%）")
            }
            // 清理过大的缓存
            else if (lightweightStats.cacheSize > 30) {
                // 执行部分清理
                LightweightMarkdownCache.clearAll()
                AppLog.d("已清理轻量级缓存（大小过大: ${lightweightStats.cacheSize}项）")
            }
        }
    }
} 