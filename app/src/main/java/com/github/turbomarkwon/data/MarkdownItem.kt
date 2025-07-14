package com.github.turbomarkwon.data

import org.commonmark.node.Node

/**
 * 表示Markdown文档中的一个渲染项
 */
sealed class MarkdownItem {
    abstract val id: String
    abstract val node: Node
    
    data class Paragraph(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class Heading(
        override val id: String,
        override val node: Node,
        val level: Int
    ) : MarkdownItem()
    
    data class CodeBlock(
        override val id: String,
        override val node: Node,
        val language: String?
    ) : MarkdownItem()
    
    data class BulletList(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class OrderedList(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class ListItem(
        override val id: String,
        override val node: Node,
        val isOrdered: Boolean,
        val level: Int
    ) : MarkdownItem()
    
    data class Table(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class BlockQuote(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class ThematicBreak(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class HtmlBlock(
        override val id: String,
        override val node: Node
    ) : MarkdownItem()
    
    data class Container(
        override val id: String,
        override val node: Node,
        val containerType: String,
        val title: String?
    ) : MarkdownItem()
}

/**
 * Markdown解析结果
 */
data class MarkdownParseResult(
    val items: List<MarkdownItem>,
    val parseTimeMs: Long,
    val itemCount: Int
)

/**
 * 渲染状态
 */
sealed class MarkdownRenderState {
    object Loading : MarkdownRenderState()
    data class Success(val result: MarkdownParseResult) : MarkdownRenderState()
    data class Error(val exception: Throwable) : MarkdownRenderState()
} 