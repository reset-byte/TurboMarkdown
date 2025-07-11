package com.github.turbomarkwon

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.turbomarkwon.adapter.MarkdownAdapter
import com.github.turbomarkwon.config.MarkwonConfig
import com.github.turbomarkwon.data.MarkdownRenderState
import com.github.turbomarkwon.data.SampleMarkdown
import com.github.turbomarkwon.databinding.ActivityMainBinding
import com.github.turbomarkwon.databinding.DialogPerformanceStatsBinding
import com.github.turbomarkwon.viewmodel.MarkdownViewModel
import com.github.turbomarkwon.util.AppLog
import com.github.turbomarkwon.util.RecyclerViewPerformanceMonitor
import com.google.android.material.snackbar.Snackbar
import io.noties.markwon.Markwon

/**
 * 主Activity - 展示高性能Markdown渲染
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var markwon: Markwon
    private lateinit var adapter: MarkdownAdapter
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
        setupSwipeRefresh()
        setupFab()
        setupRecyclerViewPerformanceMonitor()
        loadSampleMarkdown()
    }
    
    /**
     * 设置Markwon实例
     */
    private fun setupMarkwon() {
        markwon = MarkwonConfig.createOptimizedMarkwon(this)
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = MarkdownAdapter(markwon)
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
            
            // 性能优化配置
            setItemViewCacheSize(20)
            recycledViewPool.setMaxRecycledViews(0, 10)  // 段落类型
            recycledViewPool.setMaxRecycledViews(1, 5)   // 标题类型
            recycledViewPool.setMaxRecycledViews(2, 5)   // 代码块类型
            recycledViewPool.setMaxRecycledViews(3, 8)   // 列表项类型
        }
    }
    
    /**
     * 设置ViewModel观察者
     */
    private fun setupViewModel() {
        // 观察Markdown项目列表
        viewModel.markdownItems.observe(this) { items ->
            adapter.submitList(items)
            AppLog.d("Loaded ${items.size} markdown items")
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            binding.swipeRefreshLayout.isRefreshing = isLoading
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
                is MarkdownRenderState.Loading -> {
                    AppLog.d("Loading markdown...")
                }
                is MarkdownRenderState.Success -> {
                    val result = state.result
                    AppLog.d("Parse completed in ${result.parseTimeMs}ms")
                    AppLog.d("Total items: ${result.itemCount}")
                    
                    // 记录启动时间
                    val currentTime = System.currentTimeMillis()
                    val totalStartupTime = currentTime - startupTime
                    viewModel.setStartupTime(totalStartupTime)
                    
                    // 3秒后自动显示性能统计
                    binding.root.postDelayed({
                        showPerformanceDialog()
                    }, 3000)
                }
                is MarkdownRenderState.Error -> {
                    AppLog.e("Parse error", state.exception)
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
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }
    
    /**
     * 设置悬浮按钮
     */
    private fun setupFab() {
        binding.fabStats.setOnClickListener {
            showPerformanceDialog()
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
        // 这里可以选择加载不同的示例
        val useSimple = intent.getBooleanExtra("simple", false)
        val testCache = intent.getBooleanExtra("cache_test", false)
        
        val markdown = when {
            testCache -> SampleMarkdown.CODE_CACHE_TEST_MARKDOWN
            useSimple -> SampleMarkdown.SIMPLE_MARKDOWN
            else -> SampleMarkdown.SAMPLE_LONG_MARKDOWN
        }
        
        viewModel.loadMarkdown(markdown)
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
            tvParseTime.text = "${stats["parse_time"]}ms"
            tvTotalItems.text = "${stats["total_items"]}"
            tvStartupTime.text = "${stats["startup_time"]}ms"
            tvMemoryUsage.text = "${stats["memory_usage"]}MB"
            tvCacheSize.text = "${stats["cache_size"]}"
            
            tvParagraphs.text = "${stats["paragraphs"]}"
            tvHeadings.text = "${stats["headings"]}"
            tvCodeBlocks.text = "${stats["code_blocks"]}"
            tvListItems.text = "${stats["lists"]}"
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        com.github.turbomarkwon.renderer.MarkdownRenderer.clearCache()
        
        // 停止RecyclerView性能监控
        recyclerViewPerformanceMonitor?.stopMonitoring(binding.recyclerView)
    }
}