package com.github.turbomarkwon.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.turbomarkwon.cache.CachePerformanceAnalyzer
import com.github.turbomarkwon.util.RecyclerViewPerformanceMonitor
import kotlinx.coroutines.launch
import com.github.turbomarkwon.util.AppLog

/**
 * ViewModel - 管理Markdown数据和状态
 */
class MarkdownViewModel : ViewModel() {
    
    // 原始 Markdown 文本（用于官方适配器）
    private val _markdownText = MutableLiveData<String>()
    val markdownText: LiveData<String> = _markdownText
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // 渲染状态（简化版）
    private val _renderState = MutableLiveData<MarkdownRenderState>()
    val renderState: LiveData<MarkdownRenderState> = _renderState
    
    // 性能数据
    private var startupTime: Long = 0
    private var loadStartTime: Long = 0
    
    // 帧率监控数据
    private val _frameMetrics = MutableLiveData<FrameMetrics>()
    val frameMetrics: LiveData<FrameMetrics> = _frameMetrics
    
    // 滚动状态
    private val _isScrolling = MutableLiveData(false)
    val isScrolling: LiveData<Boolean> = _isScrolling
    
    // 帧率监控数据类
    data class FrameMetrics(
        val currentFps: Float,
        val averageFrameTime: Float,
        val droppedFrames: Int,
        val rating: RecyclerViewPerformanceMonitor.PerformanceRating,
        val scrollVelocity: Float = 0f
    )
    
    // 简化的渲染状态
    sealed class MarkdownRenderState {
        object Loading : MarkdownRenderState()
        data class Success(val loadTimeMs: Long, val contentLength: Int) : MarkdownRenderState()
        data class Error(val exception: Exception) : MarkdownRenderState()
    }
    
    /**
     * 加载Markdown内容
     */
    fun loadMarkdown(markdownText: String) {
        AppLog.d("MarkdownViewModel: 开始加载Markdown，长度: ${markdownText.length}")
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _renderState.value = MarkdownRenderState.Loading
                loadStartTime = System.currentTimeMillis()
                
                AppLog.d("MarkdownViewModel: 设置_markdownText.value")
                // 保存原始文本，用于官方适配器
                _markdownText.value = markdownText
                AppLog.d("MarkdownViewModel: _markdownText.value已设置，当前值长度: ${_markdownText.value?.length}")
                
                // 拍摄性能快照
                CachePerformanceAnalyzer.takeMemorySnapshot()
                
                // 计算加载时间
                val loadTime = System.currentTimeMillis() - loadStartTime
                
                // 手动记录加载时间到性能分析器
                CachePerformanceAnalyzer.recordParseTime(loadTime)
                
                // 更新状态
                _renderState.value = MarkdownRenderState.Success(loadTime, markdownText.length)
                _errorMessage.value = null
                
                AppLog.d("MarkdownViewModel: Markdown加载完成，耗时: ${loadTime}ms")
                
            } catch (e: Exception) {
                _renderState.value = MarkdownRenderState.Error(e)
                _errorMessage.value = e.message
                AppLog.e("MarkdownViewModel: Markdown加载失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 设置启动时间
     */
    fun setStartupTime(startupTime: Long) {
        this.startupTime = startupTime
    }
    
    /**
     * 更新帧率数据
     */
    fun updateFrameMetrics(
        fps: Float,
        averageFrameTime: Float,
        droppedFrames: Int,
        rating: RecyclerViewPerformanceMonitor.PerformanceRating,
        scrollVelocity: Float = 0f
    ) {
        _frameMetrics.value = FrameMetrics(
            fps, averageFrameTime, droppedFrames, rating, scrollVelocity
        )
    }
    
    /**
     * 更新滚动状态
     */
    fun updateScrollingState(isScrolling: Boolean) {
        _isScrolling.value = isScrolling
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        CachePerformanceAnalyzer.logPerformanceDetails()
    }
    
    /**
     * 获取简化的统计信息
     */
    @SuppressLint("DefaultLocale")
    fun getStatistics(): Map<String, Any?> {
        val currentText = _markdownText.value ?: ""
        val loadTime = when (val state = _renderState.value) {
            is MarkdownRenderState.Success -> state.loadTimeMs
            else -> 0L
        }
        val memoryUsage = getMemoryUsage()
        val cacheStats = CachePerformanceAnalyzer.getCacheStats()
        
        return mapOf(
            // 基础性能指标
            "parse_time" to loadTime,
            "startup_time" to startupTime,
            "memory_usage" to memoryUsage,
            
            // 内容基本信息
            "content_length" to currentText.length,
            "content_lines" to currentText.lines().size,
            
            // Entry缓存性能（基于SimpleEntry模式）
            "total_render_count" to cacheStats["totalRenderCount"],
            "total_cache_hit_count" to cacheStats["totalCacheHitCount"],
            "global_hit_rate" to "${String.format("%.1f", cacheStats["globalHitRate"])}%",
            "entry_cache_types" to cacheStats["entryCacheCount"],
            "average_parse_time" to "${cacheStats["averageParseTime"]}ms",
            "last_memory_usage" to "${cacheStats["lastMemoryUsage"]}MB",
            
            // 内容统计
            "adapter_type" to "MarkwonMultiTypeAdapter (官方适配器)",
            "cache_strategy" to "Entry级别缓存 (SimpleEntry模式)",
            "paragraphs" to "SmartParagraphEntry + 数学公式检测",
            "headings" to "HeadingEntry + 字体大小适配", 
            "code_blocks" to "CodeBlockEntry + Mermaid图表",
            "tables" to "TableEntry + 横向滚动",
            "blockquotes" to "BlockQuoteEntry + 样式",
            "containers" to "ContainerEntry + 自定义容器"
        )
    }
    
    /**
     * 获取内存使用情况
     */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }
} 