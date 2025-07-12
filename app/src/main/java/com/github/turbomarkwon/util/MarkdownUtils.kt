package com.github.turbomarkwon.util

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * Markdown工具类（增强版）
 * 支持以下功能：
 * - 基础Markdown语法（标题、粗体、斜体、链接等）
 * - 图片显示（通过Glide加载）
 * - 表格
 * - HTML解析
 * - 任务列表
 * - **增强的LaTeX数学公式支持**：
 *   - 多种格式：$...$, $$...$$, \(...\), \[...\]
 *   - 高等数学符号：积分、求和、极限、三角函数、对数等
 *   - 希腊字母：α、β、γ、δ、ε、ζ、η、θ、ι、κ、λ、μ、ν、ξ、ο、π、ρ、σ、τ、υ、φ、χ、ψ、ω
 *   - 数学字体：\mathbb{R}、\mathcal{L}、\mathfrak{F}等
 *   - 矩阵和数组：matrix、pmatrix、bmatrix、cases等
 *   - 复杂公式：分数、根号、上下标、重音符号等
 *   - 自动语法修复：修复常见的LaTeX语法错误
 *   - 智能识别：增强的公式检测算法
 * - 代码块显示（```代码``` 格式）
 * - 语法高亮
 */
object MarkdownUtils {

    @Volatile
    private var defaultMarkwon: Markwon? = null

    // 缓存不同配置的Markwon实例
    private val markwonCache = mutableMapOf<String, Markwon>()
    private val cacheLock = Any()

    @Volatile
    private var isJLatexMathInitialized = false

    /**
     * 初始化JLatexMath库
     * 必须在使用LaTeX功能之前调用
     */
    private fun initializeJLatexMath(context: Context) {
        if (!isJLatexMathInitialized) {
            synchronized(this) {
                if (!isJLatexMathInitialized) {
                    try {
                        AppLog.d("MarkdownUtils: 开始初始化JLatexMath库")

                        // 尝试创建一个简单的JLatexMathDrawable来测试库是否可用
                        val testDrawable = JLatexMathDrawable.builder("x=1")
                            .textSize(12f)
                            .build()

                        AppLog.d("MarkdownUtils: JLatexMath库初始化成功")
                        isJLatexMathInitialized = true

                    } catch (e: Exception) {
                        AppLog.e("MarkdownUtils: JLatexMath初始化失败 ${e.message}")
                        e.printStackTrace()
                        // 设置标记，避免重复尝试
                        isJLatexMathInitialized = true
                        throw e
                    }
                }
            }
        }
    }

    /**
     * 初始化默认Markwon渲染器（使用标准字体大小）
     * @param context 上下文
     */
    private fun initializeDefaultMarkwon(context: Context) {
        if (defaultMarkwon == null) {
            synchronized(this) {
                if (defaultMarkwon == null) {
                    try {
                        AppLog.d("MarkdownUtils: 开始初始化默认Markwon")

                        // 首先初始化JLatexMath
                        initializeJLatexMath(context)

                        defaultMarkwon = createMarkwonInstance(context, 48f) // 使用默认字体大小
                        AppLog.d("MarkdownUtils: 默认Markwon渲染器初始化完成")

                    } catch (e: Exception) {
                        AppLog.e("MarkdownUtils: 默认Markwon初始化失败 ${e.message}")
                        throw e
                    }
                }
            }
        }
    }

