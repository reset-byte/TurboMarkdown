package com.github.turbomarkwon.customtag

import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.TagHandler
import com.github.turbomarkwon.util.AppLog

/**
 * 自定义标签插件工具类
 * 基于Markwon的HtmlPlugin机制实现真正的自定义标签支持
 */
object CustomTagPlugin {
    
    /**
     * 创建支持自定义标签的HtmlPlugin
     * 这是真正的Markwon插件实现
     */
    fun create(): HtmlPlugin {
        AppLog.d("CustomTagPlugin: 创建自定义标签HtmlPlugin")
        
        // 创建HtmlPlugin配置
        return HtmlPlugin.create { plugin ->
            // 注册样式标签处理器
            val styleHandler = CustomStyleTagHandler()
            plugin.addHandler(styleHandler)
            
            // 注册装饰标签处理器  
            val decorationHandler = CustomTextDecorationTagHandler()
            plugin.addHandler(decorationHandler)
            
            AppLog.d("CustomTagPlugin: 注册了自定义标签处理器")
            AppLog.d("CustomTagPlugin: 样式标签: ${styleHandler.supportedTags()}")
            AppLog.d("CustomTagPlugin: 装饰标签: ${decorationHandler.supportedTags()}")
        }
    }
    
    /**
     * 初始化自定义标签配置
     */
    fun initialize() {
        AppLog.d("CustomTagPlugin: 初始化自定义标签配置")
    }
    
    /**
     * 添加自定义标签处理器
     */
    fun addCustomTagHandler(handler: TagHandler) {
        // 可以在这里添加动态注册处理器的逻辑
        AppLog.d("CustomTagPlugin: 添加自定义标签处理器: ${handler.supportedTags()}")
    }
    
    /**
     * 获取所有支持的自定义标签
     */
    fun getSupportedTags(): Set<String> {
        val allTags = mutableSetOf<String>()
        allTags.addAll(CustomStyleTagHandler().supportedTags())
        allTags.addAll(CustomTextDecorationTagHandler().supportedTags())
        return allTags
    }
    
    /**
     * 验证自定义标签是否被支持
     */
    fun isTagSupported(tagName: String): Boolean {
        return getSupportedTags().contains(tagName)
    }
} 