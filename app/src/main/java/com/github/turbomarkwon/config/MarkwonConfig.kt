package com.github.turbomarkwon.config

import android.content.Context
import android.graphics.Color
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TableTheme

/**
 * Markwon配置类 - 集成所有插件和优化
 */
object MarkwonConfig {
    
    /**
     * 创建配置完整的Markwon实例
     */
    fun createMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            // 图片支持 - 使用Glide异步加载
            .usePlugin(GlideImagesPlugin.create(context))
            
            // 表格支持
            .usePlugin(TablePlugin.create(context))
            
            // 任务列表支持
            .usePlugin(TaskListPlugin.create(context))
            
            // HTML支持
            .usePlugin(HtmlPlugin.create())
            
            // 数学公式支持
            .usePlugin(JLatexMathPlugin.create(32f))
            
            // 自动链接识别
            .usePlugin(LinkifyPlugin.create())
            
            .build()
    }
    
    /**
     * 创建轻量级Markwon实例（用于简单场景）
     */
    fun createLightweightMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    
    /**
     * 创建针对性能优化的Markwon实例
     */
    fun createOptimizedMarkwon(context: Context): Markwon {
        val tableTheme = TableTheme.buildWithDefaults(context)
            .tableBorderColor(Color.parseColor("#DDDDDD"))
            .tableOddRowBackgroundColor(Color.parseColor("#FFFFFF"))
            .tableEvenRowBackgroundColor(Color.parseColor("#F9F9F9"))
            .tableBorderWidth(2)
            .build()
        return Markwon.builder(context)
            // 图片支持 - 配置Glide进行性能优化
            .usePlugin(GlideImagesPlugin.create(context))
            // 行内解析插件（必须在最前面）
            .usePlugin(MarkwonInlineParserPlugin.create())
            // 软换行插件（保持换行）
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            // 表格支持 + 自定义主题
            .usePlugin(TablePlugin.create(tableTheme))
            // 为表格启用点击链接 MovementMethod
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            // 任务列表支持
            .usePlugin(TaskListPlugin.create(context))
            // HTML支持
            .usePlugin(HtmlPlugin.create())
            // LaTeX公式
            .usePlugin(JLatexMathPlugin.create(32f))
            // 自动链接识别
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
} 