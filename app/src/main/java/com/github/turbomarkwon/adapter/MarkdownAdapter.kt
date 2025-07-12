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
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import android.view.View
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import kotlin.math.abs
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell

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
        holder.bind(getItem(position), markwon)
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
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
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
        private var currentCodeHash: String? = null
        
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.CodeBlock) {
                AppLog.d("Binding CodeBlock item id=${item.id}, language=${item.language}")
                
                // 使用CodeDisplayView显示代码块
                showCodeBlock(item)
            }
        }
        
        private fun showCodeBlock(codeBlockItem: MarkdownItem.CodeBlock) {
            // 隐藏普通文本视图
            binding.textView.visibility = View.GONE
            
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
        
        override fun onRecycled() {
            super.onRecycled()
            // 清理CodeDisplayView
            if (codeDisplayView != null) {
                binding.codeContainer.removeView(codeDisplayView)
                codeDisplayView = null
                currentCodeHash = null
                AppLog.d("Recycled CodeDisplayView from CodeBlockViewHolder")
            }
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