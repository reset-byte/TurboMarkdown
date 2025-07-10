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
        override fun bind(item: MarkdownItem, markwon: Markwon) {
            if (item is MarkdownItem.Table) {
                AppLog.d("Binding Table item id=${item.id}")
                MarkdownRenderer.renderNode(item.node, binding.textView, markwon)
            }
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