    /**
     * 创建Markwon实例
     * @param context 上下文
     * @param fontSize LaTeX字体大小
     * @return 配置好的Markwon实例
     */
    private fun createMarkwonInstance(context: Context, fontSize: Float): Markwon {
        AppLog.d("MarkdownUtils: 创建Markwon实例，字体大小: $fontSize")

        val tableTheme = TableTheme.buildWithDefaults(context)
            .tableBorderColor(Color.parseColor("#DDDDDD"))
            .tableOddRowBackgroundColor(Color.parseColor("#FFFFFF"))
            .tableEvenRowBackgroundColor(Color.parseColor("#F9F9F9"))
            .tableBorderWidth(2)
            .build()

        val inlineParserPlugin = MarkwonInlineParserPlugin.create()
        val latexPlugin = createEnhancedLatexPlugin(fontSize)
        val glidePlugin = GlideImagesPlugin.create(context)
        val tablePlugin = TablePlugin.create(tableTheme)
        val htmlPlugin = HtmlPlugin.create()
        val taskListPlugin = TaskListPlugin.create(context)
        val softBreakPlugin = SoftBreakAddsNewLinePlugin.create()
        val movementMethodPlugin = MovementMethodPlugin.create(TableAwareMovementMethod.create())
        val linkifyPlugin = LinkifyPlugin.create()
        
        val builder = Markwon.builder(context.applicationContext)
            .usePlugin(inlineParserPlugin)
            .usePlugin(latexPlugin)
            .usePlugin(softBreakPlugin)
            .usePlugin(glidePlugin)
            .usePlugin(tablePlugin)
            .usePlugin(htmlPlugin)
            .usePlugin(taskListPlugin)
            .usePlugin(movementMethodPlugin)
            .usePlugin(linkifyPlugin)

        return builder.build()
    }

