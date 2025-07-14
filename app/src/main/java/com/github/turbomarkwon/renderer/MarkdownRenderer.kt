package com.github.turbomarkwon.renderer

import android.widget.TextView
import com.github.turbomarkwon.cache.LightweightMarkdownCache
import com.github.turbomarkwon.cache.CachePerformanceAnalyzer
import io.noties.markwon.Markwon
import org.commonmark.node.Node
import org.commonmark.ext.gfm.tables.*

/**
 * Markdown渲染器 - 负责将Node渲染到TextView
 * 使用LightweightMarkdownCache进行轻量级缓存
 */
object MarkdownRenderer {
    
    /**
     * 渲染单个Node到TextView
     * 使用轻量级缓存提升性能
     */
    fun renderNode(node: Node, textView: TextView, markwon: Markwon) {
        try {
            // 生成缓存键（基于节点内容和类型）
            val nodeContent = extractNodeContent(node)
            val nodeType = node.javaClass.simpleName
            val cacheKey = LightweightMarkdownCache.generateCacheKey(nodeContent, nodeType)
            
            // 检查轻量级缓存
            val cachedSpanned = LightweightMarkdownCache.getSpanned(cacheKey)
            
            if (cachedSpanned != null) {
                // 使用缓存的Spanned对象
                markwon.setParsedMarkdown(textView, cachedSpanned)
            } else {
                // 渲染Node并缓存结果
                val rendered = CachePerformanceAnalyzer.measureRenderTime {
                    markwon.render(createDocumentFromNode(node))
                }
                
                // 缓存渲染结果
                LightweightMarkdownCache.putSpanned(cacheKey, rendered, nodeType)
                markwon.setParsedMarkdown(textView, rendered)
            }
        } catch (e: Exception) {
            // 出错时显示错误信息
            textView.text = "渲染错误: ${e.message}"
        }
    }
    
    /**
     * 从单个Node创建Document用于渲染
     * 注意：创建节点的副本以避免破坏原始AST结构
     */
    private fun createDocumentFromNode(node: Node): org.commonmark.node.Document {
        val document = org.commonmark.node.Document()
        // 创建节点的副本而不是移动原始节点，避免破坏AST结构
        val nodeCopy = cloneNode(node)
        document.appendChild(nodeCopy)
        return document
    }
    
