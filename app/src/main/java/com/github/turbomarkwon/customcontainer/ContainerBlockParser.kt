package com.github.turbomarkwon.customcontainer

import org.commonmark.node.Block
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import java.util.regex.Pattern
import com.github.turbomarkwon.util.AppLog

/**
 * 容器块解析器工厂
 * 用于创建容器块解析器实例
 */
class ContainerBlockParserFactory : AbstractBlockParserFactory() {
    
    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        val line = state.line.toString()
        AppLog.d("ContainerBlockParser: tryStart被调用，行内容: '$line'")
        val matcher = CONTAINER_START_PATTERN.matcher(line)
        
        if (matcher.matches()) {
            val containerType = matcher.group(1)?.trim()?.lowercase() ?: return null
            
            // 检查是否为支持的容器类型
            if (!ContainerNode.isSupportedType(containerType)) {
                AppLog.d("ContainerBlockParser: 不支持的容器类型: $containerType")
                return null
            }
            
            // 提取可选标题
            val title = matcher.group(2)?.trim()
            
            AppLog.d("ContainerBlockParser: 检测到容器开始 - 类型: $containerType, 标题: $title")
            
            return BlockStart.of(ContainerBlockParser(containerType, title))
                .atIndex(state.line.length)
        }
        
        AppLog.d("ContainerBlockParser: 行 '$line' 不匹配容器模式")
        return BlockStart.none()
    }
    
    companion object {
        // 匹配 :::type 或 :::type title 语法
        private val CONTAINER_START_PATTERN = Pattern.compile("^:::([a-zA-Z]+)\\s*(.*?)\\s*$")
    }
}

/**
 * 容器块解析器
 * 负责解析容器内容并创建容器节点
 */
class ContainerBlockParser(
    private val containerType: String,
    private val title: String?
) : AbstractBlockParser() {
    
    private val containerNode = ContainerNode(containerType).apply {
        this.title = title?.takeIf { it.isNotBlank() }
    }
    
    private var finished = false
    
    override fun getBlock(): Block = containerNode
    
    override fun tryContinue(parserState: ParserState): BlockContinue? {
        // 如果已经完成，不再继续
        if (finished) {
            return BlockContinue.none()
        }
        
        val line = parserState.line.toString()
        
        // 检查是否为容器结束标记
        if (CONTAINER_END_PATTERN.matcher(line).matches()) {
            AppLog.d("ContainerBlockParser: 检测到容器结束")
            finished = true
            // 消费整行并结束块解析
            return BlockContinue.atIndex(parserState.line.length)
        }
        
        // 继续解析容器内容
        return BlockContinue.atIndex(parserState.index)
    }
    
    override fun isContainer(): Boolean = true
    
    override fun canContain(childBlock: Block): Boolean {
        // 容器可以包含除了自身以外的任何块级元素
        return childBlock !is ContainerNode
    }
    
    override fun closeBlock() {
        // 标记为已完成
        finished = true
        AppLog.d("ContainerBlockParser: 容器块解析完成 - 类型: $containerType")
    }
    
    companion object {
        // 匹配容器结束标记 :::
        private val CONTAINER_END_PATTERN = Pattern.compile("^:::$")
    }
} 