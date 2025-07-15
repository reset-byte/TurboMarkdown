package com.github.turbomarkwon.util

import org.commonmark.node.*

/**
 * 数学公式处理工具类
 */
object MathUtils {
    
    /**
     * 检测节点是否包含块级数学公式
     * 块级数学公式特征：
     * 1. 独立的段落节点
     * 2. 内容以 $$ 开头和结尾
     * 3. 通常占据整行
     */
    fun isBlockMathFormula(node: Node): Boolean {
        if (node !is Paragraph) return false
        
        val content = extractTextContent(node).trim()
        
        // 检查是否为块级数学公式
        return (content.startsWith("$$") && content.endsWith("$$") && content.length > 4) ||
               (content.startsWith("\\[") && content.endsWith("\\]")) ||
               (content.startsWith("\\begin{") && content.contains("\\end{"))
    }
    
    /**
     * 检测段落是否包含内联数学公式
     */
    fun containsInlineMathFormula(node: Node): Boolean {
        if (node !is Paragraph) return false
        
        val content = extractTextContent(node)
        
        // 检查内联公式模式：$...$（不是 $$）
        val inlinePattern = Regex("""\$[^$\n]+\$""")
        val blockPattern = Regex("""\$\$.*?\$\$""", RegexOption.DOT_MATCHES_ALL)
        
        // 有内联公式但不是块级公式
        return inlinePattern.containsMatchIn(content) && !blockPattern.containsMatchIn(content)
    }
    
    /**
     * 提取节点的文本内容
     */
    fun extractTextContent(node: Node): String {
        val content = StringBuilder()
        
        fun collectText(n: Node) {
            when (n) {
                is Text -> content.append(n.literal)
                is Code -> content.append(n.literal)
                else -> {
                    var child = n.firstChild
                    while (child != null) {
                        collectText(child)
                        child = child.next
                    }
                }
            }
        }
        
        collectText(node)
        return content.toString()
    }
    
    /**
     * 检测数学公式的类型
     */
    fun getMathFormulaType(content: String): MathFormulaType {
        val trimmed = content.trim()
        
        return when {
            trimmed.startsWith("$$") && trimmed.endsWith("$$") -> MathFormulaType.BLOCK_DOLLAR
            trimmed.startsWith("\\[") && trimmed.endsWith("\\]") -> MathFormulaType.BLOCK_BRACKET
            trimmed.startsWith("\\begin{") && trimmed.contains("\\end{") -> MathFormulaType.BLOCK_ENVIRONMENT
            trimmed.contains(Regex("""\$[^$\n]+\$""")) -> MathFormulaType.INLINE
            else -> MathFormulaType.NONE
        }
    }
    
    /**
     * 数学公式类型枚举
     */
    enum class MathFormulaType {
        NONE,              // 不是数学公式
        INLINE,            // 内联公式 $...$
        BLOCK_DOLLAR,      // 块级公式 $$...$$
        BLOCK_BRACKET,     // 块级公式 \[...\]
        BLOCK_ENVIRONMENT  // 块级环境 \begin{}...\end{}
    }
} 