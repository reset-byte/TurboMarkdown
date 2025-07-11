package com.github.turbomarkwon.util

import android.view.Choreographer
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * RecyclerView专用性能监控器
 * 使用Choreographer精确监控滚动时的帧率性能
 */
class RecyclerViewPerformanceMonitor {
    
    private var choreographer: Choreographer? = null
    private var frameCallback: Choreographer.FrameCallback? = null
    private var scrollListener: RecyclerView.OnScrollListener? = null
    
    // 滚动状态
    private var isScrolling: Boolean = false
    private var scrollStartTime: Long = 0
    private var scrollEndTime: Long = 0
    
    // 帧率统计
    private var frameCount: Int = 0
    private var frameTimesNs: MutableList<Long> = mutableListOf()
    private var lastFrameTimeNs: Long = 0
    private var currentFps: Float = 0f
    private var averageFrameTime: Float = 0f
    private var maxFrameTime: Long = 0
    private var droppedFrames: Int = 0
    
    // 滚动距离统计
    private var totalScrollDistance: Int = 0
    private var scrollVelocity: Float = 0f
    
    // 性能监控配置
    private val targetFps: Float = 60f
    private val smoothFrameTimeNs: Long = 16_666_666L // 16.67ms in nanoseconds
    private val frameWindowSize: Int = 60 // 1秒的帧窗口
    
    // 回调接口
    interface OnPerformanceUpdateListener {
        fun onScrollPerformanceUpdate(
            fps: Float,
            averageFrameTime: Float,
            droppedFrames: Int,
            scrollVelocity: Float,
            rating: PerformanceRating
        )
        
        fun onScrollStateChanged(isScrolling: Boolean)
    }
    
    enum class PerformanceRating {
        EXCELLENT, GOOD, FAIR, POOR
    }
    
    private var performanceUpdateListener: OnPerformanceUpdateListener? = null
    
