package com.github.turbomarkwon.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.HorizontalScrollView
import com.github.turbomarkwon.R
import com.github.turbomarkwon.util.AppLog
import com.github.turbomarkwon.views.CodeDisplayView
import com.github.turbomarkwon.views.MermaidDisplayView
import io.noties.markwon.Markwon
import io.noties.markwon.recycler.MarkwonAdapter
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.node.*
import com.github.turbomarkwon.customcontainer.ContainerNode
import com.github.turbomarkwon.util.MathUtils
import com.github.turbomarkwon.util.MarkdownUtils
import com.github.turbomarkwon.cache.MermaidRenderCache
import android.widget.LinearLayout
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import kotlin.math.abs
import android.text.Spanned
import java.util.HashMap
import com.github.turbomarkwon.cache.CachePerformanceAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.commonmark.node.Document

/**
 * 多类型 Markwon 适配器工厂
 */
object MarkwonMultiTypeAdapter {
    
    // 存储创建的 Entry 实例，用于缓存清理
    private var currentEntries: List<MarkwonAdapter.Entry<*, *>>? = null
    
    // 预加载相关
    private var currentDocument: Document? = null
    private var currentMarkwon: Markwon? = null
    
    // 专用的协程作用域，用于预加载任务
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 内容类型枚举 - 用于预加载优先级管理
     */
    enum class ContentType {
        MERMAID,        // 最高优先级
        MATH_FORMULAS,  // 高优先级  
        LARGE_TABLES,   // 中优先级
        CODE_BLOCKS,    // 中优先级
        PARAGRAPHS,     // 低优先级
        HEADINGS        // 低优先级
    }
    
    /**
     * 预加载优先级
     */
    enum class PreloadPriority {
        IMMEDIATE,  // 立即预加载（Mermaid、复杂数学公式）
        DELAYED,    // 延迟预加载（表格、代码块）
        IDLE        // 空闲时预加载（普通内容）
    }
    
    /**
     * 创建支持多类型的 Markwon 适配器
     */
    fun create(): MarkwonAdapter {
        // 创建 Entry 实例
        val smartParagraphEntry = SmartParagraphEntry()
        val headingEntry = HeadingEntry()
        val codeBlockEntry = CodeBlockEntry()
        val tableEntry = TableEntry()
        val blockQuoteEntry = BlockQuoteEntry()
        val containerEntry = ContainerEntry()
        
        // 保存引用以便后续清理
        currentEntries = listOf(
            smartParagraphEntry,
            headingEntry,
            codeBlockEntry,
            tableEntry,
            blockQuoteEntry,
            containerEntry
        )
        
        return MarkwonAdapter.builder(smartParagraphEntry)
            .include(Heading::class.java, headingEntry)
            .include(FencedCodeBlock::class.java, codeBlockEntry)
            .include(IndentedCodeBlock::class.java, codeBlockEntry)
            .include(TableBlock::class.java, tableEntry)
            .include(BlockQuote::class.java, blockQuoteEntry)
            .include(ContainerNode::class.java, containerEntry)
            .build()
    }
    
    /**
     * 标题 Entry - 支持缓存优化
     */
    class HeadingEntry : MarkwonAdapter.Entry<Heading, HeadingEntry.HeadingHolder>() {
        
        // 缓存已渲染的标题节点
        private val cache: MutableMap<Heading, Spanned> = HashMap()
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): HeadingHolder {
            return HeadingHolder(inflater.inflate(R.layout.item_markwon_heading, parent, false))
        }
        
        override fun bindHolder(markwon: Markwon, holder: HeadingHolder, node: Heading) {
            // 根据标题级别调整字体大小
            val textSize = when (node.level) {
                1 -> 24f
                2 -> 22f
                3 -> 20f
                4 -> 18f
                5 -> 16f
                6 -> 14f
                else -> 16f
            }
            holder.textView.textSize = textSize
            
            // 检查缓存，避免重复渲染
            var spanned = cache[node]
            if (spanned == null) {
                val renderStartTime = System.currentTimeMillis()
                spanned = markwon.render(node)
                val renderTime = System.currentTimeMillis() - renderStartTime
                
                cache[node] = spanned
                CachePerformanceAnalyzer.recordCacheMiss("HeadingEntry", renderTime)
                CachePerformanceAnalyzer.updateCacheSize("HeadingEntry", cache.size)
                AppLog.d("HeadingEntry: 新渲染标题 level=${node.level}, 缓存大小=${cache.size}, 耗时=${renderTime}ms")
            } else {
                CachePerformanceAnalyzer.recordCacheHit("HeadingEntry")
                AppLog.d("HeadingEntry: 使用缓存标题 level=${node.level}")
            }
            
            markwon.setParsedMarkdown(holder.textView, spanned)
        }
        
        override fun clear() {
            cache.clear()
            AppLog.d("HeadingEntry: 清除缓存")
        }
        
        /**
         * 预加载节点 - 提前渲染并缓存
         */
        fun preloadNode(markwon: Markwon, node: Node) {
            if (node is Heading && !cache.containsKey(node)) {
                try {
                    val renderStartTime = System.currentTimeMillis()
                    val spanned = markwon.render(node)
                    val renderTime = System.currentTimeMillis() - renderStartTime
                    
                    cache[node] = spanned
                    CachePerformanceAnalyzer.recordCacheMiss("HeadingEntry_Preload", renderTime)
                    CachePerformanceAnalyzer.updateCacheSize("HeadingEntry", cache.size)
                    AppLog.d("HeadingEntry: 预加载标题完成 level=${node.level}, 耗时=${renderTime}ms")
                } catch (e: Exception) {
                    AppLog.e("HeadingEntry: 预加载标题失败", e)
                }
            }
        }
        
