package com.github.turbomarkwon.renderer

import android.widget.TextView
import android.text.Spanned
import io.noties.markwon.Markwon
import org.commonmark.node.Node
import java.util.concurrent.ConcurrentHashMap

/**
 * Markdown渲染器 - 负责将Node渲染到TextView
 */
object MarkdownRenderer {
    
    // 渲染结果缓存 - 可选的优化
    private val renderCache = ConcurrentHashMap<String, Spanned>()
    
    /**
     * 渲染单个Node到TextView
     */
    fun renderNode(node: Node, textView: TextView, markwon: Markwon) {
        try {
            // 检查缓存
            val nodeHash = node.hashCode().toString()
            val cached = renderCache[nodeHash]
            
            if (cached != null) {
                markwon.setParsedMarkdown(textView, cached)
            } else {
                // 渲染Node并缓存结果
                val rendered = markwon.render(createDocumentFromNode(node))
                renderCache[nodeHash] = rendered
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
     * 清理缓存
     */
    fun clearCache() {
        renderCache.clear()
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Int {
        return renderCache.size
    }
} 