package com.github.turbomarkwon.customcontainer

import org.commonmark.node.CustomBlock
import org.commonmark.node.Visitor

/**
 * 自定义容器节点
 * 用于表示 :::type 语法的容器块
 */
class ContainerNode(
    val containerType: String
) : CustomBlock() {
    
    /**
     * 容器标题（可选）
     */
    var title: String? = null
    
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
    
    companion object {
        /**
         * 支持的容器类型及其配置
         */
        val CONTAINER_TYPES = mapOf(
            "note" to ContainerConfig(
                icon = "📘",
                colorRes = "#2196F3",
                title = "提示"
            ),
            "tip" to ContainerConfig(
                icon = "💡", 
                colorRes = "#4CAF50",
                title = "建议"
            ),
            "warning" to ContainerConfig(
                icon = "⚠️",
                colorRes = "#FF9800", 
                title = "警告"
            ),
            "danger" to ContainerConfig(
                icon = "❗",
                colorRes = "#F44336",
                title = "危险"
            ),
            "error" to ContainerConfig(
                icon = "❗",
                colorRes = "#F44336",
                title = "错误" 
            ),
            "info" to ContainerConfig(
                icon = "🛠",
                colorRes = "#2196F3",
                title = "信息"
            ),
            "success" to ContainerConfig(
                icon = "✅",
                colorRes = "#4CAF50",
                title = "成功"
            ),
            "question" to ContainerConfig(
                icon = "❓",
                colorRes = "#9C27B0",
                title = "提问"
            ),
            "important" to ContainerConfig(
                icon = "📌",
                colorRes = "#E91E63",
                title = "重要"
            ),
            "example" to ContainerConfig(
                icon = "🧪",
                colorRes = "#607D8B",
                title = "示例"
            )
        )
        
        /**
         * 检查是否为支持的容器类型
         */
        fun isSupportedType(type: String): Boolean {
            return CONTAINER_TYPES.containsKey(type.lowercase())
        }
        
        /**
         * 获取容器配置
         */
        fun getConfig(type: String): ContainerConfig? {
            return CONTAINER_TYPES[type.lowercase()]
        }
    }
    
    /**
     * 容器配置数据类
     */
    data class ContainerConfig(
        val icon: String,
        val colorRes: String,
        val title: String
    )
} 