package com.github.turbomarkwon.renderer

import android.widget.TextView
import android.text.Spanned
import com.github.turbomarkwon.cache.LightweightMarkdownCache
import com.github.turbomarkwon.cache.CachePerformanceAnalyzer
import io.noties.markwon.Markwon
import org.commonmark.node.Node

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
     */
    private fun createDocumentFromNode(node: Node): org.commonmark.node.Document {
        val document = org.commonmark.node.Document()
        document.appendChild(node)
        return document
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