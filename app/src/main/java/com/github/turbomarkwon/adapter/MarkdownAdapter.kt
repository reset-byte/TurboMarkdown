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
import com.github.turbomarkwon.databinding.ItemMarkdownContainerBinding
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
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.node.*
import com.github.turbomarkwon.cache.MermaidRenderCache
import com.github.turbomarkwon.customcontainer.ContainerNode
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

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
        private const val TYPE_CONTAINER = 8
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
            is MarkdownItem.Container -> TYPE_CONTAINER
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
            TYPE_CONTAINER -> {
                val binding = ItemMarkdownContainerBinding.inflate(layoutInflater, parent, false)
                ContainerViewHolder(binding)
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
    
    // 容器ViewHolder - 支持复杂内容渲染
    class ContainerViewHolder(private val binding: ItemMarkdownContainerBinding) : BaseViewHolder(binding.root) {
        private var currentContainerId: String? = null
        
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.Container) {
                AppLog.d("ContainerViewHolder: 渲染容器 - 类型: ${item.containerType}, ID: ${item.id}")
                
                // 总是清除之前的内容，确保状态正确
                binding.containerContent.removeAllViews()
                
                // 记录当前容器ID
                currentContainerId = item.id
                
                try {
                    // 设置容器标题和样式
                    setupContainerHeader(item)
                    
                    // 分析和渲染容器内容
                    analyzeAndRenderContent(item.node, markwon)
                    
                    AppLog.d("ContainerViewHolder: 容器渲染完成 - ID: ${item.id}, 子视图数量: ${binding.containerContent.childCount}")
                    
                } catch (e: Exception) {
                    AppLog.e("ContainerViewHolder: 容器渲染失败", e)
                    
                    // 渲染失败时显示错误信息而不是空内容
                    addErrorContentView(e.message ?: "未知错误")
                }
            }
        }
        
        override fun onRecycled() {
            super.onRecycled()
            AppLog.d("ContainerViewHolder: ViewHolder 被回收，清理状态")
            
            // 清理状态，准备重用
            currentContainerId = null
            binding.containerContent.removeAllViews()
            
            // 重置标题
            binding.containerTitle.text = ""
            binding.containerHeader.background = null
        }
        
        /**
         * 设置容器标题和样式
         */
        private fun setupContainerHeader(item: MarkdownItem.Container) {
            val config = ContainerNode.getConfig(item.containerType)
            
            if (config != null) {
                // 设置标题
                val title = item.title ?: config.title
                binding.containerTitle.text = "${config.icon} $title"
                
                // 设置主题颜色和背景
                try {
                    val color = Color.parseColor(config.colorRes)
                    setContainerHeaderBackground(color)
                    AppLog.d("ContainerViewHolder: 设置容器标题 - 类型: ${item.containerType}, 标题: $title, 颜色: ${config.colorRes}")
                } catch (e: Exception) {
                    AppLog.e("ContainerViewHolder: 颜色解析失败", e)
                    setContainerHeaderBackground(Color.parseColor("#2196F3"))
                }
            } else {
                // 默认样式
                binding.containerTitle.text = "📄 ${item.title ?: "容器"}"
                setContainerHeaderBackground(Color.parseColor("#2196F3"))
                AppLog.d("ContainerViewHolder: 使用默认容器样式")
            }
        }
        
        /**
         * 设置容器标题背景，包含圆角效果
         */
        private fun setContainerHeaderBackground(color: Int) {
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
            binding.containerHeader.background = drawable
            
            // 确保文本颜色为白色以提供良好对比度
            binding.containerTitle.setTextColor(Color.WHITE)
        }
        
        /**
         * 分析容器内容并创建对应的视图
         */
        private fun analyzeAndRenderContent(containerNode: Node, markwon: Markwon) {
            val childNodeCount = getChildNodeCount(containerNode)
            AppLog.d("ContainerViewHolder: 开始分析容器内容，子节点数量: $childNodeCount")
            
            // 如果容器真的没有子节点，直接显示空内容提示
            if (childNodeCount == 0) {
                AppLog.d("ContainerViewHolder: 容器确实没有子节点，显示空内容提示")
                addEmptyContentView()
                return
            }
            
            // 调试：打印完整的容器 AST 结构
            AppLog.d("ContainerViewHolder: ===== 容器 AST 结构 =====")
            debugPrintContainerStructure(containerNode, "ContainerAST: ")
            AppLog.d("ContainerViewHolder: ===========================")
            
            var child = containerNode.firstChild
            var processedNodeCount = 0
            
            while (child != null) {
                AppLog.d("ContainerViewHolder: 处理子节点类型: ${child.javaClass.simpleName}")
                
                try {
                    when (child) {
                        is Paragraph -> {
                            addParagraphView(child, markwon)
                            processedNodeCount++
                        }
                        is Heading -> {
                            addHeadingView(child, markwon)
                            processedNodeCount++
                        }
                        is FencedCodeBlock, is IndentedCodeBlock -> {
                            addCodeBlockView(child, markwon)
                            processedNodeCount++
                        }
                        is TableBlock -> {
                            addTableView(child, markwon)
                            processedNodeCount++
                        }
                        is BulletList, is OrderedList -> {
                            AppLog.d("ContainerViewHolder: 找到列表节点: ${child.javaClass.simpleName}")
                            addListView(child, markwon)
                            processedNodeCount++
                        }
                        is BlockQuote -> {
                            addBlockQuoteView(child, markwon)
                            processedNodeCount++
                        }
                        is ThematicBreak -> {
                            addThematicBreakView(child, markwon)
                            processedNodeCount++
                        }
                        else -> {
                            // 其他类型作为段落处理
                            AppLog.d("ContainerViewHolder: 未知节点类型作为段落处理: ${child.javaClass.simpleName}")
                            addParagraphView(child, markwon)
                            processedNodeCount++
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("ContainerViewHolder: 渲染子节点时出错", e)
                    // 即使出错也要继续处理其他节点
                }
                
                child = child.next
            }
            
            AppLog.d("ContainerViewHolder: 容器内容分析完成，期望处理 $childNodeCount 个节点，实际处理 $processedNodeCount 个，创建了 ${binding.containerContent.childCount} 个子视图")
            
            // 只有在没有成功创建任何子视图时才显示空内容提示
            if (binding.containerContent.childCount == 0) {
                AppLog.d("ContainerViewHolder: 警告：虽然有子节点但没有成功创建任何视图，显示空内容提示")
                addEmptyContentView()
            }
        }
        
        /**
         * 计算子节点数量（用于调试）
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
         * 调试打印容器的 AST 结构
         */
        private fun debugPrintContainerStructure(containerNode: Node, prefix: String = "") {
            AppLog.d("$prefix${containerNode.javaClass.simpleName}")
            
            var child = containerNode.firstChild
            while (child != null) {
                AppLog.d("$prefix  ├─ ${child.javaClass.simpleName}")
                
                // 如果是段落，打印其内容的前50个字符
                if (child is Paragraph) {
                    val content = extractTextContent(child)
                    AppLog.d("$prefix     内容: ${content.take(50)}...")
                } else if (child is BulletList || child is OrderedList) {
                    // 如果是列表，打印列表项
                    var listItem = child.firstChild
                    var itemIndex = 1
                    while (listItem != null) {
                        AppLog.d("$prefix     项目$itemIndex: ${listItem.javaClass.simpleName}")
                        if (listItem is ListItem) {
                            val itemContent = extractTextContent(listItem)
                            AppLog.d("$prefix        内容: ${itemContent.take(30)}...")
                        }
                        listItem = listItem.next
                        itemIndex++
                    }
                }
                
                child = child.next
            }
        }
        
        /**
         * 提取节点的文本内容
         */
        private fun extractTextContent(node: Node): String {
            val content = StringBuilder()
            
            fun collectText(n: Node) {
                when (n) {
                    is Text -> content.append(n.literal)
                    is Code -> content.append(n.literal)
                    else -> {
                        var child = n.firstChild
                        while (child != null) {
                            collectText(child)
                            child = child.next
                        }
                    }
                }
            }
            
            collectText(node)
            return content.toString().trim()
        }
        
        /**
         * 添加段落视图
         */
        private fun addParagraphView(node: Node, markwon: Markwon) {
            // 检查段落内容，看是否包含列表结构
            val nodeContent = reconstructMarkdownFromNode(node)
            AppLog.d("ContainerViewHolder: 段落内容: ${nodeContent.take(100)}...")
            
            val textView = createBaseTextView()
            
            // 检查是否包含数学公式
            if (containsMathFormula(nodeContent)) {
                MarkdownUtils.renderEnhancedToTextView(textView, nodeContent)
            } else {
                MarkdownRenderer.renderNode(node, textView, markwon)
            }
            
            // 检查渲染后的结果
            AppLog.d("ContainerViewHolder: 段落渲染后: ${textView.text.toString().take(100)}...")
            
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加标题视图
         */
        private fun addHeadingView(node: Node, markwon: Markwon) {
            val textView = createBaseTextView()
            MarkdownRenderer.renderNode(node, textView, markwon)
            
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
            
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加代码块视图
         */
        private fun addCodeBlockView(node: Node, markwon: Markwon) {
            val code = when (node) {
                is FencedCodeBlock -> node.literal ?: ""
                is IndentedCodeBlock -> node.literal ?: ""
                else -> ""
            }
            
            val language = if (node is FencedCodeBlock) node.info else null
            
            // 检查是否为 Mermaid 图表
            if (language?.lowercase() == "mermaid") {
                addMermaidView(code)
            } else {
                addCodeView(code, language)
            }
        }
        
        /**
         * 添加代码视图
         */
        private fun addCodeView(code: String, language: String?) {
            val codeDisplayView = CodeDisplayView(binding.root.context)
            codeDisplayView.setCode(code, language ?: "")
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            codeDisplayView.layoutParams = layoutParams
            
            binding.containerContent.addView(codeDisplayView)
        }
        
        /**
         * 添加 Mermaid 图表视图
         */
        private fun addMermaidView(mermaidContent: String) {
            val mermaidDisplayView = MermaidDisplayView(binding.root.context)
            mermaidDisplayView.setMermaidContent(mermaidContent) { success, error ->
                if (!success) {
                    AppLog.e("ContainerViewHolder: Mermaid 图表渲染失败: $error")
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
            
            binding.containerContent.addView(mermaidDisplayView)
        }
        
        /**
         * 添加表格视图 - 简化版本，借鉴TableViewHolder的成功实现
         */
        private fun addTableView(node: Node, markwon: Markwon) {
            AppLog.d("ContainerViewHolder: addTableView 被调用，节点类型: ${node.javaClass.simpleName}")
            
            try {
                // 创建可滚动的表格容器，复用TableViewHolder的设计
                val scrollView = createOptimizedTableScrollView()
                val textView = createOptimizedTableTextView()
                
                // 直接使用现有的markwon实例渲染，避免插件冲突
                MarkdownRenderer.renderNode(node, textView, markwon)
                
                // 应用TableViewHolder的智能优化逻辑
                optimizeTableLayout(textView, node, scrollView)
                
                scrollView.addView(textView)
                binding.containerContent.addView(scrollView)
                
                AppLog.d("ContainerViewHolder: 表格视图已成功添加，使用简化渲染方案")
                
            } catch (e: Exception) {
                AppLog.e("ContainerViewHolder: 表格渲染失败", e)
                
                // 保留格式化文本作为最终备用方案
                val textView = createBaseTextView()
                val tableMarkdown = reconstructTableMarkdown(node)
                if (tableMarkdown.isNotBlank()) {
                    displayFormattedTable(textView, tableMarkdown)
                } else {
                    textView.text = "表格渲染失败: ${e.message}"
                    textView.setTextColor(Color.RED)
                }
                binding.containerContent.addView(textView)
            }
        }
        
        /**
         * 创建优化的表格滚动视图
         */
        private fun createOptimizedTableScrollView(): HorizontalScrollView {
            return HorizontalScrollView(binding.root.context).apply {
                isHorizontalScrollBarEnabled = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                    bottomMargin = 8
                }
                
                // 复用TableViewHolder的触摸事件处理逻辑
                setupTableScrollTouchHandler(this)
            }
        }
        
        /**
         * 创建优化的表格文本视图
         */
        private fun createOptimizedTableTextView(): TextView {
            return TextView(binding.root.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 14f
                setLineSpacing(4f, 1.2f)
                setPadding(12, 8, 12, 8)
                
                // 设置表格专用的移动方法（如果需要）
                try {
                    movementMethod = io.noties.markwon.ext.tables.TableAwareMovementMethod.create()
                } catch (e: Exception) {
                    AppLog.d("ContainerViewHolder: TableAwareMovementMethod不可用，使用默认方法")
                }
            }
        }
        
        /**
         * 设置表格滚动的触摸事件处理，复用TableViewHolder的逻辑
         */
        private fun setupTableScrollTouchHandler(scrollView: HorizontalScrollView) {
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
                        val deltaX = kotlin.math.abs(event.x - startX)
                        val deltaY = kotlin.math.abs(event.y - startY)
                        
                        // 判断是否为水平滑动
                        if (deltaX > deltaY && deltaX > 20) {
                            if (!isHorizontalScroll) {
                                isHorizontalScroll = true
                                scrollView.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        scrollView.parent?.requestDisallowInterceptTouchEvent(false)
                        isHorizontalScroll = false
                    }
                }
                false
            }
        }
        
        /**
         * 优化表格布局，借鉴TableViewHolder的智能优化逻辑
         */
        private fun optimizeTableLayout(textView: TextView, tableNode: Node, scrollView: HorizontalScrollView) {
            val columnCount = detectTableColumnsFromNode(tableNode)
            val context = textView.context
            val screenWidth = context.resources.displayMetrics.widthPixels
            
            when {
                columnCount <= 3 -> {
                    // 3列及以下表格的处理
                    val minWidth = (screenWidth * 0.8).toInt()
                    textView.minWidth = minWidth
                    
                    // 延迟检查是否需要滚动条
                    textView.post {
                        val availableWidth = scrollView.width - scrollView.paddingLeft - scrollView.paddingRight
                        val textViewWidth = textView.width
                        
                        scrollView.isHorizontalScrollBarEnabled = textViewWidth > availableWidth && availableWidth > 0
                        
                        AppLog.d("ContainerViewHolder: ${columnCount}列表格优化 - 需要滚动: ${scrollView.isHorizontalScrollBarEnabled}")
                    }
                }
                columnCount > 3 -> {
                    // 超过3列的表格设置更大的最小宽度
                    val minWidth = (screenWidth * 1.2).toInt()
                    textView.minWidth = minWidth
                    scrollView.isHorizontalScrollBarEnabled = true
                    AppLog.d("ContainerViewHolder: 多列表格(${columnCount}列)，启用滚动条")
                }
                else -> {
                    // 检测失败时的默认处理
                    val minWidth = (screenWidth * 0.8).toInt()
                    textView.minWidth = minWidth
                    AppLog.d("ContainerViewHolder: 表格列数检测失败，使用默认配置")
                }
            }
        }
        
        /**
         * 检测表格列数，复用TableViewHolder的检测逻辑
         */
        private fun detectTableColumnsFromNode(tableNode: Node): Int {
            return try {
                val tableBlock = tableNode as? TableBlock
                
                if (tableBlock != null) {
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
                    0
                }
            } catch (e: Exception) {
                AppLog.d("ContainerViewHolder: 表格列数检测出错: ${e.message}")
                0
            }
        }
        
        /**
         * 重构表格的Markdown内容
         */
        private fun reconstructTableMarkdown(tableNode: Node): String {
            if (tableNode !is TableBlock) {
                return ""
            }
            
            val markdown = StringBuilder()
            
            try {
                // 处理表格头部和主体
                var child = tableNode.firstChild
                var isFirstRow = true
                
                while (child != null) {
                    when (child) {
                        is TableHead -> {
                            val headerRow = child.firstChild
                            if (headerRow is TableRow) {
                                val headerMarkdown = reconstructTableRow(headerRow)
                                markdown.append(headerMarkdown).append("\n")
                                
                                // 添加分隔行
                                val cellCount = countTableCells(headerRow)
                                val separatorRow = (1..cellCount).joinToString(" | ") { "---" }
                                markdown.append("| $separatorRow |\n")
                            }
                        }
                        is TableBody -> {
                            var bodyRow = child.firstChild
                            while (bodyRow != null) {
                                if (bodyRow is TableRow) {
                                    val rowMarkdown = reconstructTableRow(bodyRow)
                                    markdown.append(rowMarkdown).append("\n")
                                }
                                bodyRow = bodyRow.next
                            }
                        }
                    }
                    child = child.next
                }
                
                return markdown.toString()
                
            } catch (e: Exception) {
                AppLog.e("ContainerViewHolder: 重构表格Markdown失败", e)
                return ""
            }
        }
        
        /**
         * 重构表格行的Markdown
         */
        private fun reconstructTableRow(rowNode: TableRow): String {
            val cells = mutableListOf<String>()
            
            var cell = rowNode.firstChild
            while (cell != null) {
                if (cell is TableCell) {
                    val cellContent = extractNodeTextContent(cell)
                    cells.add(cellContent.trim())
                }
                cell = cell.next
            }
            
            return "| ${cells.joinToString(" | ")} |"
        }
        
        /**
         * 计算表格行中的单元格数量
         */
        private fun countTableCells(rowNode: TableRow): Int {
            var count = 0
            var cell = rowNode.firstChild
            while (cell != null) {
                if (cell is TableCell) {
                    count++
                }
                cell = cell.next
            }
            return count
        }
        
        /**
         * 提取节点的文本内容
         */
        private fun extractNodeTextContent(node: Node): String {
            val content = StringBuilder()
            
            fun extractText(currentNode: Node) {
                when (currentNode) {
                    is Text -> {
                        content.append(currentNode.literal ?: "")
                    }
                    is Code -> {
                        content.append("`${currentNode.literal ?: ""}`")
                    }
                    is Emphasis -> {
                        content.append("*")
                        var child = currentNode.firstChild
                        while (child != null) {
                            extractText(child)
                            child = child.next
                        }
                        content.append("*")
                    }
                    is StrongEmphasis -> {
                        content.append("**")
                        var child = currentNode.firstChild
                        while (child != null) {
                            extractText(child)
                            child = child.next
                        }
                        content.append("**")
                    }
                    else -> {
                        // 递归处理其他节点的子节点
                        var child = currentNode.firstChild
                        while (child != null) {
                            extractText(child)
                            child = child.next
                        }
                    }
                }
            }
            
            extractText(node)
            return content.toString()
        }
        

        
        /**
         * 添加列表视图
         */
        private fun addListView(node: Node, markwon: Markwon) {
            AppLog.d("ContainerViewHolder: 开始渲染列表，类型: ${node.javaClass.simpleName}")
            
            // 检查列表的子节点
            var listItemCount = 0
            var listItem = node.firstChild
            while (listItem != null) {
                listItemCount++
                AppLog.d("ContainerViewHolder: 列表项 $listItemCount: ${listItem.javaClass.simpleName}")
                listItem = listItem.next
            }
            AppLog.d("ContainerViewHolder: 列表包含 $listItemCount 个项目")
            
            val textView = createBaseTextView()
            
            // 为列表设置特殊的样式，确保有足够的左边距
            textView.setPadding(24, 8, 0, 8)
            
            MarkdownRenderer.renderNode(node, textView, markwon)
            
            // 检查渲染后的文本内容
            AppLog.d("ContainerViewHolder: 列表渲染后的文本内容: ${textView.text.toString().take(100)}...")
            
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加引用块视图
         */
        private fun addBlockQuoteView(node: Node, markwon: Markwon) {
            val textView = createBaseTextView()
            MarkdownRenderer.renderNode(node, textView, markwon)
            
            // 添加引用样式
            textView.setPadding(32, 16, 0, 16)
            textView.setBackgroundColor(0x1A000000) // 半透明背景
            
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加分隔线视图
         */
        private fun addThematicBreakView(node: Node, markwon: Markwon) {
            val textView = createBaseTextView()
            MarkdownRenderer.renderNode(node, textView, markwon)
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加空内容提示
         */
        private fun addEmptyContentView() {
            val textView = createBaseTextView()
            textView.text = "此容器暂无内容"
            textView.alpha = 0.6f
            textView.gravity = android.view.Gravity.CENTER
            binding.containerContent.addView(textView)
        }
        
        /**
         * 添加错误内容提示
         */
        private fun addErrorContentView(errorMessage: String) {
            val textView = createBaseTextView()
            textView.text = "容器渲染出错: $errorMessage"
            textView.alpha = 0.8f
            textView.gravity = android.view.Gravity.CENTER
            textView.setTextColor(Color.RED)
            binding.containerContent.addView(textView)
        }
        
        /**
         * 创建基础文本视图
         */
        private fun createBaseTextView(): TextView {
            return TextView(binding.root.context).apply {
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
                is BulletList -> {
                    // 处理无序列表
                    content.append("\n")
                    var listItem = node.firstChild
                    while (listItem != null) {
                        if (listItem is ListItem) {
                            content.append("- ")
                            processChildren(listItem, content)
                            content.append("\n")
                        }
                        listItem = listItem.next
                    }
                }
                is OrderedList -> {
                    // 处理有序列表
                    content.append("\n")
                    var listItem = node.firstChild
                    var itemNumber = 1
                    while (listItem != null) {
                        if (listItem is ListItem) {
                            content.append("$itemNumber. ")
                            processChildren(listItem, content)
                            content.append("\n")
                            itemNumber++
                        }
                        listItem = listItem.next
                    }
                }
                is ListItem -> {
                    // 列表项通常由父列表处理，这里直接处理子节点
                    processChildren(node, content)
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
                "\\$\\$[\\s\\S]*?\\$\\$",
                "\\$[^\\$\\n]*?\\$",
                "\\\\\\([\\s\\S]*?\\\\\\)",
                "\\\\\\[[\\s\\S]*?\\\\\\]",
                "\\\\[a-zA-Z]+",
                "\\\\(frac|sqrt|sum|int|lim|infty|partial|nabla|alpha|beta|gamma|delta|epsilon|theta|lambda|mu|pi|sigma|omega|begin|end|pmatrix|bmatrix|vmatrix|matrix)"
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
         * 美化显示表格文本（备用方案）
         */
        private fun displayFormattedTable(textView: TextView, tableMarkdown: String) {
            // 美化表格显示
            textView.apply {
                // 使用等宽字体确保对齐
                typeface = android.graphics.Typeface.MONOSPACE
                
                // 稍微增大字体大小以提高可读性
                textSize = 13f
                
                // 添加内边距
                setPadding(16, 12, 16, 12)
                
                // 设置行间距
                setLineSpacing(2f, 1.15f)
                
                // 设置轻微的背景色以突出显示
                setBackgroundColor(Color.parseColor("#F8F9FA"))
                
                // 设置文本颜色
                setTextColor(Color.parseColor("#2C3E50"))
                
                // 处理表格内容：清理多余的空行并格式化
                val formattedContent = formatTableContent(tableMarkdown)
                text = formattedContent
            }
            
            AppLog.d("ContainerViewHolder: 表格已格式化显示，内容长度: ${tableMarkdown.length}")
        }
        
        /**
         * 格式化表格内容文本
         */
        private fun formatTableContent(tableMarkdown: String): String {
            return tableMarkdown
                .lines()
                .filter { it.trim().isNotEmpty() } // 移除空行
                .map { line ->
                    when {
                        line.trim().startsWith("|") && line.trim().endsWith("|") -> {
                            // 表格行，稍微美化格式
                            line.trim()
                        }
                        line.trim().contains("---") -> {
                            // 分隔符行，使用更美观的字符
                            line.replace("-", "─")
                        }
                        else -> line.trim()
                    }
                }
                .joinToString("\n")
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