    /**
     * 深度复制节点以避免修改原始AST
     */
    private fun cloneNode(original: Node): Node {
        val clone = when (original) {
            // 行内元素
            is org.commonmark.node.Text -> {
                val textNode = org.commonmark.node.Text()
                textNode.literal = original.literal
                textNode
            }
            is org.commonmark.node.Code -> {
                val codeNode = org.commonmark.node.Code()
                codeNode.literal = original.literal
                codeNode
            }
            is org.commonmark.node.Emphasis -> {
                org.commonmark.node.Emphasis()
            }
            is org.commonmark.node.StrongEmphasis -> {
                org.commonmark.node.StrongEmphasis()
            }
            is org.commonmark.node.Link -> {
                val linkNode = org.commonmark.node.Link()
                linkNode.destination = original.destination
                linkNode.title = original.title
                linkNode
            }
            is org.commonmark.node.Image -> {
                val imageNode = org.commonmark.node.Image()
                imageNode.destination = original.destination
                imageNode.title = original.title
                imageNode
            }
            is org.commonmark.node.HardLineBreak -> {
                org.commonmark.node.HardLineBreak()
            }
            is org.commonmark.node.SoftLineBreak -> {
                org.commonmark.node.SoftLineBreak()
            }
            is org.commonmark.node.HtmlInline -> {
                val htmlNode = org.commonmark.node.HtmlInline()
                htmlNode.literal = original.literal
                htmlNode
            }
            
            // 块级元素
            is org.commonmark.node.Paragraph -> {
                org.commonmark.node.Paragraph()
            }
            is org.commonmark.node.Heading -> {
                val headingNode = org.commonmark.node.Heading()
                headingNode.level = original.level
                headingNode
            }
            is org.commonmark.node.FencedCodeBlock -> {
                val codeBlock = org.commonmark.node.FencedCodeBlock()
                codeBlock.literal = original.literal
                codeBlock.info = original.info
                codeBlock
            }
            is org.commonmark.node.IndentedCodeBlock -> {
                val codeBlock = org.commonmark.node.IndentedCodeBlock()
                codeBlock.literal = original.literal
                codeBlock
            }
            is org.commonmark.node.BlockQuote -> {
                org.commonmark.node.BlockQuote()
            }
            is org.commonmark.node.BulletList -> {
                val listNode = org.commonmark.node.BulletList()
                listNode.bulletMarker = original.bulletMarker
                listNode.isTight = original.isTight
                listNode
            }
            is org.commonmark.node.OrderedList -> {
                val listNode = org.commonmark.node.OrderedList()
                listNode.startNumber = original.startNumber
                listNode.delimiter = original.delimiter
                listNode.isTight = original.isTight
                listNode
            }
            is org.commonmark.node.ListItem -> {
                org.commonmark.node.ListItem()
            }
            is org.commonmark.node.ThematicBreak -> {
                org.commonmark.node.ThematicBreak()
            }
            is org.commonmark.node.HtmlBlock -> {
                val htmlBlock = org.commonmark.node.HtmlBlock()
                htmlBlock.literal = original.literal
                htmlBlock
            }
            
            // 处理 Table 相关节点（来自 GFM 扩展）
            is TableBlock -> {
                TableBlock()
            }
            is TableHead -> {
                TableHead()
            }
            is TableBody -> {
                TableBody()
            }
            is TableRow -> {
                TableRow()
            }
            is TableCell -> {
                val cellNode = TableCell()
                // 只复制可访问的属性
                cellNode.alignment = original.alignment
                // header 字段是私有的，无法直接访问
                cellNode
            }
            
            // 处理容器节点
            is com.github.turbomarkwon.customcontainer.ContainerNode -> {
                val containerNode = com.github.turbomarkwon.customcontainer.ContainerNode(original.containerType)
                containerNode.title = original.title
                containerNode
            }
            
            // 对于其他未知类型，根据其是否为块级元素来处理
            else -> {
                when {
                    // 如果是块级元素但我们不知道具体类型，创建一个段落节点
                    original is org.commonmark.node.Block -> {
                        org.commonmark.node.Paragraph()
                    }
                    // 如果是行内元素但我们不知道具体类型，创建文本节点
                    else -> {
                        val textNode = org.commonmark.node.Text()
                        textNode.literal = extractNodeContent(original)
                        textNode
                    }
                }
            }
        }
        
        // 递归复制子节点
        var child = original.firstChild
        while (child != null) {
            val childCopy = cloneNode(child)
            clone.appendChild(childCopy)
            child = child.next
        }
        
        return clone
    }
    
    /**
     * 提取节点内容用于生成缓存键
     */
    private fun extractNodeContent(node: Node): String {
        val sb = StringBuilder()
        
        // 递归提取所有文本内容
        fun extractText(currentNode: Node) {
            when (currentNode) {
                is org.commonmark.node.Text -> {
                    sb.append(currentNode.literal)
                }
                is org.commonmark.node.Code -> {
                    sb.append(currentNode.literal)
                }
                is org.commonmark.node.FencedCodeBlock -> {
                    sb.append(currentNode.info ?: "").append(currentNode.literal)
                }
                is org.commonmark.node.IndentedCodeBlock -> {
                    sb.append(currentNode.literal)
                }
                is org.commonmark.node.Heading -> {
                    sb.append("h").append(currentNode.level).append(":")
                }
                is org.commonmark.node.HtmlInline -> {
                    sb.append(currentNode.literal)
                }
                is org.commonmark.node.HtmlBlock -> {
                    sb.append(currentNode.literal)
                }
            }
            
            // 递归处理子节点
            var child = currentNode.firstChild
            while (child != null) {
                extractText(child)
                child = child.next
            }
        }
        
        extractText(node)
        
        // 如果没有提取到内容，使用节点的类型和hashCode
        return if (sb.isNotEmpty()) {
            sb.toString()
        } else {
            "${node.javaClass.simpleName}_${node.hashCode()}"
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        LightweightMarkdownCache.clearAll()
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Int {
        return LightweightMarkdownCache.getCacheSize()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): LightweightMarkdownCache.CacheStats {
        return LightweightMarkdownCache.getCacheStats()
    }
} 