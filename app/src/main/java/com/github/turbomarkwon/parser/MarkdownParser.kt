package com.github.turbomarkwon.parser

import com.github.turbomarkwon.data.MarkdownItem
import com.github.turbomarkwon.data.MarkdownParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TablesExtension
import java.util.*
import kotlin.system.measureTimeMillis
import com.github.turbomarkwon.util.AppLog

class MarkdownParser {
    
    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()
    
    suspend fun parseMarkdownAsync(markdownText: String): MarkdownParseResult = withContext(Dispatchers.Default) {
        var items: List<MarkdownItem>
        val parseTimeMs = measureTimeMillis {
            val document = parser.parse(markdownText)
            AppLog.d("firstChild class = ${document.firstChild?.javaClass?.simpleName}")
            items = parseDocument(document)
            // 调试：统计表格节点数量
            val tableCount = items.count { it is MarkdownItem.Table }
            AppLog.d("Parsed table items = $tableCount")
        }
        
        return@withContext MarkdownParseResult(
            items = items,
            parseTimeMs = parseTimeMs,
            itemCount = items.size
        )
    }
    
    private fun parseDocument(document: Node): List<MarkdownItem> {
        val items = mutableListOf<MarkdownItem>()
        var child = document.firstChild
        
        while (child != null) {
            when (child) {
                is Paragraph -> {
                    items.add(MarkdownItem.Paragraph(
                        id = generateId(),
                        node = child
                    ))
                }
                is Heading -> {
                    items.add(MarkdownItem.Heading(
                        id = generateId(),
                        node = child,
                        level = child.level
                    ))
                }
                is FencedCodeBlock -> {
                    items.add(MarkdownItem.CodeBlock(
                        id = generateId(),
                        node = child,
                        language = child.info
                    ))
                }
                is IndentedCodeBlock -> {
                    items.add(MarkdownItem.CodeBlock(
                        id = generateId(),
                        node = child,
                        language = null
                    ))
                }
                is BulletList -> {
                    items.addAll(parseList(child, false))
                }
                is OrderedList -> {
                    items.addAll(parseList(child, true))
                }
                is BlockQuote -> {
                    // 引用块作为整体处理，不提取内部代码块
                    items.add(MarkdownItem.BlockQuote(
                        id = generateId(),
                        node = child
                    ))
                }
                is ThematicBreak -> {
                    items.add(MarkdownItem.ThematicBreak(
                        id = generateId(),
                        node = child
                    ))
                }
                is HtmlBlock -> {
                    items.add(MarkdownItem.HtmlBlock(
                        id = generateId(),
                        node = child
                    ))
                }
                is TableBlock -> {
                    items.add(MarkdownItem.Table(
                        id = generateId(),
                        node = child
                    ))
                }
                else -> {
                    items.add(MarkdownItem.Paragraph(
                        id = generateId(),
                        node = child
                    ))
                }
            }
            child = child.next
        }
        
        return items
    }
    
    private fun parseList(listNode: ListBlock, isOrdered: Boolean, level: Int = 0): List<MarkdownItem> {
        val items = mutableListOf<MarkdownItem>()
        var child = listNode.firstChild
        
        while (child != null) {
            if (child is ListItem) {
                items.add(MarkdownItem.ListItem(
                    id = generateId(),
                    node = child,
                    isOrdered = isOrdered,
                    level = level
                ))
                
                var nestedChild = child.firstChild
                while (nestedChild != null) {
                    when (nestedChild) {
                        is BulletList -> {
                            items.addAll(parseList(nestedChild, false, level + 1))
                        }
                        is OrderedList -> {
                            items.addAll(parseList(nestedChild, true, level + 1))
                        }
                    }
                    nestedChild = nestedChild.next
                }
            }
            child = child.next
        }
        
        return items
    }

    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }
} 