    /**
     * 开始监控RecyclerView性能
     */
    fun startMonitoring(recyclerView: RecyclerView) {
        choreographer = Choreographer.getInstance()
        setupScrollListener(recyclerView)
        AppLog.d("RecyclerView performance monitoring started")
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring(recyclerView: RecyclerView) {
        stopFrameMonitoring()
        scrollListener?.let { recyclerView.removeOnScrollListener(it) }
        choreographer = null
        AppLog.d("RecyclerView performance monitoring stopped")
    }
    
    /**
     * 设置性能更新监听器
     */
    fun setOnPerformanceUpdateListener(listener: OnPerformanceUpdateListener) {
        performanceUpdateListener = listener
    }
    
    /**
     * 设置滚动监听器
     */
    private fun setupScrollListener(recyclerView: RecyclerView) {
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                handleScrollStateChange(newState)
            }
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isScrolling) {
                    // 累计滚动距离
                    totalScrollDistance += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                    
                    // 计算滚动速度
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = currentTime - scrollStartTime
                    if (deltaTime > 0) {
                        scrollVelocity = totalScrollDistance.toFloat() / deltaTime * 1000f // pixels/second
                    }
                }
            }
        }
        
        recyclerView.addOnScrollListener(scrollListener!!)
    }
    
    /**
     * 处理滚动状态变化
     */
    private fun handleScrollStateChange(newState: Int) {
        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                if (isScrolling) {
                    stopScrolling()
                }
            }
            RecyclerView.SCROLL_STATE_DRAGGING,
            RecyclerView.SCROLL_STATE_SETTLING -> {
                if (!isScrolling) {
                    startScrolling()
                }
            }
        }
    }
    
    /**
     * 开始滚动
     */
    private fun startScrolling() {
        isScrolling = true
        scrollStartTime = System.currentTimeMillis()
        totalScrollDistance = 0
        resetFrameStats()
        startFrameMonitoring()
        
        performanceUpdateListener?.onScrollStateChanged(true)
        AppLog.d("Started scrolling performance monitoring")
    }
    
    /**
     * 停止滚动
     */
    private fun stopScrolling() {
        isScrolling = false
        scrollEndTime = System.currentTimeMillis()
        stopFrameMonitoring()
        
        performanceUpdateListener?.onScrollStateChanged(false)
        logScrollingStats()
        AppLog.d("Stopped scrolling performance monitoring")
    }
    
    /**
     * 开始帧率监控
     */
    private fun startFrameMonitoring() {
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isScrolling) {
                    handleFrameUpdate(frameTimeNanos)
                    choreographer?.postFrameCallback(this)
                }
            }
        }
        
        choreographer?.postFrameCallback(frameCallback!!)
    }
    
    /**
     * 停止帧率监控
     */
    private fun stopFrameMonitoring() {
        frameCallback?.let { choreographer?.removeFrameCallback(it) }
        frameCallback = null
    }
    
    /**
     * 处理帧更新
     */
    private fun handleFrameUpdate(frameTimeNanos: Long) {
        if (lastFrameTimeNs == 0L) {
            lastFrameTimeNs = frameTimeNanos
            return
        }
        
        val frameInterval = frameTimeNanos - lastFrameTimeNs
        lastFrameTimeNs = frameTimeNanos
        
        // 更新帧时间统计
        frameTimesNs.add(frameInterval)
        frameCount++
        
        // 保持滑动窗口
        if (frameTimesNs.size > frameWindowSize) {
            frameTimesNs.removeAt(0)
        }
        
        // 检查是否丢帧
        if (frameInterval > smoothFrameTimeNs) {
            droppedFrames++
        }
        
        // 更新最大帧时间
        maxFrameTime = maxOf(maxFrameTime, frameInterval)
        
        // 计算当前FPS和平均帧时间
        updatePerformanceMetrics()
        
        // 通知性能更新
        val rating = calculatePerformanceRating()
        performanceUpdateListener?.onScrollPerformanceUpdate(
            currentFps,
            averageFrameTime,
            droppedFrames,
            scrollVelocity,
            rating
        )
    }
    
    /**
     * 更新性能指标
     */
    private fun updatePerformanceMetrics() {
        if (frameTimesNs.isEmpty()) return
        
        // 计算平均帧时间（毫秒）
        val averageFrameTimeNs = frameTimesNs.average()
        averageFrameTime = (averageFrameTimeNs / 1_000_000f).toFloat()
        
        // 计算当前FPS
        currentFps = if (averageFrameTimeNs > 0) {
            (1_000_000_000f / averageFrameTimeNs).roundToInt().toFloat()
        } else {
            0f
        }
        
        // 限制FPS上限
        currentFps = currentFps.coerceAtMost(targetFps)
    }
    
    /**
     * 计算性能评级
     */
    private fun calculatePerformanceRating(): PerformanceRating {
        val smoothPercentage = getSmoothPercentage()
        
        return when {
            currentFps >= 55f && smoothPercentage >= 95f -> PerformanceRating.EXCELLENT
            currentFps >= 45f && smoothPercentage >= 90f -> PerformanceRating.GOOD
            currentFps >= 30f && smoothPercentage >= 80f -> PerformanceRating.FAIR
            else -> PerformanceRating.POOR
        }
    }
    
    /**
     * 获取流畅度百分比
     */
    private fun getSmoothPercentage(): Float {
        if (frameCount == 0) return 100f
        
        val smoothFrames = frameCount - droppedFrames
        return (smoothFrames.toFloat() / frameCount.toFloat() * 100f)
    }
    
    /**
     * 重置帧统计
     */
    private fun resetFrameStats() {
        frameCount = 0
        frameTimesNs.clear()
        lastFrameTimeNs = 0
        currentFps = 0f
        averageFrameTime = 0f
        maxFrameTime = 0
        droppedFrames = 0
    }
    
    /**
     * 记录滚动统计
     */
    private fun logScrollingStats() {
        val scrollDuration = scrollEndTime - scrollStartTime
        val avgScrollSpeed = if (scrollDuration > 0) {
            totalScrollDistance.toFloat() / scrollDuration * 1000f
        } else 0f
        
        AppLog.d("Scrolling stats: " +
                "Duration=${scrollDuration}ms, " +
                "Distance=${totalScrollDistance}px, " +
                "AvgSpeed=${avgScrollSpeed}px/s, " +
                "FPS=${currentFps}, " +
                "DroppedFrames=${droppedFrames}/${frameCount}")
    }
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): Map<String, Any> {
        return mapOf(
            "is_scrolling" to isScrolling,
            "current_fps" to currentFps,
            "average_frame_time" to averageFrameTime,
            "max_frame_time" to (maxFrameTime / 1_000_000f), // 转换为毫秒
            "dropped_frames" to droppedFrames,
            "total_frames" to frameCount,
            "smooth_percentage" to getSmoothPercentage(),
            "scroll_velocity" to scrollVelocity,
            "performance_rating" to calculatePerformanceRating().name
        )
    }
} 