        class HeadingHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val textView: TextView = requireView(R.id.text)
        }
    }
    
    /**
     * 代码块 Entry - 处理 FencedCodeBlock 和 IndentedCodeBlock，支持 Mermaid 缓存
     */
    class CodeBlockEntry : MarkwonAdapter.Entry<Node, CodeBlockEntry.CodeBlockHolder>() {
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): CodeBlockHolder {
            return CodeBlockHolder(inflater.inflate(R.layout.item_markwon_code_block, parent, false))
        }
        
        override fun bindHolder(markwon: Markwon, holder: CodeBlockHolder, node: Node) {
            val codeText = when (node) {
                is FencedCodeBlock -> node.literal ?: ""
                is IndentedCodeBlock -> node.literal ?: ""
                else -> "Unknown code block"
            }
            
            val language = when (node) {
                is FencedCodeBlock -> node.info
                else -> null
            }
            
            AppLog.d("CodeBlockEntry: 渲染代码块 language=$language")
            
            // 清除之前的内容
            holder.codeContainer.removeAllViews()
            
            // 检查是否为 Mermaid 图表
            if (language?.lowercase() == "mermaid") {
                showMermaidDiagram(holder, codeText)
            } else {
                showCodeBlock(holder, codeText, language)
            }
        }
        
        /**
         * 显示 Mermaid 图表，带缓存优化
         */
        private fun showMermaidDiagram(holder: CodeBlockHolder, mermaidContent: String) {
            // 生成全局缓存键
            val globalCacheKey = MermaidRenderCache.generateCacheKey(mermaidContent)
            
            // 检查全局缓存状态
            when (MermaidRenderCache.getRenderState(globalCacheKey)) {
                MermaidRenderCache.MermaidRenderState.SUCCESS -> {
                    // 已成功渲染，可以直接复用
                    AppLog.d("CodeBlockEntry: Mermaid图表已在全局缓存中，复用中...")
                    reuseExistingMermaidView(holder, mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.RENDERING -> {
                    // 正在渲染中，等待完成
                    AppLog.d("CodeBlockEntry: Mermaid图表正在渲染中，等待完成...")
                    createMermaidViewAndWait(holder, mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.ERROR -> {
                    // 之前渲染失败，重新尝试
                    AppLog.d("CodeBlockEntry: 之前渲染失败，重新尝试...")
                    createMermaidViewAndRender(holder, mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.NONE -> {
                    // 首次渲染
                    AppLog.d("CodeBlockEntry: 首次渲染此Mermaid图表")
                    createMermaidViewAndRender(holder, mermaidContent, globalCacheKey)
                }
            }
        }
        
        /**
         * 复用已存在的 Mermaid 视图
         */
        private fun reuseExistingMermaidView(holder: CodeBlockHolder, mermaidContent: String, cacheKey: String) {
            val mermaidView = MermaidDisplayView(holder.itemView.context)
            mermaidView.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    AppLog.d("CodeBlockEntry: 成功复用缓存的Mermaid图表")
                } else {
                    AppLog.e("CodeBlockEntry: 复用缓存的Mermaid图表失败: $error")
                    MermaidRenderCache.markRenderingError(cacheKey)
                }
            }
            holder.codeContainer.addView(mermaidView)
        }
        
        /**
         * 创建 Mermaid 视图并等待渲染完成
         */
        private fun createMermaidViewAndWait(holder: CodeBlockHolder, mermaidContent: String, cacheKey: String) {
            val mermaidView = MermaidDisplayView(holder.itemView.context)
            mermaidView.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    MermaidRenderCache.markRenderingSuccess(cacheKey)
                    AppLog.d("CodeBlockEntry: Mermaid图表等待后渲染成功")
                } else {
                    MermaidRenderCache.markRenderingError(cacheKey)
                    AppLog.e("CodeBlockEntry: Mermaid图表等待后渲染失败: $error")
                }
            }
            holder.codeContainer.addView(mermaidView)
        }
        
        /**
         * 创建 Mermaid 视图并开始渲染
         */
        private fun createMermaidViewAndRender(holder: CodeBlockHolder, mermaidContent: String, cacheKey: String) {
            // 标记开始渲染
            MermaidRenderCache.markRenderingStart(cacheKey)
            
            val mermaidView = MermaidDisplayView(holder.itemView.context)
            mermaidView.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    MermaidRenderCache.markRenderingSuccess(cacheKey)
                    AppLog.d("CodeBlockEntry: Mermaid图表渲染成功")
                } else {
                    MermaidRenderCache.markRenderingError(cacheKey)
                    AppLog.e("CodeBlockEntry: Mermaid图表渲染失败: $error")
                }
            }
            holder.codeContainer.addView(mermaidView)
        }
        
        /**
         * 显示代码块
         */
        private fun showCodeBlock(holder: CodeBlockHolder, codeText: String, language: String?) {
            val codeView = CodeDisplayView(holder.itemView.context)
            codeView.setCode(codeText, language ?: "code")
            holder.codeContainer.addView(codeView)
        }
        
        override fun onViewRecycled(holder: CodeBlockHolder) {
            super.onViewRecycled(holder)
            // 清理视图以避免内存泄漏
            holder.codeContainer.removeAllViews()
        }
        
        // CodeBlockEntry 不需要缓存，因为它使用自定义视图而不是 Markwon 渲染
        
        class CodeBlockHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val codeContainer: android.widget.FrameLayout = requireView(R.id.codeContainer)
        }
    }
    
    /**
     * 表格 Entry - 支持缓存优化
     */
    class TableEntry : MarkwonAdapter.Entry<TableBlock, TableEntry.TableHolder>() {
        
        // 缓存已渲染的表格节点
        private val cache: MutableMap<TableBlock, Spanned> = HashMap()
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): TableHolder {
            val holder = TableHolder(inflater.inflate(R.layout.item_markwon_table, parent, false))
            // 初始化表格滚动处理
            setupTableScrolling(holder)
            return holder
        }
        
        @SuppressLint("SetTextI18n")
        override fun bindHolder(markwon: Markwon, holder: TableHolder, node: TableBlock) {
            try {
                AppLog.d("TableEntry: 开始渲染表格，节点类型: ${node.javaClass.simpleName}")
                
                // 设置TableAwareMovementMethod
                holder.textView.movementMethod = io.noties.markwon.ext.tables.TableAwareMovementMethod.create()
                
                // 清除之前的内容
                holder.textView.text = ""
                
                // 检查缓存，避免重复渲染
                var spanned = cache[node]
                if (spanned == null) {
                    val renderStartTime = System.currentTimeMillis()
                    // 使用MarkwonAdapter的标准方式渲染
                    spanned = markwon.render(node)
                    val renderTime = System.currentTimeMillis() - renderStartTime
                    
                    cache[node] = spanned
                    CachePerformanceAnalyzer.recordCacheMiss("TableEntry", renderTime)
                    CachePerformanceAnalyzer.updateCacheSize("TableEntry", cache.size)
                    AppLog.d("TableEntry: 新渲染表格，缓存大小=${cache.size}, 耗时=${renderTime}ms")
                } else {
                    CachePerformanceAnalyzer.recordCacheHit("TableEntry")
                    AppLog.d("TableEntry: 使用缓存表格")
                }
                
                AppLog.d("TableEntry: 渲染结果Spanned长度: ${spanned.length}")
                
                if (spanned.isNotEmpty()) {
                    markwon.setParsedMarkdown(holder.textView, spanned)
                    AppLog.d("TableEntry: 成功设置渲染内容到TextView")
                } else {
                    AppLog.d("TableEntry: 渲染结果为空，尝试备用方法")
                    val tableMarkdown = convertTableToMarkdown(node)
                    AppLog.d("TableEntry: 生成备用Markdown: $tableMarkdown")
                    markwon.setMarkdown(holder.textView, tableMarkdown)
                }
                
                // 延迟检查渲染结果
                holder.textView.post {
                    AppLog.d("TableEntry: 渲染后TextView文本长度: ${holder.textView.text?.length}")
                    AppLog.d("TableEntry: TextView宽度: ${holder.textView.width}")
                    
                    // 检查是否有表格相关的Spans
                    if (holder.textView.text is android.text.Spanned) {
                        val spannable = holder.textView.text as android.text.Spanned
                        val spans = spannable.getSpans(0, spannable.length, Any::class.java)
                        AppLog.d("TableEntry: TextView Spans数量: ${spans.size}")
                    }
                }
                
                // 确保可见性
                holder.textView.visibility = android.view.View.VISIBLE
                
                // 智能优化表格显示
                optimizeTableDisplay(holder, node)
                
            } catch (e: Exception) {
                AppLog.e("TableEntry: 表格渲染错误", e)
                holder.textView.text = "表格渲染失败: ${e.message}"
                holder.textView.visibility = android.view.View.VISIBLE
            }
        }
        
        override fun clear() {
            cache.clear()
            AppLog.d("TableEntry: 清除缓存")
        }
        
        /**
         * 预加载节点 - 提前渲染并缓存表格
         */
        fun preloadNode(markwon: Markwon, node: Node) {
            if (node is TableBlock && !cache.containsKey(node)) {
                try {
                    val renderStartTime = System.currentTimeMillis()
                    val spanned = markwon.render(node)
                    val renderTime = System.currentTimeMillis() - renderStartTime
                    
                    cache[node] = spanned
                    CachePerformanceAnalyzer.recordCacheMiss("TableEntry_Preload", renderTime)
                    CachePerformanceAnalyzer.updateCacheSize("TableEntry", cache.size)
                    AppLog.d("TableEntry: 预加载表格完成，耗时=${renderTime}ms")
                } catch (e: Exception) {
                    AppLog.e("TableEntry: 预加载表格失败", e)
                }
            }
        }
        
        /**
         * 智能优化表格显示
         */
        private fun optimizeTableDisplay(holder: TableHolder, tableBlock: TableBlock) {
            try {
                val columnCount = detectTableColumns(tableBlock)
                AppLog.d("TableEntry: 检测到表格列数: $columnCount")
                
                val scrollView = findHorizontalScrollView(holder.itemView)
                val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels
                
                when {
                    columnCount <= 3 -> {
                        // 3列及以下表格的处理
                        val minWidth = (screenWidth * 0.8).toInt()
                        holder.textView.minWidth = minWidth
                        
                        holder.textView.post {
                            val availableWidth = scrollView?.width ?: screenWidth
                            val textViewWidth = holder.textView.width
                            
                            if (textViewWidth <= availableWidth && availableWidth > 0) {
                                scrollView?.isHorizontalScrollBarEnabled = false
                                AppLog.d("TableEntry: ${columnCount}列表格适合屏幕，禁用滚动条")
                            } else {
                                scrollView?.isHorizontalScrollBarEnabled = true
                                AppLog.d("TableEntry: ${columnCount}列表格需要滚动")
                            }
                        }
                    }
                    columnCount > 3 -> {
                        // 超过3列的表格
                        val minWidth = (screenWidth * 1.2).toInt()
                        holder.textView.minWidth = minWidth
                        scrollView?.isHorizontalScrollBarEnabled = true
                        AppLog.d("TableEntry: 多列表格(${columnCount}列)，启用滚动条")
                    }
                    else -> {
                        // 检测失败的情况
                        val minWidth = (screenWidth * 0.8).toInt()
                        holder.textView.minWidth = minWidth
                        AppLog.d("TableEntry: 未知列数，设置默认宽度")
                    }
                }
            } catch (e: Exception) {
                AppLog.e("TableEntry: 表格显示优化失败", e)
            }
        }
        
        /**
         * 设置表格的水平滚动效果
         */
        @SuppressLint("ClickableViewAccessibility")
        private fun setupTableScrolling(holder: TableHolder) {
            val scrollView = findHorizontalScrollView(holder.itemView) ?: return
            
            var startX = 0f
            var startY = 0f
            var isHorizontalScroll = false
            
            scrollView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isHorizontalScroll = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = abs(event.x - startX)
                        val deltaY = abs(event.y - startY)
                        
                        // 判断是否为水平滑动（水平距离大于垂直距离且超过阈值）
                        if (deltaX > deltaY && deltaX > 20) {
                            if (!isHorizontalScroll) {
                                isHorizontalScroll = true
                                // 只在水平滑动时请求父视图不要拦截触摸事件
                                scrollView.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        // 触摸结束时，恢复父视图的触摸事件处理
                        scrollView.parent?.requestDisallowInterceptTouchEvent(false)
                        isHorizontalScroll = false
                    }
                }
                false // 返回false，让HorizontalScrollView继续处理滑动
            }
        }
        
        /**
         * 检测表格列数
         */
        private fun detectTableColumns(tableBlock: TableBlock): Int {
            return try {
                // 获取表格头部来确定列数
                var child = tableBlock.firstChild
                while (child != null) {
                    if (child is TableHead) {
                        var headerRow = child.firstChild
                        while (headerRow != null) {
                            if (headerRow is TableRow) {
                                var columnCount = 0
                                var cell = headerRow.firstChild
                                while (cell != null) {
                                    if (cell is TableCell) {
                                        columnCount++
                                    }
                                    cell = cell.next
                                }
                                return columnCount
                            }
                            headerRow = headerRow.next
                        }
                    }
                    child = child.next
                }
                0
            } catch (e: Exception) {
                AppLog.d("TableEntry: 检测表格列数出错: ${e.message}")
                0
            }
        }
        
        /**
         * 查找HorizontalScrollView
         */
        private fun findHorizontalScrollView(view: android.view.View): HorizontalScrollView? {
            if (view is HorizontalScrollView) {
                return view
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findHorizontalScrollView(child)
                    if (result != null) {
                        return result
                    }
                }
            }
            return null
        }
        
        /**
         * 将TableBlock转换为Markdown字符串
         */
        private fun convertTableToMarkdown(tableBlock: TableBlock): String {
            val sb = StringBuilder()
            var columnCount = 0
            
            var child = tableBlock.firstChild
            while (child != null) {
                when (child) {
                    is TableHead -> {
                        var headerRow = child.firstChild
                        while (headerRow != null) {
                            if (headerRow is TableRow) {
                                sb.append("|")
                                var cell = headerRow.firstChild
                                while (cell != null) {
                                    if (cell is TableCell) {
                                        sb.append(" ")
                                        var textNode = cell.firstChild
                                        while (textNode != null) {
                                            if (textNode is Text) {
                                                sb.append(textNode.literal)
                                            }
                                            textNode = textNode.next
                                        }
                                        sb.append(" |")
                                        columnCount++
                                    }
                                    cell = cell.next
                                }
                                sb.append("\n")
                                
                                // 添加分隔行
                                sb.append("|")
                                repeat(columnCount) {
                                    sb.append(" --- |")
                                }
                                sb.append("\n")
                            }
                            headerRow = headerRow.next
                        }
                    }
                    is org.commonmark.ext.gfm.tables.TableBody -> {
                        var bodyRow = child.firstChild
                        while (bodyRow != null) {
                            if (bodyRow is TableRow) {
                                sb.append("|")
                                var cell = bodyRow.firstChild
                                while (cell != null) {
                                    if (cell is TableCell) {
                                        sb.append(" ")
                                        var textNode = cell.firstChild
                                        while (textNode != null) {
                                            if (textNode is Text) {
                                                sb.append(textNode.literal)
                                            }
                                            textNode = textNode.next
                                        }
                                        sb.append(" |")
                                    }
                                    cell = cell.next
                                }
                                sb.append("\n")
                            }
                            bodyRow = bodyRow.next
                        }
                    }
                }
                child = child.next
            }
            
            return sb.toString()
        }
        
        class TableHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val textView: TextView = requireView(R.id.text)
        }
    }
    
    /**
     * 引用块 Entry - 支持缓存优化
     */
    class BlockQuoteEntry : MarkwonAdapter.Entry<BlockQuote, BlockQuoteEntry.BlockQuoteHolder>() {
        
        // 缓存已渲染的引用块节点
        private val cache: MutableMap<BlockQuote, Spanned> = HashMap()
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): BlockQuoteHolder {
            return BlockQuoteHolder(inflater.inflate(R.layout.item_markwon_blockquote, parent, false))
        }
        
        override fun bindHolder(markwon: Markwon, holder: BlockQuoteHolder, node: BlockQuote) {
            // 检查缓存，避免重复渲染
            var spanned = cache[node]
            if (spanned == null) {
                val renderStartTime = System.currentTimeMillis()
                spanned = markwon.render(node)
                val renderTime = System.currentTimeMillis() - renderStartTime
                
                cache[node] = spanned
                CachePerformanceAnalyzer.recordCacheMiss("BlockQuoteEntry", renderTime)
                CachePerformanceAnalyzer.updateCacheSize("BlockQuoteEntry", cache.size)
                AppLog.d("BlockQuoteEntry: 新渲染引用块，缓存大小=${cache.size}, 耗时=${renderTime}ms")
            } else {
                CachePerformanceAnalyzer.recordCacheHit("BlockQuoteEntry")
                AppLog.d("BlockQuoteEntry: 使用缓存引用块")
            }
            
            markwon.setParsedMarkdown(holder.textView, spanned)
            
            holder.textView.setPadding(32, 16, 0, 16)
            holder.textView.setBackgroundColor(0x1A000000) // 半透明背景
        }
        
        override fun clear() {
            cache.clear()
            AppLog.d("BlockQuoteEntry: 清除缓存")
        }
        
        class BlockQuoteHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val textView: TextView = requireView(R.id.text)
        }
    }
    
    /**
     * 自定义容器 Entry - 支持复杂内容渲染（无需缓存，因为使用自定义渲染）
     */
    class ContainerEntry : MarkwonAdapter.Entry<ContainerNode, ContainerEntry.ContainerHolder>() {
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): ContainerHolder {
            return ContainerHolder(inflater.inflate(R.layout.item_markwon_container, parent, false))
        }
        
        override fun bindHolder(markwon: Markwon, holder: ContainerHolder, node: ContainerNode) {
            // 获取容器配置
            val config = ContainerNode.getConfig(node.containerType)
            
            AppLog.d("ContainerEntry: 渲染容器 type=${node.containerType}")
            
            // 设置容器标题和样式
            setupContainerHeader(holder, node, config)
            
            // 清除之前的内容
            holder.contentLayout.removeAllViews()
            
            // 分析和渲染容器内容
            analyzeAndRenderContent(holder, node, markwon)
        }
        
        /**
         * 设置容器标题和样式
         */
        private fun setupContainerHeader(holder: ContainerHolder, node: ContainerNode, config: ContainerNode.ContainerConfig?) {
            if (config != null) {
                holder.iconView.text = config.icon
                holder.titleView.text = node.title ?: config.title
                
                // 设置主题颜色和背景
                try {
                    val color = Color.parseColor(config.colorRes)
                    setContainerHeaderBackground(holder, color)
                    AppLog.d("ContainerEntry: 设置容器标题 - 类型: ${node.containerType}, 标题: ${node.title ?: config.title}, 颜色: ${config.colorRes}")
                } catch (e: Exception) {
                    AppLog.e("ContainerEntry: 颜色解析失败", e)
                    setContainerHeaderBackground(holder, Color.parseColor("#2196F3"))
                }
            } else {
                holder.iconView.text = "📋"
                holder.titleView.text = node.title ?: node.containerType
                setContainerHeaderBackground(holder, Color.parseColor("#2196F3"))
                AppLog.d("ContainerEntry: 使用默认容器样式")
            }
        }
        
        /**
         * 设置容器标题背景，包含圆角效果
         */
        private fun setContainerHeaderBackground(holder: ContainerHolder, color: Int) {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                cornerRadii = floatArrayOf(
                    12f, 12f,  // 左上角
                    12f, 12f,  // 右上角
                    0f, 0f,    // 右下角
                    0f, 0f     // 左下角
                )
            }
            holder.containerHeader.setBackgroundResource(0)
            holder.containerHeader.background = drawable
            
            // 确保文本颜色为白色以提供良好对比度
            holder.titleView.setTextColor(Color.WHITE)
            holder.iconView.setTextColor(Color.WHITE)
        }
        
        /**
         * 分析容器内容并创建对应的视图
         */
        private fun analyzeAndRenderContent(holder: ContainerHolder, containerNode: ContainerNode, markwon: Markwon) {
            val childNodeCount = getChildNodeCount(containerNode)
            AppLog.d("ContainerEntry: 开始分析容器内容，子节点数量: $childNodeCount")
            
            // 如果容器没有子节点，显示空内容提示
            if (childNodeCount == 0) {
                AppLog.d("ContainerEntry: 容器没有子节点，显示空内容提示")
                addEmptyContentView(holder)
                return
            }
            
            var child = containerNode.firstChild
            var processedNodeCount = 0
            
            while (child != null) {
                AppLog.d("ContainerEntry: 处理子节点类型: ${child.javaClass.simpleName}")
                
                try {
                    when (child) {
                        is Paragraph -> {
                            addParagraphView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is Heading -> {
                            addHeadingView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is FencedCodeBlock, is IndentedCodeBlock -> {
                            addCodeBlockView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is TableBlock -> {
                            addTableView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is BulletList, is OrderedList -> {
                            AppLog.d("ContainerEntry: 找到列表节点: ${child.javaClass.simpleName}")
                            addListView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is BlockQuote -> {
                            addBlockQuoteView(holder, child, markwon)
                            processedNodeCount++
                        }
                        is ThematicBreak -> {
                            addThematicBreakView(holder, child, markwon)
                            processedNodeCount++
                        }
                        else -> {
                            // 其他类型作为段落处理
                            AppLog.d("ContainerEntry: 未知节点类型作为段落处理: ${child.javaClass.simpleName}")
                            addParagraphView(holder, child, markwon)
                            processedNodeCount++
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("ContainerEntry: 渲染子节点时出错", e)
                }
                
                child = child.next
            }
            
            AppLog.d("ContainerEntry: 容器内容分析完成，期望处理 $childNodeCount 个节点，实际处理 $processedNodeCount 个，创建了 ${holder.contentLayout.childCount} 个子视图")
            
            // 只有在没有成功创建任何子视图时才显示空内容提示
            if (holder.contentLayout.childCount == 0) {
                AppLog.d("ContainerEntry: 警告：虽然有子节点但没有成功创建任何视图，显示空内容提示")
                addEmptyContentView(holder)
            }
        }
        
        /**
         * 计算子节点数量
         */
        private fun getChildNodeCount(node: Node): Int {
            var count = 0
            var child = node.firstChild
            while (child != null) {
                count++
                child = child.next
            }
            return count
        }
        
        /**
         * 添加段落视图
         */
        private fun addParagraphView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            val nodeContent = reconstructMarkdownFromNode(node)
            AppLog.d("ContainerEntry: 段落内容: ${nodeContent.take(100)}...")
            
            val textView = createBaseTextView(holder.itemView.context)
            
            // 检查是否包含数学公式
            if (containsMathFormula(nodeContent)) {
                MarkdownUtils.renderEnhancedToTextView(textView, nodeContent)
            } else {
                val rendered = markwon.render(node)
                markwon.setParsedMarkdown(textView, rendered)
            }
            
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 添加标题视图
         */
        private fun addHeadingView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            val textView = createBaseTextView(holder.itemView.context)
            val rendered = markwon.render(node)
            markwon.setParsedMarkdown(textView, rendered)
            
            // 根据标题级别调整样式
            if (node is Heading) {
                val textSize = when (node.level) {
                    1 -> 18f
                    2 -> 16f
                    3 -> 15f
                    4 -> 14f
                    5 -> 13f
                    6 -> 12f
                    else -> 11f
                }
                textView.textSize = textSize
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 添加代码块视图
         */
        private fun addCodeBlockView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            val code = when (node) {
                is FencedCodeBlock -> node.literal ?: ""
                is IndentedCodeBlock -> node.literal ?: ""
                else -> ""
            }
            
            val language = if (node is FencedCodeBlock) node.info else null
            
            // 检查是否为 Mermaid 图表
            if (language?.lowercase() == "mermaid") {
                addMermaidView(holder, code)
            } else {
                addCodeView(holder, code, language)
            }
        }
        
        /**
         * 添加代码视图
         */
        private fun addCodeView(holder: ContainerHolder, code: String, language: String?) {
            val codeDisplayView = CodeDisplayView(holder.itemView.context)
            codeDisplayView.setCode(code, language ?: "")
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            codeDisplayView.layoutParams = layoutParams
            
            holder.contentLayout.addView(codeDisplayView)
        }
        
        /**
         * 添加 Mermaid 图表视图
         */
        private fun addMermaidView(holder: ContainerHolder, mermaidContent: String) {
            val mermaidDisplayView = MermaidDisplayView(holder.itemView.context)
            mermaidDisplayView.setMermaidContent(mermaidContent) { success, error ->
                if (!success) {
                    AppLog.e("ContainerEntry: Mermaid 图表渲染失败: $error")
                }
            }
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            mermaidDisplayView.layoutParams = layoutParams
            
            holder.contentLayout.addView(mermaidDisplayView)
        }
        
        /**
         * 添加表格视图
         */
        private fun addTableView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            AppLog.d("ContainerEntry: addTableView 被调用，节点类型: ${node.javaClass.simpleName}")
            
            try {
                val textView = createBaseTextView(holder.itemView.context)
                
                // 设置表格专用的移动方法
                try {
                    textView.movementMethod = io.noties.markwon.ext.tables.TableAwareMovementMethod.create()
                } catch (e: Exception) {
                    AppLog.d("ContainerEntry: TableAwareMovementMethod不可用，使用默认方法")
                }
                
                val rendered = markwon.render(node)
                markwon.setParsedMarkdown(textView, rendered)
                
                holder.contentLayout.addView(textView)
                
                AppLog.d("ContainerEntry: 表格视图已成功添加")
                
            } catch (e: Exception) {
                AppLog.e("ContainerEntry: 表格渲染失败", e)
                val textView = createBaseTextView(holder.itemView.context)
                textView.text = "表格渲染失败: ${e.message}"
                textView.setTextColor(Color.RED)
                holder.contentLayout.addView(textView)
            }
        }
        
        /**
         * 添加列表视图
         */
        private fun addListView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            AppLog.d("ContainerEntry: 开始渲染列表，类型: ${node.javaClass.simpleName}")
            
            val textView = createBaseTextView(holder.itemView.context)
            
            // 为列表设置特殊的样式，确保有足够的左边距
            textView.setPadding(24, 8, 0, 8)
            
            val rendered = markwon.render(node)
            markwon.setParsedMarkdown(textView, rendered)
            
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 添加引用块视图
         */
        private fun addBlockQuoteView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            val textView = createBaseTextView(holder.itemView.context)
            val rendered = markwon.render(node)
            markwon.setParsedMarkdown(textView, rendered)
            
            // 添加引用样式
            textView.setPadding(32, 16, 0, 16)
            textView.setBackgroundColor(0x1A000000) // 半透明背景
            
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 添加分隔线视图
         */
        private fun addThematicBreakView(holder: ContainerHolder, node: Node, markwon: Markwon) {
            val textView = createBaseTextView(holder.itemView.context)
            val rendered = markwon.render(node)
            markwon.setParsedMarkdown(textView, rendered)
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 添加空内容提示
         */
        private fun addEmptyContentView(holder: ContainerHolder) {
            val textView = createBaseTextView(holder.itemView.context)
            textView.text = "此容器暂无内容"
            textView.alpha = 0.6f
            textView.gravity = android.view.Gravity.CENTER
            holder.contentLayout.addView(textView)
        }
        
        /**
         * 创建基础文本视图
         */
        private fun createBaseTextView(context: android.content.Context): TextView {
            return TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                    bottomMargin = 4
                }
                textSize = 14f
                setLineSpacing(4f, 1.2f)
                setPadding(0, 8, 0, 8)
            }
        }
        
        /**
         * 从节点重构 Markdown 内容
         */
        private fun reconstructMarkdownFromNode(node: Node): String {
            val result = processNodeToMarkdown(node)
            return fixLatexEscaping(result)
        }
        
        /**
         * 递归处理节点转换为 Markdown
         */
        private fun processNodeToMarkdown(node: Node): String {
            val content = StringBuilder()
            
            when (node) {
                is Text -> {
                    content.append(node.literal)
                }
                is Code -> {
                    content.append("`").append(node.literal).append("`")
                }
                is Emphasis -> {
                    content.append("*")
                    processChildren(node, content)
                    content.append("*")
                }
                is StrongEmphasis -> {
                    content.append("**")
                    processChildren(node, content)
                    content.append("**")
                }
                is Link -> {
                    content.append("[")
                    processChildren(node, content)
                    content.append("](").append(node.destination).append(")")
                }
                is Image -> {
                    content.append("![")
                    processChildren(node, content)
                    content.append("](").append(node.destination).append(")")
                }
                is HardLineBreak -> {
                    content.append("\n")
                }
                is SoftLineBreak -> {
                    content.append("\n")
                }
                is HtmlInline -> {
                    content.append(node.literal)
                }
                else -> {
                    processChildren(node, content)
                }
            }
            
            return content.toString()
        }
        
        /**
         * 处理子节点
         */
        private fun processChildren(parentNode: Node, content: StringBuilder) {
            var child = parentNode.firstChild
            while (child != null) {
                content.append(processNodeToMarkdown(child))
                child = child.next
            }
        }
        
        /**
         * 修复 LaTeX 转义问题
         */
        private fun fixLatexEscaping(content: String): String {
            if (!content.contains("$$")) {
                return content
            }
            
            return content.replace(Regex("(\\$\\$[\\s\\S]*?\\$\\$)")) { match ->
                val mathContent = match.value
                mathContent.replace(Regex("\\\\\\s*\n")) { match ->
                    "\\\\\\\\${match.value.substring(1)}"
                }.replace(Regex("([^\\\\])\\\\(\\s*\n)")) { match ->
                    "${match.groupValues[1]}\\\\\\\\${match.groupValues[2]}"
                }
            }
        }
        
        /**
         * 检查文本是否包含数学公式
         */
        private fun containsMathFormula(text: String): Boolean {
            val mathPatterns = listOf(
                "\\$\\$[\\s\\S]*?\\$\\$",  // 块级公式 $$...$$
                "\\$[^\\$\\n]*?\\$",      // 行内公式 $...$
                "\\\\\\([\\s\\S]*?\\\\\\)", // LaTeX 行内公式 \(...\)
                "\\\\\\[[\\s\\S]*?\\\\\\]", // LaTeX 块级公式 \[...\]
                "\\\\[a-zA-Z]+",          // LaTeX 命令
                "\\\\(frac|sqrt|sum|int|lim|infty|partial|nabla|alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|omega|begin|end|pmatrix|bmatrix|vmatrix|matrix)" // 常见数学符号和矩阵
            )
            
            return mathPatterns.any { pattern ->
                try {
                    text.contains(Regex(pattern))
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        override fun onViewRecycled(holder: ContainerHolder) {
            super.onViewRecycled(holder)
            // 清理内容以避免内存泄漏
            holder.contentLayout.removeAllViews()
        }
        
        class ContainerHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val containerHeader: LinearLayout = requireView(R.id.containerHeader)
            val iconView: TextView = requireView(R.id.containerIcon)
            val titleView: TextView = requireView(R.id.containerTitle)
            val contentLayout: LinearLayout = requireView(R.id.containerContent)
        }
    }
    
    /**
     * 智能段落 Entry - 支持数学公式检测和处理，支持缓存优化
     */
    class SmartParagraphEntry : MarkwonAdapter.Entry<Node, SmartParagraphEntry.SmartParagraphHolder>() {
        
        // 缓存已渲染的段落节点
        private val cache: MutableMap<Node, Spanned> = HashMap()
        
        override fun createHolder(inflater: LayoutInflater, parent: ViewGroup): SmartParagraphHolder {
            // 创建一个容器，在运行时决定使用哪个布局
            val container = android.widget.FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return SmartParagraphHolder(container)
        }
        
        override fun bindHolder(markwon: Markwon, holder: SmartParagraphHolder, node: Node) {
            // 清除之前的内容
            holder.container.removeAllViews()
            
            // 检测是否为数学公式段落
            val isMathFormula = when (node) {
                is Paragraph -> MathUtils.isBlockMathFormula(node)
                else -> false
            }
            
            // 额外检查：通过重构内容来检测数学公式
            val nodeContent = reconstructMarkdownFromNode(node)
            val containsMath = containsMathFormula(nodeContent)
            
            AppLog.d("SmartParagraphEntry: 检测节点类型=${node.javaClass.simpleName}, MathUtils检测=$isMathFormula, 内容检测=$containsMath")
            
            // 统一使用 item_markwon_simple 布局
            val inflater = LayoutInflater.from(holder.container.context)
            val itemView = inflater.inflate(R.layout.item_markwon_simple, holder.container, false)
            val textView = itemView.findViewById<TextView>(R.id.text)
            
            // 渲染内容
            if (containsMath) {
                // 使用增强渲染处理数学公式（不使用缓存，因为可能有动态内容）
                AppLog.d("SmartParagraphEntry: 使用增强渲染处理数学公式")
                MarkdownUtils.renderEnhancedToTextView(textView, nodeContent)
            } else {
                // 检查缓存，避免重复渲染
                var spanned = cache[node]
                if (spanned == null) {
                    val renderStartTime = System.currentTimeMillis()
                    spanned = markwon.render(node)
                    val renderTime = System.currentTimeMillis() - renderStartTime
                    
                    cache[node] = spanned
                    CachePerformanceAnalyzer.recordCacheMiss("SmartParagraphEntry", renderTime)
                    CachePerformanceAnalyzer.updateCacheSize("SmartParagraphEntry", cache.size)
                    AppLog.d("SmartParagraphEntry: 新渲染段落，缓存大小=${cache.size}, 耗时=${renderTime}ms")
                } else {
                    CachePerformanceAnalyzer.recordCacheHit("SmartParagraphEntry")
                    AppLog.d("SmartParagraphEntry: 使用缓存段落")
                }
                markwon.setParsedMarkdown(textView, spanned)
            }
            
            // 为数学公式添加特殊处理
            if (isMathFormula || containsMath) {
                textView.gravity = android.view.Gravity.START
                textView.textSize = 18f  // 稍大的字体
                // 可以添加更多数学公式特定的样式
                textView.setPadding(
                    textView.paddingLeft,
                    textView.paddingTop + 8,
                    textView.paddingRight,
                    textView.paddingBottom + 8
                )
                AppLog.d("SmartParagraphEntry: 应用数学公式样式")
            }
            
            holder.container.addView(itemView)
        }
        
        override fun clear() {
            cache.clear()
            AppLog.d("SmartParagraphEntry: 清除缓存")
        }
        
        /**
         * 预加载节点 - 提前渲染并缓存段落（不包含数学公式的）
         */
        fun preloadNode(markwon: Markwon, node: Node) {
            if (!cache.containsKey(node)) {
                try {
                    // 检查是否包含数学公式，数学公式不适合预加载（动态内容）
                    val nodeContent = reconstructMarkdownFromNode(node)
                    val containsMath = containsMathFormula(nodeContent)
                    
                    if (!containsMath) {
                        val renderStartTime = System.currentTimeMillis()
                        val spanned = markwon.render(node)
                        val renderTime = System.currentTimeMillis() - renderStartTime
                        
                        cache[node] = spanned
                        CachePerformanceAnalyzer.recordCacheMiss("SmartParagraphEntry_Preload", renderTime)
                        CachePerformanceAnalyzer.updateCacheSize("SmartParagraphEntry", cache.size)
                        AppLog.d("SmartParagraphEntry: 预加载段落完成，耗时=${renderTime}ms")
                    } else {
                        AppLog.d("SmartParagraphEntry: 跳过数学公式段落的预加载")
                    }
                } catch (e: Exception) {
                    AppLog.e("SmartParagraphEntry: 预加载段落失败", e)
                }
            }
        }
        
        /**
         * 从节点重构 Markdown 内容
         */
        private fun reconstructMarkdownFromNode(node: Node): String {
            val result = processNodeToMarkdown(node)
            return fixLatexEscaping(result)
        }
        
        /**
         * 递归处理节点转换为 Markdown
         */
        private fun processNodeToMarkdown(node: Node): String {
            val content = StringBuilder()
            
            when (node) {
                is Text -> {
                    content.append(node.literal)
                }
                is Code -> {
                    content.append("`").append(node.literal).append("`")
                }
                is Emphasis -> {
                    content.append("*")
                    processChildren(node, content)
                    content.append("*")
                }
                is StrongEmphasis -> {
                    content.append("**")
                    processChildren(node, content)
                    content.append("**")
                }
                is Link -> {
                    content.append("[")
                    processChildren(node, content)
                    content.append("](").append(node.destination).append(")")
                }
                is Image -> {
                    content.append("![")
                    processChildren(node, content)
                    content.append("](").append(node.destination).append(")")
                }
                is HardLineBreak -> {
                    content.append("\n")
                }
                is SoftLineBreak -> {
                    content.append("\n")
                }
                is HtmlInline -> {
                    content.append(node.literal)
                }
                else -> {
                    processChildren(node, content)
                }
            }
            
            return content.toString()
        }
        
        /**
         * 处理子节点
         */
        private fun processChildren(parentNode: Node, content: StringBuilder) {
            var child = parentNode.firstChild
            while (child != null) {
                content.append(processNodeToMarkdown(child))
                child = child.next
            }
        }
        
        /**
         * 修复 LaTeX 转义问题
         */
        private fun fixLatexEscaping(content: String): String {
            if (!content.contains("$$")) {
                return content
            }
            
            return content.replace(Regex("(\\$\\$[\\s\\S]*?\\$\\$)")) { match ->
                val mathContent = match.value
                mathContent.replace(Regex("\\\\\\s*\n")) { match ->
                    "\\\\\\\\${match.value.substring(1)}"
                }.replace(Regex("([^\\\\])\\\\(\\s*\n)")) { match ->
                    "${match.groupValues[1]}\\\\\\\\${match.groupValues[2]}"
                }
            }
        }
        
        /**
         * 检查文本是否包含数学公式
         */
        private fun containsMathFormula(text: String): Boolean {
            val mathPatterns = listOf(
                "\\$\\$[\\s\\S]*?\\$\\$",  // 块级公式 $$...$$
                "\\$[^\\$\\n]*?\\$",      // 行内公式 $...$
                "\\\\\\([\\s\\S]*?\\\\\\)", // LaTeX 行内公式 \(...\)
                "\\\\\\[[\\s\\S]*?\\\\\\]", // LaTeX 块级公式 \[...\]
                "\\\\[a-zA-Z]+",          // LaTeX 命令
                "\\\\(frac|sqrt|sum|int|lim|infty|partial|nabla|alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|omega|begin|end|pmatrix|bmatrix|vmatrix|matrix)" // 常见数学符号和矩阵
            )
            
            return mathPatterns.any { pattern ->
                try {
                    text.contains(Regex(pattern))
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        override fun onViewRecycled(holder: SmartParagraphHolder) {
            super.onViewRecycled(holder)
            holder.container.removeAllViews()
        }
        
        class SmartParagraphHolder(itemView: android.view.View) : MarkwonAdapter.Holder(itemView) {
            val container: android.widget.FrameLayout = itemView as android.widget.FrameLayout
        }
    }
    
    /**
     * 智能预加载 - 核心方法
     * @param adapter 当前适配器实例
     * @param markwon Markwon 渲染实例
     * @param markdownText 原始 Markdown 文本
     */
    fun enableIntelligentPreloading(adapter: MarkwonAdapter, markwon: Markwon, markdownText: String) {
        try {
            currentMarkwon = markwon
            
            // 解析文档以进行内容分析
            val parser = org.commonmark.parser.Parser.builder().build()
            currentDocument = parser.parse(markdownText) as Document
            
            AppLog.d("MarkwonMultiTypeAdapter: 启用智能预加载，文档节点数: ${countNodes(currentDocument)}")
            
            // 按优先级预加载昂贵内容
            preloadExpensiveContentByPriority(markwon, currentDocument)
            
        } catch (e: Exception) {
            AppLog.e("MarkwonMultiTypeAdapter: 智能预加载启用失败", e)
        }
    }
    
    /**
     * 滚动预测预加载 - 在即将进入视口时预加载
     * @param upcomingStartPosition 即将显示的起始位置
     * @param preloadCount 预加载的项目数量
     */
    fun preloadUpcomingContent(upcomingStartPosition: Int, preloadCount: Int) {
        currentDocument?.let { document ->
            currentMarkwon?.let { markwon ->
                preloadScope.launch {
                    try {
                        AppLog.d("MarkwonMultiTypeAdapter: 预加载即将显示的内容，位置: $upcomingStartPosition, 数量: $preloadCount")
                        
                        val nodes = getNodesForPositionRange(document, upcomingStartPosition, preloadCount)
                        preloadSpecificNodes(markwon, nodes)
                        
                    } catch (e: Exception) {
                        AppLog.e("MarkwonMultiTypeAdapter: 滚动预加载失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 按优先级预加载昂贵内容
     */
    private fun preloadExpensiveContentByPriority(markwon: Markwon, document: Document?) {
        document?.let { doc ->
            preloadScope.launch {
                try {
                    // 立即预加载：Mermaid 图表（最耗时）
                    preloadContentType(markwon, doc, ContentType.MERMAID)
                    
                    // 延迟预加载：复杂数学公式
                    delay(50)
                    preloadContentType(markwon, doc, ContentType.MATH_FORMULAS)
                    
                    // 更多延迟：表格和代码块
                    delay(100)
                    preloadContentType(markwon, doc, ContentType.LARGE_TABLES)
                    preloadContentType(markwon, doc, ContentType.CODE_BLOCKS)
                    
                    AppLog.d("MarkwonMultiTypeAdapter: 优先级预加载完成")
                    
                } catch (e: Exception) {
                    AppLog.e("MarkwonMultiTypeAdapter: 优先级预加载失败", e)
                }
            }
        }
    }
    
    /**
     * 预加载特定内容类型
     */
    private fun preloadContentType(markwon: Markwon, document: Document, contentType: ContentType) {
        when (contentType) {
            ContentType.MERMAID -> preloadMermaidDiagrams(document)
            ContentType.MATH_FORMULAS -> preloadMathFormulas(markwon, document)
            ContentType.LARGE_TABLES -> preloadLargeTables(markwon, document)
            ContentType.CODE_BLOCKS -> preloadCodeBlocks(markwon, document)
            ContentType.PARAGRAPHS -> preloadParagraphs(markwon, document)
            ContentType.HEADINGS -> preloadHeadings(markwon, document)
        }
    }
    
    /**
     * 预加载 Mermaid 图表 - 最高优先级
     */
    private fun preloadMermaidDiagrams(document: Document) {
        val mermaidNodes = findNodesByType(document) { node ->
            node is FencedCodeBlock && node.info?.lowercase() == "mermaid"
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${mermaidNodes.size} 个 Mermaid 图表，开始预加载")
        
        mermaidNodes.forEach { node ->
            if (node is FencedCodeBlock) {
                val content = node.literal ?: ""
                val cacheKey = MermaidRenderCache.generateCacheKey(content)
                
                // 如果还没有缓存，则标记为开始渲染（预加载）
                if (MermaidRenderCache.getRenderState(cacheKey) == MermaidRenderCache.MermaidRenderState.NONE) {
                    MermaidRenderCache.markRenderingStart(cacheKey)
                    AppLog.d("MarkwonMultiTypeAdapter: 预加载 Mermaid 图表: ${content.take(50)}...")
                }
            }
        }
    }
    
    /**
     * 预加载数学公式
     */
    private fun preloadMathFormulas(markwon: Markwon, document: Document) {
        val mathNodes = findNodesByType(document) { node ->
            node is Paragraph && containsMathContent(node)
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${mathNodes.size} 个数学公式段落，开始预加载")
        
        mathNodes.forEach { node ->
            currentEntries?.forEach { entry ->
                if (entry is SmartParagraphEntry) {
                    entry.preloadNode(markwon, node)
                }
            }
        }
    }
    
    /**
     * 预加载大型表格
     */
    private fun preloadLargeTables(markwon: Markwon, document: Document) {
        val tableNodes = findNodesByType(document) { node ->
            node is TableBlock && isLargeTable(node)
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${tableNodes.size} 个大型表格，开始预加载")
        
        tableNodes.forEach { node ->
            currentEntries?.forEach { entry ->
                if (entry is TableEntry) {
                    entry.preloadNode(markwon, node)
                }
            }
        }
    }
    
    /**
     * 预加载代码块
     */
    private fun preloadCodeBlocks(markwon: Markwon, document: Document) {
        val codeNodes = findNodesByType(document) { node ->
            (node is FencedCodeBlock || node is IndentedCodeBlock) && 
            !(node is FencedCodeBlock && node.info?.lowercase() == "mermaid")
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${codeNodes.size} 个代码块，开始预加载")
        
        // 代码块预加载主要是语法高亮处理，相对简单
        codeNodes.forEach { node ->
            // 可以在这里进行语法高亮的预处理
            AppLog.d("MarkwonMultiTypeAdapter: 预加载代码块")
        }
    }
    
    /**
     * 预加载段落
     */
    private fun preloadParagraphs(markwon: Markwon, document: Document) {
        val paragraphNodes = findNodesByType(document) { node ->
            node is Paragraph && !containsMathContent(node)
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${paragraphNodes.size} 个普通段落，开始预加载")
        
        paragraphNodes.forEach { node ->
            currentEntries?.forEach { entry ->
                if (entry is SmartParagraphEntry) {
                    entry.preloadNode(markwon, node)
                }
            }
        }
    }
    
    /**
     * 预加载标题
     */
    private fun preloadHeadings(markwon: Markwon, document: Document) {
        val headingNodes = findNodesByType(document) { node ->
            node is Heading
        }
        
        AppLog.d("MarkwonMultiTypeAdapter: 发现 ${headingNodes.size} 个标题，开始预加载")
        
        headingNodes.forEach { node ->
            currentEntries?.forEach { entry ->
                if (entry is HeadingEntry) {
                    entry.preloadNode(markwon, node)
                }
            }
        }
    }
    
    /**
     * 预加载指定的节点列表
     */
    private fun preloadSpecificNodes(markwon: Markwon, nodes: List<Node>) {
        nodes.forEach { node ->
            when (node) {
                is FencedCodeBlock -> {
                    if (node.info?.lowercase() == "mermaid") {
                        preloadMermaidDiagrams(Document().apply { appendChild(node) })
                    }
                }
                is TableBlock -> {
                    currentEntries?.filterIsInstance<TableEntry>()?.forEach { entry ->
                        entry.preloadNode(markwon, node)
                    }
                }
                is Paragraph -> {
                    currentEntries?.filterIsInstance<SmartParagraphEntry>()?.forEach { entry ->
                        entry.preloadNode(markwon, node)
                    }
                }
                is Heading -> {
                    currentEntries?.filterIsInstance<HeadingEntry>()?.forEach { entry ->
                        entry.preloadNode(markwon, node)
                    }
                }
            }
        }
    }
    
    /**
     * 工具方法：查找特定类型的节点
     */
    private fun findNodesByType(document: Document, predicate: (Node) -> Boolean): List<Node> {
        val results = mutableListOf<Node>()
        
        fun traverseNode(node: Node) {
            if (predicate(node)) {
                results.add(node)
            }
            
            var child = node.firstChild
            while (child != null) {
                traverseNode(child)
                child = child.next
            }
        }
        
        traverseNode(document)
        return results
    }
    
    /**
     * 工具方法：检查段落是否包含数学内容
     */
    private fun containsMathContent(node: Paragraph): Boolean {
        // 这里可以复用 SmartParagraphEntry 中的数学公式检测逻辑
        val content = reconstructMarkdownFromParagraph(node)
        return containsMathFormula(content)
    }
    
    /**
     * 工具方法：判断是否为大型表格
     */
    private fun isLargeTable(tableBlock: TableBlock): Boolean {
        var rowCount = 0
        var columnCount = 0
        
        var child = tableBlock.firstChild
        while (child != null) {
            when (child) {
                is TableHead -> {
                    var headerRow = child.firstChild
                    while (headerRow != null) {
                        if (headerRow is TableRow) {
                            var cell = headerRow.firstChild
                            while (cell != null) {
                                if (cell is TableCell) {
                                    columnCount++
                                }
                                cell = cell.next
                            }
                        }
                        headerRow = headerRow.next
                    }
                }
                is org.commonmark.ext.gfm.tables.TableBody -> {
                    var bodyRow = child.firstChild
                    while (bodyRow != null) {
                        if (bodyRow is TableRow) {
                            rowCount++
                        }
                        bodyRow = bodyRow.next
                    }
                }
            }
            child = child.next
        }
        
        // 定义大型表格：超过3列或超过5行
        return columnCount > 3 || rowCount > 5
    }
    
    /**
     * 工具方法：从段落重构 Markdown
     */
    private fun reconstructMarkdownFromParagraph(paragraph: Paragraph): String {
        val content = StringBuilder()
        var child = paragraph.firstChild
        while (child != null) {
            when (child) {
                is Text -> content.append(child.literal)
                is Code -> content.append("`").append(child.literal).append("`")
                // 可以添加更多节点类型处理
            }
            child = child.next
        }
        return content.toString()
    }
    
    /**
     * 工具方法：检查文本是否包含数学公式
     */
    private fun containsMathFormula(text: String): Boolean {
        val mathPatterns = listOf(
            "\\$\\$[\\s\\S]*?\\$\\$",  // 块级公式 $$...$$
            "\\$[^\\$\\n]*?\\$",      // 行内公式 $...$
            "\\\\\\([\\s\\S]*?\\\\\\)", // LaTeX 行内公式 \(...\)
            "\\\\\\[[\\s\\S]*?\\\\\\]", // LaTeX 块级公式 \[...\]
            "\\\\[a-zA-Z]+",          // LaTeX 命令
            "\\\\(frac|sqrt|sum|int|lim|infty|partial|nabla|alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|omega|begin|end|pmatrix|bmatrix|vmatrix|matrix)" // 常见数学符号和矩阵
        )
        
        return mathPatterns.any { pattern ->
            try {
                text.contains(Regex(pattern))
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 工具方法：计算文档中的节点数量
     */
    private fun countNodes(document: Document?): Int {
        if (document == null) return 0
        
        var count = 0
        fun traverseNode(node: Node) {
            count++
            var child = node.firstChild
            while (child != null) {
                traverseNode(child)
                child = child.next
            }
        }
        
        traverseNode(document)
        return count
    }
    
    /**
     * 工具方法：获取指定位置范围的节点
     */
    private fun getNodesForPositionRange(document: Document, startPosition: Int, count: Int): List<Node> {
        val allNodes = mutableListOf<Node>()
        
        fun collectNodes(node: Node) {
            allNodes.add(node)
            var child = node.firstChild
            while (child != null) {
                collectNodes(child)
                child = child.next
            }
        }
        
        collectNodes(document)
        
        val endPosition = (startPosition + count).coerceAtMost(allNodes.size)
        return if (startPosition < allNodes.size) {
            allNodes.subList(startPosition, endPosition)
        } else {
            emptyList()
        }
    }
    
    /**
     * 清除所有 Entry 的缓存
     * 这是外部调用的主要清理方法
     */
    fun clearAllCaches() {
        currentEntries?.forEach { entry ->
            try {
                entry.clear()
                AppLog.d("MarkwonMultiTypeAdapter: 清除了 ${entry.javaClass.simpleName} 缓存")
            } catch (e: Exception) {
                AppLog.e("MarkwonMultiTypeAdapter: 清除 ${entry.javaClass.simpleName} 缓存失败", e)
            }
        }
        AppLog.d("MarkwonMultiTypeAdapter: 所有 Entry 缓存清除完成")
    }
} 