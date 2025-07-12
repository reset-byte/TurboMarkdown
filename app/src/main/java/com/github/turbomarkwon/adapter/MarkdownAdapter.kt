package com.github.turbomarkwon.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.turbomarkwon.data.MarkdownItem
import com.github.turbomarkwon.databinding.ItemMarkdownCodeBlockBinding
import com.github.turbomarkwon.databinding.ItemMarkdownHeadingBinding
import com.github.turbomarkwon.databinding.ItemMarkdownListBinding
import com.github.turbomarkwon.databinding.ItemMarkdownParagraphBinding
import com.github.turbomarkwon.databinding.ItemMarkdownTableBinding
import com.github.turbomarkwon.renderer.MarkdownRenderer
import io.noties.markwon.Markwon
import com.github.turbomarkwon.util.AppLog
import com.github.turbomarkwon.views.CodeDisplayView
import com.github.turbomarkwon.views.MermaidDisplayView
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import android.view.View
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import com.github.turbomarkwon.util.MarkdownUtils
import kotlin.math.abs
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.node.*
import com.github.turbomarkwon.cache.MermaidRenderCache

/**
 * RecyclerView适配器 - 实现分块渲染Markdown内容
 */
class MarkdownAdapter(
    private val markwon: Markwon
) : ListAdapter<MarkdownItem, MarkdownAdapter.BaseViewHolder>(MarkdownDiffCallback()) {

    companion object {
        private const val TYPE_PARAGRAPH = 0
        private const val TYPE_HEADING = 1
        private const val TYPE_CODE_BLOCK = 2
        private const val TYPE_LIST_ITEM = 3
        private const val TYPE_TABLE = 4
        private const val TYPE_BLOCK_QUOTE = 5
        private const val TYPE_THEMATIC_BREAK = 6
        private const val TYPE_HTML_BLOCK = 7
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MarkdownItem.Paragraph -> TYPE_PARAGRAPH
            is MarkdownItem.Heading -> TYPE_HEADING
            is MarkdownItem.CodeBlock -> TYPE_CODE_BLOCK
            is MarkdownItem.ListItem -> TYPE_LIST_ITEM
            is MarkdownItem.Table -> TYPE_TABLE
            is MarkdownItem.BlockQuote -> TYPE_BLOCK_QUOTE
            is MarkdownItem.ThematicBreak -> TYPE_THEMATIC_BREAK
            is MarkdownItem.HtmlBlock -> TYPE_HTML_BLOCK
            else -> TYPE_PARAGRAPH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            TYPE_PARAGRAPH -> {
                val binding = ItemMarkdownParagraphBinding.inflate(layoutInflater, parent, false)
                ParagraphViewHolder(binding)
            }
            TYPE_HEADING -> {
                val binding = ItemMarkdownHeadingBinding.inflate(layoutInflater, parent, false)
                HeadingViewHolder(binding)
            }
            TYPE_CODE_BLOCK -> {
                val binding = ItemMarkdownCodeBlockBinding.inflate(layoutInflater, parent, false)
                CodeBlockViewHolder(binding)
            }
            TYPE_LIST_ITEM -> {
                val binding = ItemMarkdownListBinding.inflate(layoutInflater, parent, false)
                ListItemViewHolder(binding)
            }
            TYPE_TABLE -> {
                val binding = ItemMarkdownTableBinding.inflate(layoutInflater, parent, false)
                TableViewHolder(binding)
            }
            TYPE_BLOCK_QUOTE -> {
                val binding = ItemMarkdownParagraphBinding.inflate(layoutInflater, parent, false)
                BlockQuoteViewHolder(binding)
            }
            TYPE_THEMATIC_BREAK -> {
                val binding = ItemMarkdownParagraphBinding.inflate(layoutInflater, parent, false)
                ThematicBreakViewHolder(binding)
            }
            TYPE_HTML_BLOCK -> {
                val binding = ItemMarkdownParagraphBinding.inflate(layoutInflater, parent, false)
                HtmlBlockViewHolder(binding)
            }
            else -> {
                val binding = ItemMarkdownParagraphBinding.inflate(layoutInflater, parent, false)
                ParagraphViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        AppLog.d("MarkdownAdapter: 绑定项目类型: ${item.javaClass.simpleName}, ID: ${item.id}")
        holder.bind(item, markwon)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        // 取消任何正在进行的图片加载
        holder.onRecycled()
    }

    // 基础ViewHolder抽象类
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: MarkdownItem, markwon: Markwon)
        open fun onRecycled() {}
    }

    // 段落ViewHolder
    class ParagraphViewHolder(private val binding: ItemMarkdownParagraphBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.Paragraph) {
                // 重构节点内容为markdown格式，保留换行符和格式信息
                val nodeContent = reconstructMarkdownFromNode(item.node)
                
                // 添加调试日志
                AppLog.d("ParagraphViewHolder: 处理段落内容: ${nodeContent.take(100)}...")
                
                // 检查是否包含数学公式
                val hasMathFormula = containsMathFormula(nodeContent)
                AppLog.d("ParagraphViewHolder: 是否包含数学公式: $hasMathFormula")
                
                if (hasMathFormula) {
                    // 使用增强渲染处理数学公式
                    AppLog.d("ParagraphViewHolder: 使用增强渲染处理数学公式")
                    MarkdownUtils.renderEnhancedToTextView(binding.textView, nodeContent)
                } else {
                    // 使用常规渲染
                    AppLog.d("ParagraphViewHolder: 使用常规渲染")
                    MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
                }
            }
        }
        
        /**
         * 从节点重构markdown内容，保留格式信息
         */
        private fun reconstructMarkdownFromNode(node: Node): String {
            val result = processNodeToMarkdown(node)
            // 修复 LaTeX 反斜杠转义问题：将单个 \ 恢复为正确的 LaTeX 格式
            return fixLatexEscaping(result)
        }
        
        /**
         * 修复 LaTeX 转义问题
         */
        private fun fixLatexEscaping(content: String): String {
            // 对于包含数学公式的内容，修复反斜杠转义
            if (!content.contains("$$")) {
                return content
            }
            
            AppLog.d("ParagraphViewHolder: 修复前的内容: $content")
            
            // 在数学公式块中修复转义
            val result = content.replace(Regex("(\\$\\$[\\s\\S]*?\\$\\$)")) { match ->
                val mathContent = match.value
                AppLog.d("ParagraphViewHolder: 处理数学内容: $mathContent")
                
                // 专门修复矩阵换行符问题：将行末的单个 \ 修复为 \\
                val fixed = mathContent
                    // 修复矩阵/表格中的换行符：\ 后跟换行或空白应该是 \\
                    .replace(Regex("\\\\\\s*\n")) { match ->
                        "\\\\\\\\${match.value.substring(1)}"
                    }
                    // 修复其他行末的单个反斜杠
                    .replace(Regex("([^\\\\])\\\\(\\s*\n)")) { match ->
                        "${match.groupValues[1]}\\\\\\\\${match.groupValues[2]}"
                    }
                
                AppLog.d("ParagraphViewHolder: 修复后的数学内容: $fixed")
                fixed
            }
            
            AppLog.d("ParagraphViewHolder: 修复后的完整内容: $result")
            return result
        }
        
        /**
         * 递归处理节点转换为markdown
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
                    // 处理其他节点类型，直接处理子节点
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
         * 检查文本是否包含数学公式
         */
        private fun containsMathFormula(text: String): Boolean {
            // 检查常见的数学公式标记
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
    }

    // 标题ViewHolder
    class HeadingViewHolder(private val binding: ItemMarkdownHeadingBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.Heading) {
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
                
                // 根据标题级别调整文本大小
                val textSize = when (item.level) {
                    1 -> 20f
                    2 -> 18f
                    3 -> 17f
                    4 -> 16f
                    5 -> 14f
                    6 -> 12f
                    else -> 10f
                }
                binding.textView.textSize = textSize
            }
        }
    }

    // 代码块ViewHolder - 处理独立的代码块
    class CodeBlockViewHolder(private val binding: ItemMarkdownCodeBlockBinding) : BaseViewHolder(binding.root) {
        private var codeDisplayView: CodeDisplayView? = null
        private var mermaidDisplayView: MermaidDisplayView? = null
        private var currentCodeHash: String? = null
        // 新增：全局缓存键
        private var currentCacheKey: String? = null

        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.CodeBlock) {
                AppLog.d("Binding CodeBlock item id=${item.id}, language=${item.language}")
                
                // 根据语言类型判断是否为mermaid图表
                if (item.language?.lowercase() == "mermaid") {
                    showMermaidDiagram(item)
                } else {
                    showCodeBlock(item)
                }
            }
        }
        
        private fun showCodeBlock(codeBlockItem: MarkdownItem.CodeBlock) {
            // 隐藏普通文本视图和mermaid视图
            binding.textView.visibility = View.GONE
            cleanupMermaidView()
            
            // 显示代码容器
            binding.codeContainer.visibility = View.VISIBLE
            
            // 从节点中提取代码内容
            val code = when (val node = codeBlockItem.node) {
                is FencedCodeBlock -> node.literal ?: ""
                is IndentedCodeBlock -> node.literal ?: ""
                else -> ""
            }
            
            // 生成内容哈希值用于检查是否需要更新
            val codeHash = "${code.hashCode()}_${codeBlockItem.language}"
            
            // 创建或重用CodeDisplayView
            if (codeDisplayView == null) {
                codeDisplayView = CodeDisplayView(binding.root.context)
                binding.codeContainer.addView(codeDisplayView)
                AppLog.d("Created new CodeDisplayView for independent code block")
            }
            
            // 只有当内容发生变化时才更新CodeDisplayView
            if (currentCodeHash != codeHash) {
                codeDisplayView?.setCode(code, codeBlockItem.language ?: "")
                currentCodeHash = codeHash
                AppLog.d("Updated code content: language=${codeBlockItem.language}, length=${code.length}")
            } else {
                AppLog.d("Code content unchanged, skipping update")
            }
        }
        
        private fun showMermaidDiagram(codeBlockItem: MarkdownItem.CodeBlock) {
            // 隐藏普通文本视图和代码视图
            binding.textView.visibility = View.GONE
            cleanupCodeView()
            
            // 显示代码容器
            binding.codeContainer.visibility = View.VISIBLE
            
            // 从节点中提取Mermaid图表内容
            val mermaidContent = when (val node = codeBlockItem.node) {
                is FencedCodeBlock -> node.literal ?: ""
                is IndentedCodeBlock -> node.literal ?: ""
                else -> ""
            }
            
            // 生成全局缓存键
            val globalCacheKey = MermaidRenderCache.generateCacheKey(mermaidContent)
            currentCacheKey = globalCacheKey
            
            // 检查全局缓存状态
            when (MermaidRenderCache.getRenderState(globalCacheKey)) {
                MermaidRenderCache.MermaidRenderState.SUCCESS -> {
                    // 已成功渲染，可以直接复用
                    AppLog.d("Mermaid diagram already rendered in global cache, reusing")
                    reuseExistingMermaidView(mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.RENDERING -> {
                    // 正在渲染中，等待完成
                    AppLog.d("Mermaid diagram is being rendered, waiting...")
                    createMermaidViewAndWait(mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.ERROR -> {
                    // 之前渲染失败，重新尝试
                    AppLog.d("Previous render failed, retrying...")
                    createMermaidViewAndRender(mermaidContent, globalCacheKey)
                }
                MermaidRenderCache.MermaidRenderState.NONE -> {
                    // 首次渲染
                    AppLog.d("First time rendering this mermaid diagram")
                    createMermaidViewAndRender(mermaidContent, globalCacheKey)
                }
            }
        }
        
        /**
         * 复用已存在的 Mermaid 视图
         */
        private fun reuseExistingMermaidView(mermaidContent: String, cacheKey: String) {
            // 创建或重用MermaidDisplayView
            if (mermaidDisplayView == null) {
                mermaidDisplayView = MermaidDisplayView(binding.root.context)
                binding.codeContainer.addView(mermaidDisplayView)
                AppLog.d("Created new MermaidDisplayView for cached diagram")
            }
            
            // 设置内容，由于已缓存，内部会快速完成
            mermaidDisplayView?.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    AppLog.d("Successfully reused cached Mermaid diagram")
                } else {
                    AppLog.e("Failed to reuse cached Mermaid diagram: $error")
                    // 如果复用失败，标记为需要重新渲染
                    MermaidRenderCache.markRenderingError(cacheKey)
                }
            }
        }
        
        /**
         * 创建 Mermaid 视图并等待渲染完成
         */
        private fun createMermaidViewAndWait(mermaidContent: String, cacheKey: String) {
            // 创建或重用MermaidDisplayView
            if (mermaidDisplayView == null) {
                mermaidDisplayView = MermaidDisplayView(binding.root.context)
                binding.codeContainer.addView(mermaidDisplayView)
                AppLog.d("Created new MermaidDisplayView for waiting diagram")
            }
            
            // 设置内容并等待渲染
            mermaidDisplayView?.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    MermaidRenderCache.markRenderingSuccess(cacheKey)
                    AppLog.d("Mermaid diagram rendered successfully after waiting")
                } else {
                    MermaidRenderCache.markRenderingError(cacheKey)
                    AppLog.e("Mermaid diagram rendering failed after waiting: $error")
                }
            }
        }
        
        /**
         * 创建 Mermaid 视图并开始渲染
         */
        private fun createMermaidViewAndRender(mermaidContent: String, cacheKey: String) {
            // 创建或重用MermaidDisplayView
            if (mermaidDisplayView == null) {
                mermaidDisplayView = MermaidDisplayView(binding.root.context)
                binding.codeContainer.addView(mermaidDisplayView)
                AppLog.d("Created new MermaidDisplayView for new diagram")
            }
            
            // 标记开始渲染
            MermaidRenderCache.markRenderingStart(cacheKey)
            
            // 设置内容并开始渲染
            mermaidDisplayView?.setMermaidContent(mermaidContent) { success, error ->
                if (success) {
                    MermaidRenderCache.markRenderingSuccess(cacheKey)
                    AppLog.d("Mermaid diagram rendered successfully")
                } else {
                    MermaidRenderCache.markRenderingError(cacheKey)
                    AppLog.e("Mermaid diagram rendering failed: $error")
                }
            }
        }
        
        private fun cleanupCodeView() {
            if (codeDisplayView != null) {
                binding.codeContainer.removeView(codeDisplayView)
                codeDisplayView = null
            }
        }
        
        private fun cleanupMermaidView() {
            if (mermaidDisplayView != null) {
                mermaidDisplayView?.destroy()
                binding.codeContainer.removeView(mermaidDisplayView)
                mermaidDisplayView = null
            }
        }
        
        override fun onRecycled() {
            super.onRecycled()
            AppLog.d("CodeBlockViewHolder onRecycled, currentCodeHash: $currentCodeHash")
        }
    }

    // 列表项ViewHolder
    class ListItemViewHolder(private val binding: ItemMarkdownListBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.ListItem) {
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
                
                // 根据嵌套级别调整缩进
                val paddingStart = (item.level + 1) * 32
                binding.textView.setPadding(paddingStart, 0, 0, 0)
            }
        }
    }

    // 表格ViewHolder
    class TableViewHolder(private val binding: ItemMarkdownTableBinding) : BaseViewHolder(binding.root) {
        init {
            setupTableScrolling()
        }
        
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.Table) {
                AppLog.d("Binding Table item id=${item.id}")
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
                
                // 针对2列表格的优化处理
                optimizeForTwoColumnTable(item)
            }
        }
        
        /**
         * 针对表格列数的优化处理
         */
        private fun optimizeForTwoColumnTable(tableItem: MarkdownItem.Table) {
            // 检测表格列数
            val columnCount = detectTableColumns(tableItem)
            
            if (columnCount <= 3) {
                // 3列及以下表格的处理
                val scrollView = findHorizontalScrollView(binding.root)
                scrollView?.let { sv ->
                    // 为3列及以下表格设置合理的最小宽度，确保表格内容能正确显示
                    val screenWidth = sv.context.resources.displayMetrics.widthPixels
                    val minWidth = (screenWidth * 0.8).toInt() // 设置为屏幕宽度的80%
                    binding.textView.minWidth = minWidth
                    
                    // 延迟检查内容宽度（等待渲染完成）
                    binding.textView.post {
                        val availableWidth = sv.width - sv.paddingLeft - sv.paddingRight
                        val textViewWidth = binding.textView.width
                        
                        AppLog.d("$columnCount-column table: availableWidth=$availableWidth, textViewWidth=$textViewWidth, minWidth=$minWidth")
                        
                        if (textViewWidth <= availableWidth && availableWidth > 0) {
                            // 内容适合屏幕，禁用水平滚动条
                            sv.isHorizontalScrollBarEnabled = false
                            AppLog.d("$columnCount-column table fits screen, disabled horizontal scrollbar")
                        } else {
                            // 内容需要滚动，启用滚动条
                            sv.isHorizontalScrollBarEnabled = true
                            AppLog.d("$columnCount-column table needs scrolling, enabled horizontal scrollbar")
                        }
                    }
                }
            } else if (columnCount > 3) {
                // 超过3列的表格设置更大的最小宽度，保持滚动条
                val scrollView = findHorizontalScrollView(binding.root)
                val screenWidth = binding.root.context.resources.displayMetrics.widthPixels
                val minWidth = (screenWidth * 1.2).toInt() // 设置为屏幕宽度的120%
                binding.textView.minWidth = minWidth
                scrollView?.isHorizontalScrollBarEnabled = true
                AppLog.d("Multi-column table ($columnCount columns), enabled horizontal scrollbar, minWidth=$minWidth")
            } else {
                // 其他情况（检测失败）设置默认最小宽度
                val screenWidth = binding.root.context.resources.displayMetrics.widthPixels
                val minWidth = (screenWidth * 0.8).toInt() // 设置为屏幕宽度的80%
                binding.textView.minWidth = minWidth
                AppLog.d("Unknown table columns, set default minWidth=$minWidth")
            }
        }
        
        /**
         * 检测表格列数（改进实现）
         */
        private fun detectTableColumns(tableItem: MarkdownItem.Table): Int {
            return try {
                // 通过TableBlock节点检测列数
                val tableBlock = tableItem.node as? TableBlock
                
                if (tableBlock != null) {
                    // 获取表格头部来确定列数
                    val header = tableBlock.firstChild as? TableHead
                    header?.let { h ->
                        val headerRow = h.firstChild as? TableRow
                        var columnCount = 0
                        var cell = headerRow?.firstChild
                        while (cell != null) {
                            if (cell is TableCell) {
                                columnCount++
                            }
                            cell = cell.next
                        }
                        columnCount
                    } ?: 0
                } else {
                    // 备用方法：分析渲染后的内容
                    0 // 无法确定时返回0，不进行特殊处理
                }
            } catch (e: Exception) {
                AppLog.d("Error detecting table columns: ${e.message}")
                0 // 出错时返回0，不进行特殊处理
            }
        }
        
        /**
         * 设置表格的水平滚动效果，与代码块保持一致
         */
        private fun setupTableScrolling() {
            // 找到HorizontalScrollView - 从根视图开始查找
            val scrollView = findHorizontalScrollView(binding.root) ?: return
            
            // 设置触摸事件监听器，与CodeDisplayView保持一致
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
         * 递归查找HorizontalScrollView
         */
        private fun findHorizontalScrollView(view: View): HorizontalScrollView? {
            if (view is HorizontalScrollView) {
                return view
            }
            if (view is android.view.ViewGroup) {
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
    }

    // 引用块ViewHolder
    class BlockQuoteViewHolder(private val binding: ItemMarkdownParagraphBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.BlockQuote) {
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)

                // 添加引用样式
                binding.textView.setPadding(32, 16, 0, 16)
                binding.textView.setBackgroundColor(0x0D000000) // 轻微背景色
            }
        }
    }

    // 分隔线ViewHolder
    class ThematicBreakViewHolder(private val binding: ItemMarkdownParagraphBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.ThematicBreak) {
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
            }
        }
    }

    // HTML块ViewHolder
    class HtmlBlockViewHolder(private val binding: ItemMarkdownParagraphBinding) : BaseViewHolder(binding.root) {
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.HtmlBlock) {
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
            }
        }
    }
}

/**
 * DiffUtil回调 - 用于高效的列表更新
 */
class MarkdownDiffCallback : DiffUtil.ItemCallback<MarkdownItem>() {
    override fun areItemsTheSame(oldItem: MarkdownItem, newItem: MarkdownItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MarkdownItem, newItem: MarkdownItem): Boolean {
        return oldItem == newItem
    }
} 