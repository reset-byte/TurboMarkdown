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
    }
}