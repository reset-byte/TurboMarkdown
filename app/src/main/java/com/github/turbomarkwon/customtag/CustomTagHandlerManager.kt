package com.github.turbomarkwon.customtag

import io.noties.markwon.html.TagHandler
import com.github.turbomarkwon.util.AppLog

/**
 * 自定义标签处理器管理器
 */
object CustomTagHandlerManager {
    
    private val tagHandlers = mutableMapOf<String, TagHandler>()
    
    init {
        // 注册默认的标签处理器
        registerDefaultHandlers()
    }
    
    /**
     * 注册默认的标签处理器
     */
    private fun registerDefaultHandlers() {
        val styleHandler = CustomStyleTagHandler()
        val decorationHandler = CustomTextDecorationTagHandler()
        
        // 注册样式标签
        styleHandler.supportedTags().forEach { tagName ->
            tagHandlers[tagName] = styleHandler
            AppLog.d("CustomTagHandlerManager: 注册样式标签处理器 '$tagName'")
        }
        
        // 注册装饰标签
        decorationHandler.supportedTags().forEach { tagName ->
            tagHandlers[tagName] = decorationHandler
            AppLog.d("CustomTagHandlerManager: 注册装饰标签处理器 '$tagName'")
        }
    }
    
    /**
     * 注册自定义标签处理器
     */
    fun registerHandler(tagName: String, handler: TagHandler) {
        tagHandlers[tagName] = handler
        AppLog.d("CustomTagHandlerManager: 注册自定义标签处理器 '$tagName'")
    }
    
    /**
     * 注册支持多个标签的处理器
     */
    fun registerHandler(handler: TagHandler) {
        handler.supportedTags().forEach { tagName ->
            tagHandlers[tagName] = handler
            AppLog.d("CustomTagHandlerManager: 注册标签处理器 '$tagName'")
        }
    }
    
    /**
     * 获取所有支持的标签
     */
    fun getSupportedTags(): Set<String> = tagHandlers.keys
    
    /**
     * 获取标签处理器
     */
    fun getHandler(tagName: String): TagHandler? = tagHandlers[tagName]
    
    /**
     * 创建标签处理器映射（用于HtmlPlugin配置）
     */
    fun createTagHandlerMap(): Map<String, TagHandler> {
        return tagHandlers.toMap()
    }
    
    /**
     * 获取所有已注册的处理器（去重）
     */
    fun getAllHandlers(): Set<TagHandler> {
        return tagHandlers.values.toSet()
    }
} 