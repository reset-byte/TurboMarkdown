package com.github.turbomarkwon.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.turbomarkwon.R
import com.github.turbomarkwon.util.AppLog
import android.util.Base64

/**
 * 专门用于展示Mermaid图表的自定义View
 * 功能特性：
 * - 使用WebView + Mermaid.js渲染图表
 * - 支持多种Mermaid图表类型（流程图、序列图、甘特图等）
 * - 响应式设计，适配不同屏幕尺寸
 * - 错误处理和加载状态显示
 * - 与Android原生代码的JavaScript接口
 */
class MermaidDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    
    private var mermaidContent: String = ""
    private var isPageReady: Boolean = false
    private var renderCallback: ((Boolean, String?) -> Unit)? = null
    
    companion object {
        private const val MERMAID_TEMPLATE_FILE = "file:///android_asset/mermaid_template.html"
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        
        // 设置背景
        background = ContextCompat.getDrawable(context, R.drawable.bg_rectangle)
        
        initializeViews()
        setupWebView()
    }

    /**
     * 初始化所有子视图
     */
    private fun initializeViews() {
        // 创建进度条
        progressBar = ProgressBar(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 32)
                gravity = android.view.Gravity.CENTER
            }
            isIndeterminate = true
        }
        
        // 创建错误文本视图
        errorTextView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            setTextColor(Color.RED)
            textSize = 14f
            visibility = GONE
            gravity = android.view.Gravity.CENTER
        }
        
        // 创建WebView
        webView = WebView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            visibility = GONE
        }
        
        // 添加到布局
        addView(progressBar)
        addView(errorTextView)
        addView(webView)
    }

    /**
     * 设置WebView配置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(false)
        }
        
        // 设置WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                AppLog.d("MermaidDisplayView: WebView页面加载完成")
            }
            
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                AppLog.e("MermaidDisplayView: WebView加载错误 - $description")
                showError("页面加载失败: $description")
            }
        }
        
        // 设置WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    AppLog.d("MermaidDisplayView: WebView Console - ${it.message()}")
                }
                return true
            }
        }
        
        // 添加JavaScript接口
        webView.addJavascriptInterface(MermaidJavaScriptInterface(), "MermaidInterface")
    }

    /**
     * 设置Mermaid图表内容
     * @param content Mermaid图表的内容
     * @param callback 渲染完成后的回调 (成功/失败, 错误信息)
     */
    fun setMermaidContent(content: String, callback: ((Boolean, String?) -> Unit)? = null) {
        this.mermaidContent = content.trim()
        this.renderCallback = callback
        
        if (mermaidContent.isEmpty()) {
            showError("Mermaid内容不能为空")
            callback?.invoke(false, "内容为空")
            return
        }
        
        AppLog.d("MermaidDisplayView: 设置Mermaid内容，长度=${mermaidContent.length}")
        
        // 显示加载状态
        showLoading()
        
        // 加载HTML模板
        loadMermaidTemplate()
    }

    /**
     * 加载Mermaid模板
     */
    private fun loadMermaidTemplate() {
        try {
            AppLog.d("MermaidDisplayView: 开始加载Mermaid模板")
            webView.loadUrl(MERMAID_TEMPLATE_FILE)
        } catch (e: Exception) {
            AppLog.e("MermaidDisplayView: 加载模板失败 - ${e.message}")
            showError("加载模板失败: ${e.message}")
            renderCallback?.invoke(false, e.message)
        }
    }

    /**
     * 渲染Mermaid图表
     */
    private fun renderMermaidChart() {
        if (!isPageReady || mermaidContent.isEmpty()) {
            AppLog.d("MermaidDisplayView: 页面未就绪或内容为空，延迟渲染")
            return
        }
        
        try {
            // 使用Base64编码内容以避免特殊字符问题
            val base64Content = Base64.encodeToString(mermaidContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val javascript = "window.renderMermaidFromBase64('$base64Content');"
            
            AppLog.d("MermaidDisplayView: 执行JavaScript渲染")
            webView.evaluateJavascript(javascript) { result ->
                AppLog.d("MermaidDisplayView: JavaScript执行结果: $result")
            }
        } catch (e: Exception) {
            AppLog.e("MermaidDisplayView: 渲染失败 - ${e.message}")
            showError("渲染失败: ${e.message}")
            renderCallback?.invoke(false, e.message)
        }
    }

    /**
     * 显示加载状态
     */
    private fun showLoading() {
        progressBar.visibility = VISIBLE
        errorTextView.visibility = GONE
        webView.visibility = GONE
    }

    /**
     * 显示成功状态
     */
    private fun showSuccess() {
        progressBar.visibility = GONE
        errorTextView.visibility = GONE
        webView.visibility = VISIBLE
    }

    /**
     * 显示错误状态
     */
    private fun showError(message: String) {
        progressBar.visibility = GONE
        errorTextView.text = message
        errorTextView.visibility = VISIBLE
        webView.visibility = GONE
    }

    /**
     * JavaScript接口类
     */
    private inner class MermaidJavaScriptInterface {
        
        @JavascriptInterface
        fun onPageReady() {
            AppLog.d("MermaidDisplayView: JavaScript页面就绪")
            post {
                isPageReady = true
                renderMermaidChart()
            }
        }
        
        @JavascriptInterface
        fun onRenderComplete() {
            AppLog.d("MermaidDisplayView: Mermaid渲染完成")
            post {
                showSuccess()
                renderCallback?.invoke(true, null)
            }
        }
        
        @JavascriptInterface
        fun onRenderError(message: String) {
            AppLog.e("MermaidDisplayView: Mermaid渲染错误 - $message")
            post {
                showError("渲染错误: $message")
                renderCallback?.invoke(false, message)
            }
        }
    }

    /**
     * 获取当前Mermaid内容
     */
    fun getMermaidContent(): String = mermaidContent

    /**
     * 清理WebView资源
     */
    fun destroy() {
        try {
            webView.destroy()
        } catch (e: Exception) {
            AppLog.e("MermaidDisplayView: 清理WebView失败 - ${e.message}")
        }
    }

    /**
     * 暂停WebView
     */
    fun onPause() {
        try {
            webView.onPause()
        } catch (e: Exception) {
            AppLog.e("MermaidDisplayView: 暂停WebView失败 - ${e.message}")
        }
    }

    /**
     * 恢复WebView
     */
    fun onResume() {
        try {
            webView.onResume()
        } catch (e: Exception) {
            AppLog.e("MermaidDisplayView: 恢复WebView失败 - ${e.message}")
        }
    }
} 