    /**
     * 创建增强的LaTeX插件
     * @param fontSize LaTeX字体大小
     * @return 配置好的JLatexMathPlugin
     */
    private fun createEnhancedLatexPlugin(fontSize: Float): JLatexMathPlugin {
        try {
            AppLog.d("MarkdownUtils: 创建LaTeX插件，字体大小: $fontSize")

            val plugin = JLatexMathPlugin.create(fontSize) { builder ->
                builder.inlinesEnabled(true)
                builder.blocksEnabled(true)
                builder.blocksLegacy(false)

                builder.errorHandler { latex, error ->
                    AppLog.e("MarkdownUtils: LaTeX错误 - 公式: '$latex', 错误: ${error.message}")
                    if (latex.startsWith("$") && latex.endsWith("$") && !latex.startsWith("$$")) {
                        AppLog.e("MarkdownUtils: ❌ 行内公式渲染失败!")
                    }
                    error.printStackTrace()
                    null
                }
            }

            AppLog.d("MarkdownUtils: LaTeX插件创建成功")
            return plugin
        } catch (e: Exception) {
            AppLog.e("MarkdownUtils: LaTeX插件创建失败 ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 获取Markwon实例（智能缓存）
     * @param context 上下文
     * @param textView 目标TextView（用于获取字体大小）
     * @return Markwon实例
     */
    private fun getMarkwon(context: Context, textView: TextView): Markwon {
        val fontSize = textView.textSize.toInt()
        val cacheKey = "${fontSize}"

        // 对于标准字体大小且不使用自定义代码显示，使用默认实例
        if (fontSize in 40..56) {
            if (defaultMarkwon == null) {
                initializeDefaultMarkwon(context)
            }
            return defaultMarkwon!!
        }

        // 对于特殊字体大小或使用自定义代码显示，使用缓存
        synchronized(cacheLock) {
            return markwonCache.getOrPut(cacheKey) {
                AppLog.d("MarkdownUtils: 为字体大小 $fontSize 创建新的Markwon实例")
                initializeJLatexMath(context)
                createMarkwonInstance(context, fontSize.toFloat())
            }
        }
    }

    /**
     * 渲染Markdown文本到TextView
     * @param textView 目标TextView
     * @param markdown Markdown文本
     */
    private fun renderToTextView(textView: TextView, markdown: String) {
        try {
            AppLog.d("MarkdownUtils: 获取Markwon实例")
            val markwonInstance = getMarkwon(textView.context, textView)
            AppLog.d("MarkdownUtils: Markwon实例获取成功")

            AppLog.d("MarkdownUtils: 设置Markdown到TextView")
            markwonInstance.setMarkdown(textView, markdown)
            AppLog.d("MarkdownUtils: setMarkdown调用完成")
        } catch (e: Exception) {
            AppLog.e("MarkdownUtils: renderToTextView失败 ${e.message}")
            textView.text = "Markwon渲染失败: ${e.message}\n原始内容: $markdown"
            throw e
        }
    }

    /**
     * 预处理Markdown文本（移除不必要的空格和换行）
     * @param markdown 原始Markdown文本
     * @return 预处理后的Markdown文本
     */
    private fun preprocessMarkdown(markdown: String): String {
        return markdown
            .replace("\r\n", "\n") // 统一换行符
            .replace("\r", "\n")
            .trim() // 移除首尾空白
    }

    /**
     * 预处理LaTeX公式，修复常见语法问题并转换标准LaTeX语法
     * 支持的语法转换：
     * - \(公式\) → $$公式$$ (行内公式)
     * - \[公式\] → $$公式$$ (块级公式)
     * - $公式$ → $$公式$$ (单美元符号行内公式)
     * @param text 包含LaTeX公式的文本
     * @return 修复后的文本
     */
    private fun preprocessLatexFormulas(text: String): String {
        var processedText = text

        AppLog.d("MarkdownUtils: 开始LaTeX语法预处理")
        AppLog.d("MarkdownUtils: 原始文本: $processedText")

        // 1. 转换标准LaTeX语法为Markwon支持的格式

        // 转换块级公式 \[公式\] → $$公式$$
        processedText = processedText.replace(Regex("\\\\\\[([\\s\\S]*?)\\\\\\]")) { match ->
            val formula = match.groupValues[1].trim()
            AppLog.d("MarkdownUtils: 转换块级公式 \\[\\] → \$\$\$\$: $formula")
            "\n$$${formula}$$\n"
        }

        // 转换行内公式 \(公式\) → $$公式$$
        processedText = processedText.replace(Regex("\\\\\\(([\\s\\S]*?)\\\\\\)")) { match ->
            val formula = match.groupValues[1].trim()
            AppLog.d("MarkdownUtils: 转换行内公式 \\(\\) → \$\$\$\$: $formula")
            "$$${formula}$$"
        }

        // 转换单美元符号行内公式 $公式$ → $$公式$$
        // 使用更严格的匹配规则，避免误匹配普通的美元符号
        // 合并处理所有单美元符号公式，避免重复匹配
        // 重要：避免匹配已经转换的$$公式$$格式
        processedText = processedText.replace(Regex("(?<!\\$)\\$([^$\\n]+?)\\$(?!\\$)")) { match ->
            val formula = match.groupValues[1].trim()
            // 检查是否真的是LaTeX公式（包含LaTeX命令或特殊符号，或者是单个字母/命令）
            if (isLikelyLatexFormula(formula) || formula.matches(Regex("^[a-zA-Z]$")) || formula.matches(
                    Regex("^\\\\[a-zA-Z]+$")
                )
            ) {
                AppLog.d("MarkdownUtils: 转换单美元公式 \$ → \$\$\$\$: $formula")
                "$$${formula}$$"
            } else {
                // 保持原样，可能是普通的美元符号
                AppLog.d("MarkdownUtils: 保持美元符号不变（非LaTeX）: $formula")
                match.value
            }
        }

        AppLog.d("MarkdownUtils: 语法转换后: $processedText")

        // 2. 修复常见的LaTeX语法问题
        processedText = processedText
            // 修复缺失的大括号
            .replace(Regex("\\\\frac\\s*([^{\\s]+)\\s*([^{\\s]+)")) { match ->
                "\\frac{${match.groupValues[1]}}{${match.groupValues[2]}}"
            }
            // 修复上下标缺失大括号
            .replace(Regex("([a-zA-Z])_([a-zA-Z0-9]+)(?![a-zA-Z0-9])")) { match ->
                "${match.groupValues[1]}_{${match.groupValues[2]}}"
            }
            .replace(Regex("([a-zA-Z])\\^([a-zA-Z0-9]+)(?![a-zA-Z0-9])")) { match ->
                "${match.groupValues[1]}^{${match.groupValues[2]}}"
            }
            // 修复sqrt缺失大括号
            .replace(Regex("\\\\sqrt\\s*([^{\\s]+)")) { match ->
                "\\sqrt{${match.groupValues[1]}}"
            }

        AppLog.d("MarkdownUtils: 语法修复后: $processedText")

        return processedText
    }

    /**
     * 判断字符串是否像LaTeX公式
     * @param text 待判断的文本
     * @return 如果像LaTeX公式返回true
     */
    private fun isLikelyLatexFormula(text: String): Boolean {
        // 检查是否包含LaTeX特征
        val latexIndicators = listOf(
            // LaTeX命令
            "\\\\[a-zA-Z]+",
            // 希腊字母
            "\\\\(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)",
            // 数学符号
            "\\\\(frac|sqrt|sum|int|lim|infty|partial|nabla|pm|mp|times|div|leq|geq|neq|approx|equiv)",
            // 上下标
            "[_^]\\{[^}]+\\}",
            // 简单上下标
            "[a-zA-Z][_^][a-zA-Z0-9]+",
            // 数学字体
            "\\\\(mathbb|mathbf|mathcal|mathfrak|mathrm)\\{",
            // 特殊括号
            "\\\\(left|right)[\\[\\]\\(\\)\\{\\}\\|]",
            // 矩阵环境
            "\\\\begin\\{(matrix|pmatrix|bmatrix|vmatrix|cases)\\}",
            // 数学运算符
            "\\\\(sum|prod|int|oint|iint|iiint)_",
            // 分数形式
            "\\\\frac\\{[^}]+\\}\\{[^}]+\\}",
            // 开根号
            "\\\\sqrt\\{[^}]+\\}",
            // 上下标组合
            "[a-zA-Z0-9]_\\{[^}]+\\}\\^\\{[^}]+\\}",
            // 简单数学表达式
            "[a-zA-Z]+\\s*[=<>]\\s*[a-zA-Z0-9]+",
            // 多个数学符号
            "[a-zA-Z]\\s*[+\\-*/=]\\s*[a-zA-Z]"
        )

        return latexIndicators.any { pattern ->
            try {
                text.contains(Regex(pattern))
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 渲染预处理后的Markdown到TextView（包含LaTeX预处理）
     * @param textView 目标TextView
     * @param markdown Markdown文本
     */
    fun renderEnhancedToTextView(textView: TextView, markdown: String) {
        try {
            AppLog.d("MarkdownUtils: 开始增强渲染")
            AppLog.d("MarkdownUtils: 输入文本: $markdown")

            val preprocessedMarkdown = preprocessMarkdown(markdown)
            AppLog.d("MarkdownUtils: 预处理后: $preprocessedMarkdown")

            val latexProcessedText = preprocessLatexFormulas(preprocessedMarkdown)
            AppLog.d("MarkdownUtils: LaTeX处理后: $latexProcessedText")

            AppLog.d("MarkdownUtils: 调用renderToTextView")
            renderToTextView(textView, latexProcessedText)

            AppLog.d("MarkdownUtils: 增强渲染完成")
        } catch (e: Exception) {
            AppLog.e("MarkdownUtils: 增强渲染失败 ${e.message}")
            // 回退到直接设置文本
            textView.text = "渲染失败: ${e.message}\n原始内容: $markdown"
            throw e
        }
    }

    /**
     * 获取默认的Markwon实例（推荐使用）
     * @param context 上下文
     * @return 配置完整的Markwon实例
     */
    fun getDefaultMarkwon(context: Context): Markwon {
        if (defaultMarkwon == null) {
            initializeDefaultMarkwon(context)
        }
        return defaultMarkwon!!
    }

    /**
     * 获取优化的Markwon实例（高性能场景）
     * @param context 上下文
     * @return 性能优化的Markwon实例
     */
    fun getOptimizedMarkwon(context: Context): Markwon {
        val cacheKey = "optimized_48"
        
        synchronized(cacheLock) {
            return markwonCache.getOrPut(cacheKey) {
                AppLog.d("MarkdownUtils: 创建优化的Markwon实例")
                initializeJLatexMath(context)
                createMarkwonInstance(context, 48f)
            }
        }
    }

    /**
     * 获取轻量级Markwon实例（简单场景）
     * @param context 上下文
     * @return 轻量级Markwon实例
     */
    fun getLightweightMarkwon(context: Context): Markwon {
        val cacheKey = "lightweight"
        
        synchronized(cacheLock) {
            return markwonCache.getOrPut(cacheKey) {
                AppLog.d("MarkdownUtils: 创建轻量级Markwon实例")
                
                // 轻量级版本只包含基本功能
                Markwon.builder(context.applicationContext)
                    .usePlugin(GlideImagesPlugin.create(context))
                    .usePlugin(LinkifyPlugin.create())
                    .usePlugin(SoftBreakAddsNewLinePlugin.create())
                    .build()
            }
        }
    }
} 