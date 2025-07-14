package com.github.turbomarkwon.customcontainer

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import org.commonmark.parser.Parser
import com.github.turbomarkwon.util.AppLog

/**
 * 容器插件
 * 整合容器解析器和渲染器，为 Markwon 提供 :::type 容器语法支持
 */
class ContainerPlugin private constructor() : AbstractMarkwonPlugin() {
    
    override fun configureParser(builder: Parser.Builder) {
        // 注册容器块解析器工厂
        builder.customBlockParserFactory(ContainerBlockParserFactory())
        AppLog.d("ContainerPlugin: 注册容器块解析器")
    }
    
    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        // 注册容器节点渲染器
        builder.on(ContainerNode::class.java) { visitor, node ->
            renderContainerNode(visitor, node)
        }
        AppLog.d("ContainerPlugin: 注册容器节点渲染器")
    }
    
    /**
     * 渲染容器节点
     */
    private fun renderContainerNode(visitor: MarkwonVisitor, node: ContainerNode) {
        AppLog.d("ContainerPlugin: 渲染容器节点 - 类型: ${node.containerType}")
        
        val config = ContainerNode.getConfig(node.containerType)
        if (config != null) {
            val builder = visitor.builder()
            
            // 添加容器开始标记
            builder.append("\n\n")
            
            // 添加带样式的标题行
            val title = node.title ?: config.title
            val titleText = "${config.icon} $title"
            val titleStart = builder.length
            builder.append(titleText)
            val titleEnd = builder.length
            
            // 应用标题样式
            try {
                val color = Color.parseColor(config.colorRes)
                SpannableBuilder.setSpans(
                    builder,
                    arrayOf(
                        ForegroundColorSpan(color),
                        StyleSpan(Typeface.BOLD),
                        RelativeSizeSpan(1.1f),
                        BackgroundColorSpan(Color.parseColor("#F5F5F5"))
                    ),
                    titleStart, titleEnd
                )
            } catch (e: Exception) {
                AppLog.e("ContainerPlugin: 颜色解析失败", e)
                SpannableBuilder.setSpans(
                    builder,
                    arrayOf(
                        ForegroundColorSpan(Color.parseColor("#2196F3")),
                        StyleSpan(Typeface.BOLD),
                        RelativeSizeSpan(1.1f)
                    ),
                    titleStart, titleEnd
                )
            }
            
            builder.append("\n")
            
            // 添加内容缩进和样式
            val contentStart = builder.length
            
            // 渲染子节点内容
            var child = node.firstChild
            while (child != null) {
                child.accept(visitor)
                child = child.next
            }
            
            val contentEnd = builder.length
            
            // 为内容添加左边距样式
            if (contentEnd > contentStart) {
                SpannableBuilder.setSpans(
                    builder,
                    arrayOf(
                        android.text.style.LeadingMarginSpan.Standard(48, 24)
                    ),
                    contentStart, contentEnd
                )
            }
            
            // 添加容器结束标记
            builder.append("\n\n")
        }
    }
    
    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        // 如果需要特殊配置，可以在这里添加
        AppLog.d("ContainerPlugin: 配置容器插件")
    }
    
    companion object {
        /**
         * 创建容器插件实例
         */
        fun create(): MarkwonPlugin {
            AppLog.d("ContainerPlugin: 创建容器插件实例")
            return ContainerPlugin()
        }
        
        /**
         * 获取支持的容器类型
         */
        fun getSupportedContainerTypes(): Set<String> {
            return ContainerNode.CONTAINER_TYPES.keys
        }
        
        /**
         * 检查容器类型是否支持
         */
        fun isSupportedContainerType(type: String): Boolean {
            return ContainerNode.isSupportedType(type)
        }
        
        /**
         * 获取容器配置信息
         */
        fun getContainerConfig(type: String): ContainerNode.ContainerConfig? {
            return ContainerNode.getConfig(type)
        }
    }
} 