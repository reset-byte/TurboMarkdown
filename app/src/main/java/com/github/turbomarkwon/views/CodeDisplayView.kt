package com.github.turbomarkwon.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.github.turbomarkwon.R
import com.github.turbomarkwon.util.AppLog
import java.util.concurrent.ConcurrentHashMap

/**
 * 专门用于展示代码的自定义View
 * 功能特性：
 * - 水平滚动支持
 * - 语法高亮
 * - 复制代码功能
 * - 语言标识显示
 * - 行号显示（可选）
 * - 美观的代码块样式
 * - 语法高亮缓存
 */
class CodeDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        // 语法高亮缓存 - 静态缓存，在所有CodeDisplayView实例间共享
        private val syntaxHighlightCache = ConcurrentHashMap<String, Spanned>()
        
        /**
         * 清理语法高亮缓存
         */
        fun clearSyntaxCache() {
            syntaxHighlightCache.clear()
        }
        
        /**
         * 获取缓存大小
         */
        fun getCacheSize(): Int {
            return syntaxHighlightCache.size
        }
    }

    private lateinit var headerContainer: LinearLayout
    private lateinit var languageLabel: TextView
    private lateinit var copyButton: ImageButton
    private lateinit var scrollView: HorizontalScrollView
    private lateinit var codeTextView: TextView
    private lateinit var lineNumberView: TextView
    private lateinit var codeContainer: LinearLayout

    private var codeContent: String = ""
    private var languageType: String = ""
    private var showLineNumbers: Boolean = true

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 设置代码块整体样式
        background = ContextCompat.getDrawable(context, R.drawable.bg_rectangle)

        initializeViews()
        setupClickListeners()
    }

    /**
     * 初始化所有子视图
     */
    private fun initializeViews() {
        // 1. 创建头部容器（语言标签 + 复制按钮）
        headerContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 36, 48, 36)
            background = ContextCompat.getDrawable(context, R.drawable.bg_code_header)
        }

        // 语言标签
        languageLabel = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.black_100))
            typeface = Typeface.MONOSPACE
            text = "Code"
        }

        // 复制按钮
        copyButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_copy_black)
            background = null
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                resources.displayMetrics
            ).toInt()
            layoutParams = LayoutParams(size, size)
        }

        // 头部布局
        headerContainer.addView(languageLabel)
        headerContainer.addView(Space(context), LayoutParams(0, 0, 1f)) // 填充空间
        headerContainer.addView(copyButton)

        // 2. 创建代码显示区域
        scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isFillViewport = true
            
            // 防止水平滑动触摸事件冲突，阻止侧边栏被意外打开
            var startX = 0f
            var startY = 0f
            var isHorizontalScroll = false
            
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isHorizontalScroll = false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val deltaX = kotlin.math.abs(event.x - startX)
                        val deltaY = kotlin.math.abs(event.y - startY)
                        
                        // 判断是否为水平滑动（水平距离大于垂直距离且超过阈值）
                        if (deltaX > deltaY && deltaX > 20) {
                            if (!isHorizontalScroll) {
                                isHorizontalScroll = true
                                // 只在水平滑动时请求父视图不要拦截触摸事件
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        // 触摸结束时，恢复父视图的触摸事件处理
                        parent?.requestDisallowInterceptTouchEvent(false)
                        isHorizontalScroll = false
                    }
                }
                false // 返回false，让HorizontalScrollView继续处理滑动
            }
        }

        // 代码容器（行号 + 代码内容）
        codeContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.black_0))
            setPadding(0, 24, 0, 24)
        }

        // 行号视图
        lineNumberView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.black_40))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.END
            setPadding(12, 0, 8, 0)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32f,
                resources.displayMetrics
            ).toInt()
        }

        // 代码文本视图
        codeTextView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.black_90))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            setPadding(8, 0, 16, 0)
            setHorizontallyScrolling(true)
            isHorizontalScrollBarEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
        }

        // 组装视图层次
        codeContainer.addView(lineNumberView)
        codeContainer.addView(codeTextView)
        scrollView.addView(codeContainer)

        addView(headerContainer)
        addView(scrollView)
    }

    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        copyButton.setOnClickListener {
            copyCodeToClipboard()
        }
    }

    /**
     * 设置代码内容
     * @param code 代码文本
     * @param language 编程语言类型
     */
    fun setCode(code: String, language: String = "") {
        this.codeContent = code.trimEnd()
        this.languageType = language

        // 更新语言标签
        updateLanguageLabel()

        // 更新代码显示
        updateCodeDisplay()

        // 更新行号
        if (showLineNumbers) {
            updateLineNumbers()
        }
    }

    /**
     * 更新语言标签显示
     */
    private fun updateLanguageLabel() {
        val displayLanguage = when (languageType.lowercase()) {
            "kotlin", "kt" -> "Kotlin"
            "java" -> "Java"
            "javascript", "js" -> "JavaScript"
            "typescript", "ts" -> "TypeScript"
            "python", "py" -> "Python"
            "cpp", "c++" -> "C++"
            "c" -> "C"
            "csharp", "cs" -> "C#"
            "go" -> "Go"
            "rust", "rs" -> "Rust"
            "swift" -> "Swift"
            "dart" -> "Dart"
            "php" -> "PHP"
            "ruby", "rb" -> "Ruby"
            "shell", "bash", "sh" -> "Shell"
            "sql" -> "SQL"
            "json" -> "JSON"
            "xml" -> "XML"
            "html" -> "HTML"
            "css" -> "CSS"
            "markdown", "md" -> "Markdown"
            "yaml", "yml" -> "YAML"
            "dockerfile" -> "Dockerfile"
            else -> if (languageType.isNotEmpty()) languageType.uppercase() else "Code"
        }

        languageLabel.text = displayLanguage
    }

    /**
     * 更新代码显示（应用语法高亮）
     */
    private fun updateCodeDisplay() {
        try {
            // 生成缓存键：代码内容 + 语言类型
            val cacheKey = "${codeContent.hashCode()}_${languageType}"
            
            // 检查缓存
            val cachedHighlight = syntaxHighlightCache[cacheKey]
            if (cachedHighlight != null) {
                codeTextView.text = cachedHighlight
                AppLog.cache("语法高亮缓存命中: $languageType, 缓存大小: ${syntaxHighlightCache.size}")
                return
            }
            
            // 应用语法高亮并缓存结果
            val highlightedText = applySyntaxHighlighting(codeContent, languageType)
            syntaxHighlightCache[cacheKey] = highlightedText
            codeTextView.text = highlightedText
            AppLog.cache("语法高亮已缓存: $languageType, 代码长度: ${codeContent.length}, 缓存大小: ${syntaxHighlightCache.size}")
        } catch (e: Exception) {
            // 回退到普通文本显示
            codeTextView.text = codeContent
            AppLog.e("语法高亮失败: ${e.message}")
        }
    }

    /**
     * 更新行号显示
     */
    private fun updateLineNumbers() {
        val lineCount = codeContent.count { it == '\n' } + 1
        val lineNumbers = StringBuilder()

        for (i in 1..lineCount) {
            lineNumbers.append(i.toString().padStart(3, ' '))
            if (i < lineCount) {
                lineNumbers.append('\n')
            }
        }

        lineNumberView.text = lineNumbers.toString()
        lineNumberView.visibility = if (lineCount > 1) View.VISIBLE else View.GONE
    }

    /**
     * 应用语法高亮
     * @param code 代码文本
     * @param language 编程语言
     * @return 高亮后的Spanned文本
     */
    private fun applySyntaxHighlighting(code: String, language: String): Spanned {
        return try {
            // 基础语法高亮实现
            val spannableBuilder = SpannableStringBuilder(code)

            when (language.lowercase()) {
                "kotlin", "kt" -> applyKotlinHighlighting(spannableBuilder)
                "java" -> applyJavaHighlighting(spannableBuilder)
                "javascript", "js" -> applyJavaScriptHighlighting(spannableBuilder)
                "python", "py" -> applyPythonHighlighting(spannableBuilder)
                "json" -> applyJsonHighlighting(spannableBuilder)
                else -> applyGenericHighlighting(spannableBuilder)
            }

            spannableBuilder
        } catch (e: Exception) {
            SpannableStringBuilder(code)
        }
    }

    /**
     * 应用Kotlin语法高亮
     */
    private fun applyKotlinHighlighting(builder: SpannableStringBuilder) {
        val keywords = listOf(
            "class", "fun", "val", "var", "if", "else", "when", "for", "while", "do",
            "return", "break", "continue", "object", "interface", "abstract", "private",
            "public", "protected", "internal", "open", "final", "override", "companion",
            "data", "sealed", "enum", "annotation", "suspend", "inline", "crossinline",
            "noinline", "reified", "import", "package", "try", "catch", "finally",
            "throw", "throws", "in", "out", "is", "as", "null", "true", "false"
        )

        applyKeywordHighlighting(builder, keywords)
        applyStringHighlighting(builder)
        applyCommentHighlighting(builder)
        applyNumberHighlighting(builder)
    }

    /**
     * 应用Java语法高亮
     */
    private fun applyJavaHighlighting(builder: SpannableStringBuilder) {
        val keywords = listOf(
            "class", "interface", "abstract", "public", "private", "protected", "static",
            "final", "void", "int", "long", "double", "float", "boolean", "char", "byte",
            "short", "String", "if", "else", "switch", "case", "default", "for", "while",
            "do", "return", "break", "continue", "try", "catch", "finally", "throw",
            "throws", "new", "this", "super", "extends", "implements", "import", "package",
            "synchronized", "volatile", "transient", "native", "strictfp", "null", "true", "false"
        )

        applyKeywordHighlighting(builder, keywords)
        applyStringHighlighting(builder)
        applyCommentHighlighting(builder)
        applyNumberHighlighting(builder)
    }

    /**
     * 应用JavaScript语法高亮
     */
    private fun applyJavaScriptHighlighting(builder: SpannableStringBuilder) {
        val keywords = listOf(
            "function", "var", "let", "const", "if", "else", "switch", "case", "default",
            "for", "while", "do", "return", "break", "continue", "try", "catch", "finally",
            "throw", "new", "this", "typeof", "instanceof", "in", "delete", "void",
            "true", "false", "null", "undefined", "class", "extends", "super", "static",
            "import", "export", "from", "default", "async", "await", "yield"
        )

        applyKeywordHighlighting(builder, keywords)
        applyStringHighlighting(builder)
        applyCommentHighlighting(builder)
        applyNumberHighlighting(builder)
    }

    /**
     * 应用Python语法高亮
     */
    private fun applyPythonHighlighting(builder: SpannableStringBuilder) {
        val keywords = listOf(
            "def", "class", "if", "elif", "else", "for", "while", "break", "continue",
            "return", "try", "except", "finally", "raise", "with", "as", "import", "from",
            "and", "or", "not", "in", "is", "lambda", "global", "nonlocal", "yield",
            "True", "False", "None", "pass", "del", "async", "await"
        )

        applyKeywordHighlighting(builder, keywords)
        applyStringHighlighting(builder)
        applyCommentHighlighting(builder, "#")
        applyNumberHighlighting(builder)
    }

    /**
     * 应用JSON语法高亮
     */
    private fun applyJsonHighlighting(builder: SpannableStringBuilder) {
        // JSON关键字
        val keywords = listOf("true", "false", "null")
        applyKeywordHighlighting(builder, keywords)

        // 字符串高亮
        applyStringHighlighting(builder)

        // 数字高亮
        applyNumberHighlighting(builder)
    }

    /**
     * 应用通用语法高亮
     */
    private fun applyGenericHighlighting(builder: SpannableStringBuilder) {
        applyStringHighlighting(builder)
        applyCommentHighlighting(builder)
        applyNumberHighlighting(builder)
    }

    /**
     * 应用关键字高亮
     */
    private fun applyKeywordHighlighting(builder: SpannableStringBuilder, keywords: List<String>) {
        val keywordColor = Color.parseColor("#d73a49") // 红色

        keywords.forEach { keyword ->
            val regex = "\\b$keyword\\b".toRegex()
            regex.findAll(builder.toString()).forEach { match ->
                builder.setSpan(
                    ForegroundColorSpan(keywordColor),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    /**
     * 应用字符串高亮
     */
    private fun applyStringHighlighting(builder: SpannableStringBuilder) {
        val stringColor = Color.parseColor("#032f62") // 蓝色

        // 双引号字符串
        val doubleQuoteRegex = "\"([^\"]|\\\\.)*\"".toRegex()
        doubleQuoteRegex.findAll(builder.toString()).forEach { match ->
            builder.setSpan(
                ForegroundColorSpan(stringColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 单引号字符串
        val singleQuoteRegex = "'([^']|\\\\.)*'".toRegex()
        singleQuoteRegex.findAll(builder.toString()).forEach { match ->
            builder.setSpan(
                ForegroundColorSpan(stringColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 应用注释高亮
     */
    private fun applyCommentHighlighting(
        builder: SpannableStringBuilder,
        commentStart: String = "//"
    ) {
        val commentColor = Color.parseColor("#6a737d") // 灰色

        // 单行注释
        val commentRegex = "${Regex.escape(commentStart)}.*$".toRegex(RegexOption.MULTILINE)
        commentRegex.findAll(builder.toString()).forEach { match ->
            builder.setSpan(
                ForegroundColorSpan(commentColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(Typeface.ITALIC),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 多行注释（/* */）
        val multiCommentRegex = "/\\*[\\s\\S]*?\\*/".toRegex()
        multiCommentRegex.findAll(builder.toString()).forEach { match ->
            builder.setSpan(
                ForegroundColorSpan(commentColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(Typeface.ITALIC),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 应用数字高亮
     */
    private fun applyNumberHighlighting(builder: SpannableStringBuilder) {
        val numberColor = Color.parseColor("#005cc5") // 蓝色

        val numberRegex = "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b".toRegex()
        numberRegex.findAll(builder.toString()).forEach { match ->
            builder.setSpan(
                ForegroundColorSpan(numberColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    /**
     * 复制代码到剪贴板
     */
    private fun copyCodeToClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Code", codeContent)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(context, "代码已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置是否显示行号
     * @param show 是否显示行号
     */
    fun setShowLineNumbers(show: Boolean) {
        this.showLineNumbers = show
        if (codeContent.isNotEmpty()) {
            updateLineNumbers()
        }
    }

    /**
     * 获取代码内容
     * @return 当前显示的代码内容
     */
    fun getCodeContent(): String = codeContent

    /**
     * 获取语言类型
     * @return 当前代码的语言类型
     */
    fun getLanguageType(): String = languageType
} 