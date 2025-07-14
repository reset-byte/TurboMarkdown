package com.github.turbomarkwon.customtag

import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.BackgroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.graphics.Typeface
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.RenderProps
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.MarkwonHtmlRenderer
import io.noties.markwon.html.TagHandler
import com.github.turbomarkwon.util.AppLog

/**
 * 自定义样式标签处理器
 * 支持: warn, info, success, error, highlight, small, large等
 */
class CustomStyleTagHandler : TagHandler() {
    
    @NonNull
    override fun supportedTags(): Collection<String> {
        return listOf("warn", "info", "success", "error", "danger", "highlight", "mark", "small", "large", "primary", "secondary")
    }
    
    override fun handle(@NonNull visitor: MarkwonVisitor, @NonNull renderer: MarkwonHtmlRenderer, @NonNull tag: HtmlTag) {
        val tagName = tag.name()
        AppLog.d("CustomStyleTagHandler: 处理标签 '$tagName'")
        
        // 如果是块级标签，访问子元素
        if (tag.isBlock) {
            visitChildren(visitor, renderer, tag.asBlock)
        }
        
        // 根据标签类型获取相应的 spans
        val spans = getSpansForTag(tagName)
        if (spans != null) {
            SpannableBuilder.setSpans(visitor.builder(), spans, tag.start(), tag.end())
        }
    }
    
    @Nullable
    private fun getSpansForTag(tagName: String): Array<Any>? {
        return when (tagName) {
            "warn" -> arrayOf(
                ForegroundColorSpan(0xFFFF9800.toInt()),
                StyleSpan(Typeface.BOLD)
            )
            "info" -> arrayOf(
                ForegroundColorSpan(0xFF2196F3.toInt()),
                StyleSpan(Typeface.BOLD)
            )
            "success" -> arrayOf(
                ForegroundColorSpan(0xFF4CAF50.toInt()),
                StyleSpan(Typeface.BOLD)
            )
            "error", "danger" -> arrayOf(
                ForegroundColorSpan(0xFFF44336.toInt()),
                StyleSpan(Typeface.BOLD)
            )
            "highlight", "mark" -> arrayOf(
                BackgroundColorSpan(0xFFFFEB3B.toInt()),
                ForegroundColorSpan(0xFF000000.toInt())
            )
            "small" -> arrayOf(RelativeSizeSpan(0.8f))
            "large" -> arrayOf(RelativeSizeSpan(1.2f))
            "primary" -> arrayOf(
                ForegroundColorSpan(0xFF1976D2.toInt()),
                StyleSpan(Typeface.BOLD)
            )
            "secondary" -> arrayOf(ForegroundColorSpan(0xFF757575.toInt()))
            else -> null
        }
    }
}

/**
 * 自定义文本装饰标签处理器
 * 支持: u (下划线), s (删除线), sub (下标), sup (上标)
 */
class CustomTextDecorationTagHandler : TagHandler() {
    
    @NonNull
    override fun supportedTags(): Collection<String> {
        return listOf("u", "s", "sub", "sup")
    }
    
    override fun handle(@NonNull visitor: MarkwonVisitor, @NonNull renderer: MarkwonHtmlRenderer, @NonNull tag: HtmlTag) {
        val tagName = tag.name()
        AppLog.d("CustomTextDecorationTagHandler: 处理标签 '$tagName'")
        
        // 如果是块级标签，访问子元素
        if (tag.isBlock) {
            visitChildren(visitor, renderer, tag.asBlock)
        }
        
        // 根据标签类型获取相应的 spans
        val spans = getSpansForTag(tagName)
        if (spans != null) {
            SpannableBuilder.setSpans(visitor.builder(), spans, tag.start(), tag.end())
        }
    }
    
    @Nullable
    private fun getSpansForTag(tagName: String): Array<Any>? {
        return when (tagName) {
            "u" -> arrayOf(UnderlineSpan())
            "s" -> arrayOf(StrikethroughSpan())
            "sub" -> arrayOf(RelativeSizeSpan(0.7f))
            "sup" -> arrayOf(RelativeSizeSpan(0.7f))
            else -> null
        }
    }
} 