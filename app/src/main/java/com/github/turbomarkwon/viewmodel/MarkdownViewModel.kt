package com.github.turbomarkwon.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.turbomarkwon.data.MarkdownItem
import com.github.turbomarkwon.data.MarkdownParseResult
import com.github.turbomarkwon.data.MarkdownRenderState
import com.github.turbomarkwon.parser.MarkdownParser
import com.github.turbomarkwon.renderer.MarkdownRenderer
import kotlinx.coroutines.launch

/**
 * ViewModel - 管理Markdown数据和状态
 */
class MarkdownViewModel : ViewModel() {
    
    private val markdownParser = MarkdownParser()
    
    // 渲染状态
    private val _renderState = MutableLiveData<MarkdownRenderState>()
    val renderState: LiveData<MarkdownRenderState> = _renderState
    
    // Markdown项目列表
    private val _markdownItems = MutableLiveData<List<MarkdownItem>>()
    val markdownItems: LiveData<List<MarkdownItem>> = _markdownItems
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // 性能数据
    private var parseResult: MarkdownParseResult? = null
    private var startupTime: Long = 0
    
    /**
     * 加载并解析Markdown内容
     */
    fun loadMarkdown(markdownText: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _renderState.value = MarkdownRenderState.Loading
                
                // 在后台线程解析Markdown
                val result = markdownParser.parseMarkdownAsync(markdownText)
                
                // 保存解析结果用于统计
                parseResult = result
                
                // 更新状态
                _renderState.value = MarkdownRenderState.Success(result)
                _markdownItems.value = result.items
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _renderState.value = MarkdownRenderState.Error(e)
                _errorMessage.value = e.message
                _markdownItems.value = emptyList()
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
     * 刷新内容
     */
    fun refresh() {
        val currentItems = _markdownItems.value
        if (currentItems != null) {
            // 清理缓存并重新加载
            MarkdownRenderer.clearCache()
            // 这里可以重新加载原始Markdown文本
            // 为了简化，我们只是重新设置当前项目
            _markdownItems.value = currentItems
        }
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        MarkdownRenderer.clearCache()
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): Map<String, Any> {
        val items = _markdownItems.value ?: emptyList()
        val parseTime = parseResult?.parseTimeMs ?: 0L
        val memoryUsage = getMemoryUsage()
        
        return mapOf(
            "total_items" to items.size,
            "paragraphs" to items.count { it is MarkdownItem.Paragraph },
            "headings" to items.count { it is MarkdownItem.Heading },
            "code_blocks" to items.count { it is MarkdownItem.CodeBlock },
            "lists" to items.count { it is MarkdownItem.ListItem },
            "tables" to items.count { it is MarkdownItem.Table },
            "cache_size" to MarkdownRenderer.getCacheSize(),
            "parse_time" to parseTime,
            "startup_time" to startupTime,
            "memory_usage" to memoryUsage
        )
    }
    
    /**
     * 获取内存使用情况
     */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        return usedMemory / (1024 * 1024) // 转换为MB
    }
} 