package com.github.turbomarkwon

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.turbomarkwon.util.MarkdownUtils
import com.github.turbomarkwon.viewmodel.MarkdownViewModel
import com.github.turbomarkwon.data.SampleMarkdown
import com.github.turbomarkwon.databinding.ActivityMainBinding
import com.github.turbomarkwon.databinding.DialogPerformanceStatsBinding
import com.github.turbomarkwon.util.AppLog
import com.github.turbomarkwon.util.RecyclerViewPerformanceMonitor
import com.google.android.material.snackbar.Snackbar
import io.noties.markwon.Markwon
import androidx.recyclerview.widget.RecyclerView
import com.github.turbomarkwon.cache.MermaidRenderCache
import io.noties.markwon.recycler.MarkwonAdapter
import com.github.turbomarkwon.adapter.MarkwonMultiTypeAdapter
import com.github.turbomarkwon.cache.CachePerformanceAnalyzer
import android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.widget.Toast

/**
 * 主Activity - 展示高性能Markdown渲染
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var markwon: Markwon
    private lateinit var adapter: MarkwonAdapter
    private val viewModel: MarkdownViewModel by viewModels()
    private var startupTime: Long = 0
    
    // 帧率监控相关
    private var recyclerViewPerformanceMonitor: RecyclerViewPerformanceMonitor? = null
    private var isScrolling = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        startupTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupMarkwon()
        setupRecyclerView()
        setupViewModel()
        setupFab()
        setupRecyclerViewPerformanceMonitor()
        loadSampleMarkdown()
    }
    
    /**
     * 设置Markwon实例
     */
    private fun setupMarkwon() {
        markwon = MarkdownUtils.getOptimizedMarkwon(this)
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        // 使用多类型的 Markwon 官方适配器
        adapter = MarkwonMultiTypeAdapter.create()
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
            
            // 针对多类型适配器的性能优化配置
            setItemViewCacheSize(30)  // 增加缓存大小
            
            // 优化 ViewHolder 回收池 - 为不同类型配置合适的缓存数量
            recycledViewPool.setMaxRecycledViews(0, 15)  // 智能段落类型（包括数学公式）
            // 多类型适配器会自动管理不同的 ViewType ID
            recycledViewPool.setMaxRecycledViews(1, 8)   // 标题类型
            recycledViewPool.setMaxRecycledViews(2, 10)  // 代码块类型  
            recycledViewPool.setMaxRecycledViews(3, 5)   // 表格类型
            recycledViewPool.setMaxRecycledViews(4, 5)   // 引用块类型
            recycledViewPool.setMaxRecycledViews(5, 6)   // 自定义容器类型
            
            // 设置滚动优化
            isNestedScrollingEnabled = true
            
            // 添加滚动监听器，在滚动时暂停图片加载
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            // 滚动停止时恢复图片加载
                            resumeImageLoading()
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING,
                        RecyclerView.SCROLL_STATE_SETTLING -> {
                            // 滚动中暂停图片加载以提高性能
                            pauseImageLoading()
                        }
                    }
                }
                
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    // 智能预加载：预测用户滚动方向并预加载即将显示的内容
                    if (dy > 0) { // 向下滚动
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                        val totalItemCount = layoutManager.itemCount
                        
                        // 当接近底部时，预加载下面的内容
                        if (lastVisiblePosition >= totalItemCount - 3 && totalItemCount > 0) {
                            MarkwonMultiTypeAdapter.preloadUpcomingContent(lastVisiblePosition + 1, 3)
                            AppLog.d("MainActivity: 触发向下滚动预加载，位置: ${lastVisiblePosition + 1}")
                        }
                    }
                }
            })
        }
    }
    
    /**
     * 设置ViewModel观察者
     */
    private fun setupViewModel() {
        // 观察Markdown渲染状态，使用原始文本进行渲染
        viewModel.markdownText.observe(this) { markdownText ->
            if (markdownText.isNotEmpty()) {
                AppLog.d("MainActivity: 收到新的Markdown内容，长度: ${markdownText.length}")
                AppLog.d("MainActivity: 内容预览: ${markdownText.take(200)}")
                
                adapter.setMarkdown(markwon, markdownText)
                AppLog.d("MainActivity: 已调用adapter.setMarkdown()更新适配器")
                
                // 启用智能预加载
                MarkwonMultiTypeAdapter.enableIntelligentPreloading(adapter, markwon, markdownText)
                AppLog.d("MainActivity: 智能预加载已启用")
                
                // 强制刷新RecyclerView
                binding.recyclerView.post {
                    adapter.notifyDataSetChanged()
                    AppLog.d("MainActivity: 适配器项目数量: ${adapter.itemCount}")
                    AppLog.d("MainActivity: 强制刷新RecyclerView完成")
                }
            } else {
                AppLog.d("MainActivity: 收到空的Markdown内容，跳过更新")
            }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        // 观察错误信息
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
        
        // 观察渲染状态
        viewModel.renderState.observe(this) { state ->
            when (state) {
                is MarkdownViewModel.MarkdownRenderState.Loading -> {
                    AppLog.d("Loading markdown...")
                }
                is MarkdownViewModel.MarkdownRenderState.Success -> {
                    AppLog.d("Markdown rendered successfully in ${state.loadTimeMs}ms")
                    AppLog.d("Content processed with multi-type Markwon adapter (${state.contentLength} chars)")
                    
                    // 记录启动时间
                    val currentTime = System.currentTimeMillis()
                    val totalStartupTime = currentTime - startupTime
                    viewModel.setStartupTime(totalStartupTime)
                }
                is MarkdownViewModel.MarkdownRenderState.Error -> {
                    AppLog.e("Markdown render error", state.exception)
                }
            }
        }
        
        // 观察帧率数据
        viewModel.frameMetrics.observe(this) { metrics ->
            updateFrameMetricsUI(metrics)
        }
        
        // 观察滚动状态
        viewModel.isScrolling.observe(this) { scrolling ->
            updateScrollStatusUI(scrolling)
        }
    }
    
    /**
     * 设置悬浮按钮
     */
    private fun setupFab() {
        binding.fabStats.setOnClickListener {
            showPerformanceDialog()
        }
        
        // 长按显示缓存性能报告
        binding.fabStats.setOnLongClickListener {
            showCachePerformanceInfo()
            true
        }
        
        // 长按显示测试选项
        binding.fabStats.setOnLongClickListener {
            showTestOptionsDialog()
            true
        }
    }
    
    /**
     * 设置RecyclerView专用性能监控器
     */
    private fun setupRecyclerViewPerformanceMonitor() {
        recyclerViewPerformanceMonitor = RecyclerViewPerformanceMonitor().apply {
            setOnPerformanceUpdateListener(object : RecyclerViewPerformanceMonitor.OnPerformanceUpdateListener {
                override fun onScrollPerformanceUpdate(
                    fps: Float,
                    averageFrameTime: Float,
                    droppedFrames: Int,
                    scrollVelocity: Float,
                    rating: RecyclerViewPerformanceMonitor.PerformanceRating
                ) {
                    // 更新ViewModel中的帧率数据
                    viewModel.updateFrameMetrics(
                        fps,
                        averageFrameTime,
                        droppedFrames,
                        rating,
                        scrollVelocity
                    )
                }
                
                override fun onScrollStateChanged(isScrolling: Boolean) {
                    // 更新滚动状态
                    this@MainActivity.isScrolling = isScrolling
                    viewModel.updateScrollingState(isScrolling)
                }
            })
            startMonitoring(binding.recyclerView)
        }
        AppLog.d("RecyclerView performance monitor initialized")
    }
    
    /**
     * 更新帧率UI显示
     */
    @SuppressLint("SetTextI18n")
    private fun updateFrameMetricsUI(metrics: MarkdownViewModel.FrameMetrics) {
        binding.apply {
            tvCurrentFps.text = "FPS: ${metrics.currentFps.toInt()}"
            tvFrameTime.text = "Frame: ${String.format("%.1f", metrics.averageFrameTime)}ms"
            
            // 更新滚动速度显示
            if (metrics.scrollVelocity > 0) {
                tvScrollVelocity.text = "速度: ${String.format("%.0f", metrics.scrollVelocity)}px/s"
            }
            
            // 根据帧率和滚动状态设置颜色
            val color = if (isScrolling) {
                // 滚动时使用严格的评级标准
                when (metrics.rating) {
                    RecyclerViewPerformanceMonitor.PerformanceRating.EXCELLENT -> 
                        android.graphics.Color.GREEN
                    RecyclerViewPerformanceMonitor.PerformanceRating.GOOD -> 
                        android.graphics.Color.BLUE
                    RecyclerViewPerformanceMonitor.PerformanceRating.FAIR -> 
                        android.graphics.Color.YELLOW
                    RecyclerViewPerformanceMonitor.PerformanceRating.POOR -> 
                        android.graphics.Color.RED
                }
            } else {
                // 静止状态使用宽松的评级标准
                when {
                    metrics.currentFps >= 30f -> android.graphics.Color.GREEN
                    metrics.currentFps >= 20f -> android.graphics.Color.BLUE
                    else -> android.graphics.Color.YELLOW
                }
            }
            tvCurrentFps.setTextColor(color)
        }
    }
    
    /**
     * 更新滚动状态UI
     */
    private fun updateScrollStatusUI(scrolling: Boolean) {
        binding.apply {
            tvScrollStatus.text = if (scrolling) {
                "滚动中"
            } else {
                "静止 (节能)"
            }
            
            // 根据滚动状态显示/隐藏速度信息
            tvScrollVelocity.visibility = if (scrolling) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
    
    /**
     * 加载示例Markdown内容
     */
    private fun loadSampleMarkdown() {
        // 加载默认的完整技术文档
        viewModel.loadMarkdown(SampleMarkdown.SAMPLE_LONG_MARKDOWN)
    }
    
    /**
     * 显示测试选项Dialog
     */
    private fun showTestOptionsDialog() {
        val options = arrayOf(
            "1、完整技术文档",
            "2、综合表格测试",
            "3、数学公式测试",
            "4、图片渲染测试",
            "5、Mermaid图表测试",
            "6、自定义标签测试",
            "7、自定义容器测试",
            "8、性能统计"
        )
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("选择测试用例")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> loadTestCase(SampleMarkdown.SAMPLE_LONG_MARKDOWN, "完整技术文档")
                    1 -> loadTestCase(SampleMarkdown.COMPREHENSIVE_TABLE_TEST_MARKDOWN, "综合表格测试")
                    2 -> loadTestCase(SampleMarkdown.MATHEMATICAL_FORMULA_TEST_MARKDOWN, "数学公式测试")
                    3 -> loadTestCase(SampleMarkdown.IMAGE_RENDERING_TEST_MARKDOWN, "图片渲染测试")
                    4 -> loadTestCase(SampleMarkdown.MERMAID_TEST_MARKDOWN, "Mermaid图表测试")
                    5 -> loadTestCase(SampleMarkdown.CUSTOM_TAGS_TEST_MARKDOWN, "自定义标签测试")
                    6 -> loadTestCase(SampleMarkdown.CONTAINER_TEST_MARKDOWN, "自定义容器测试")
                    7 -> showPerformanceDialog()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        
        // 设置左右margin为16dp
        dialog.window?.let { window ->
            val params = window.attributes
            val margin = (16 * resources.displayMetrics.density).toInt()
            params.width = resources.displayMetrics.widthPixels - 2 * margin
            window.attributes = params
        }
        
        dialog.show()
    }
    
    /**
     * 加载指定的测试用例
     */
    private fun loadTestCase(markdown: String, caseName: String) {
        AppLog.d("MainActivity: 开始加载测试用例: $caseName")
        AppLog.d("MainActivity: 测试用例内容长度: ${markdown.length}")
        AppLog.d("MainActivity: 内容预览: ${markdown.take(200)}")
        
        Snackbar.make(binding.root, "使用多类型官方适配器: $caseName", Snackbar.LENGTH_SHORT).show()
        
        // 重置启动时间以测量新的加载时间
        startupTime = System.currentTimeMillis()
        
        AppLog.d("MainActivity: 调用viewModel.loadMarkdown()")
        viewModel.loadMarkdown(markdown)
        AppLog.d("MainActivity: viewModel.loadMarkdown()调用完成")
    }
    
    /**
     * 显示性能统计Dialog
     */
    @SuppressLint("SetTextI18n")
    private fun showPerformanceDialog() {
        val dialogBinding = DialogPerformanceStatsBinding.inflate(layoutInflater)
        val stats = viewModel.getStatistics()
        
        // 填充数据
        dialogBinding.apply {
            // 基础性能指标
            tvParseTime.text = "${stats["parse_time"]}ms"
            tvStartupTime.text = "${stats["startup_time"]}ms"
            tvMemoryUsage.text = "${stats["memory_usage"]}MB"
            
            // 轻量级缓存性能
            tvCacheHitRate.text = "${stats["cache_hit_rate"]}%"
            tvCacheHits.text = "${stats["cache_hits"]}"
            tvCacheMisses.text = "${stats["cache_misses"]}"
            tvLightweightCacheSize.text = "${stats["lightweight_cache_size"]} 项"
            tvCacheMemoryEstimate.text = "${stats["cache_memory_estimate"]}"
            
            // 高级性能指标
            tvAvgParseTime.text = "${stats["avg_parse_time"]}ms"
            tvAvgRenderTime.text = "${stats["avg_render_time"]}ms"
            tvMemoryEfficiency.text = "${stats["memory_efficiency"]}"
            tvCacheEffectiveness.text = "${stats["cache_effectiveness"]}"
            
            // 内容统计
            tvTotalItems.text = "${stats["total_items"]}"
            tvParagraphs.text = "${stats["paragraphs"]}"
            tvHeadings.text = "${stats["headings"]}"
            tvCodeBlocks.text = "${stats["code_blocks"]}"
            tvListItems.text = "${stats["lists"]}"
            tvTables.text = "${stats["tables"]}"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        
        // 设置左右margin为16dp
        dialog.window?.let { window ->
            val params = window.attributes
            val margin = (16 * resources.displayMetrics.density).toInt()
            params.width = resources.displayMetrics.widthPixels - 2 * margin
            window.attributes = params
        }
        
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 显示缓存性能信息
     */
    private fun showCachePerformanceInfo() {
        val report = CachePerformanceAnalyzer.generatePerformanceReport()
        val cacheStats = CachePerformanceAnalyzer.getCacheStats()
        
        val fullReport = """
            ${report}
            
            === 全局统计 ===
            总渲染次数: ${cacheStats["totalRenderCount"]}
            总缓存命中次数: ${cacheStats["totalCacheHitCount"]}
            全局命中率: ${String.format("%.1f", cacheStats["globalHitRate"])}%
            平均解析时间: ${cacheStats["averageParseTime"]}ms
            当前内存使用: ${cacheStats["lastMemoryUsage"]}MB
            Entry缓存类型数: ${cacheStats["entryCacheCount"]}
        """.trimIndent()
        
        // 显示在对话框中
        android.app.AlertDialog.Builder(this)
            .setTitle("缓存性能报告")
            .setMessage(fullReport)
            .setPositiveButton("确定", null)
            .setNeutralButton("清除缓存") { _, _ ->
                MarkwonMultiTypeAdapter.clearAllCaches()
                CachePerformanceAnalyzer.clearAll()
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
            .show()
        
        // 同时输出到日志
        AppLog.d("MainActivity: 缓存性能报告\n$fullReport")
    }
    
    /**
     * 暂停图片加载以提高滚动性能
     */
    private fun pauseImageLoading() {
        try {
            // 暂停 Glide 图片加载
            com.bumptech.glide.Glide.with(this).pauseRequests()
        } catch (e: Exception) {
            AppLog.d("Failed to pause image loading: ${e.message}")
        }
    }
    
    /**
     * 恢复图片加载
     */
    private fun resumeImageLoading() {
        try {
            // 恢复 Glide 图片加载
            com.bumptech.glide.Glide.with(this).resumeRequests()
        } catch (e: Exception) {
            AppLog.d("Failed to resume image loading: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 清除适配器缓存以避免内存泄漏
        MarkwonMultiTypeAdapter.clearAllCaches()
        AppLog.d("MainActivity: 已清除适配器缓存")
        
        // 清除其他缓存
        CachePerformanceAnalyzer.clearAll()
        
        recyclerViewPerformanceMonitor?.stopMonitoring(binding.recyclerView)
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        
        // 低内存时清除缓存
        AppLog.d("MainActivity: 检测到低内存，清除缓存")
        MarkwonMultiTypeAdapter.clearAllCaches()
        CachePerformanceAnalyzer.handleLowMemory()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // 内存紧张时清除缓存
                AppLog.d("MainActivity: 内存紧张(level=$level)，清除适配器缓存")
                MarkwonMultiTypeAdapter.clearAllCaches()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_UI_HIDDEN -> {
                // 应用进入后台时部分清理
                AppLog.d("MainActivity: 应用后台(level=$level)，执行部分缓存清理")
                CachePerformanceAnalyzer.trimCaches()
            }
        }
    }
}