package com.github.turbomarkwon.data

/**
 * 示例Markdown内容
 */
object SampleMarkdown {
    
    /**
     * 超长技术文档示例
     */
    val SAMPLE_LONG_MARKDOWN = """
# TurboMarkdown 技术文档

## 1. 概述

TurboMarkdown 是一个专为 Android 平台设计的高性能 Markdown 渲染库，解决了在移动设备上渲染超长 Markdown 文档时的性能问题。

### 1.1 核心特性

- **分块渲染**: 使用 RecyclerView 实现分块渲染，支持超长文档流畅滚动
- **异步解析**: 在后台线程解析 Markdown，不阻塞主线程
- **缓存优化**: 智能缓存解析结果，减少重复计算
- **图片异步加载**: 集成 Glide 实现图片异步加载，避免阻塞
- **语法高亮**: 支持多种编程语言的语法高亮

### 1.2 架构设计

本项目采用 MVVM 架构模式：

- **Model**: 数据模型和业务逻辑
- **View**: UI 层，包含 Activity、Fragment 和 ViewHolder
- **ViewModel**: 数据绑定和状态管理

## 2. 技术实现

### 2.1 分块渲染原理

```kotlin
// 解析Markdown为AST节点
val document = parser.parse(markdownText)
val items = parseDocument(document)

// 为每个节点创建对应的ViewHolder
when (child) {
    is Paragraph -> MarkdownItem.Paragraph(id, child)
    is Heading -> MarkdownItem.Heading(id, child, level)
    is CodeBlock -> MarkdownItem.CodeBlock(id, child, language)
    // ... 其他类型
}
```

### 2.2 性能优化策略

1. **内存管理**
   - 使用 `DiffUtil` 进行高效的列表更新
   - 实现 ViewHolder 复用池优化
   - 图片加载内存控制

2. **渲染优化**
   - 避免使用 `textIsSelectable`
   - 启用硬件加速
   - PrecomputedTextCompat 支持

3. **线程管理**
   - 后台线程解析 Markdown
   - 主线程仅负责 UI 渲染
   - 协程管理异步操作

## 3. 使用示例

### 3.1 基本用法

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MarkdownViewModel
    private lateinit var adapter: MarkdownAdapter
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMarkwon()
        setupRecyclerView()
        setupViewModel()
        loadSampleMarkdown()
    }
}
```

### 3.2 高级配置

```kotlin
// 创建优化的Markwon实例
val markwon = MarkwonConfig.createOptimizedMarkwon(context)

// 配置RecyclerView
recyclerView.apply {
    layoutManager = LinearLayoutManager(context)
    adapter = MarkdownAdapter(markwon)
    setHasFixedSize(false)
    
    // 优化滚动性能
    setItemViewCacheSize(20)
    recycledViewPool.setMaxRecycledViews(0, 10)
}
```

## 4. 支持的Markdown语法

### 4.1 基本语法

#### 标题
```markdown
# 一级标题
## 二级标题
### 三级标题
#### 四级标题
##### 五级标题
###### 六级标题
```

#### 文本样式
- **粗体文本**
- *斜体文本*
- ~~删除线~~
- `内联代码`

#### 列表

**无序列表:**
- 项目1
- 项目2
  - 嵌套项目2.1
  - 嵌套项目2.2
- 项目3

**有序列表:**
1. 第一项
2. 第二项
   1. 子项目2.1
   2. 子项目2.2
3. 第三项

#### 任务列表
- [x] 已完成任务
- [ ] 未完成任务
- [x] 另一个已完成任务

### 4.2 高级语法

#### 代码块

**Kotlin代码:**
```kotlin
class MarkdownParser {
    private val parser: Parser = Parser.builder().build()
    
    suspend fun parseMarkdownAsync(text: String): ParseResult = 
        withContext(Dispatchers.Default) {
            val document = parser.parse(text)
            parseDocument(document)
        }
}
```

**Java代码:**
```java
public class AndroidExample {
    private RecyclerView recyclerView;
    private MarkdownAdapter adapter;
    
    public void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}
```

**JavaScript代码:**
```javascript
function optimizePerformance() {
    const cache = new Map();
    
    return function render(content) {
        if (cache.has(content)) {
            return cache.get(content);
        }
        
        const result = processContent(content);
        cache.set(content, result);
        return result;
    };
}
```

#### 引用块

> 这是一个引用块示例。
> 
> 引用块可以包含多行内容，并且支持嵌套。
> 
> > 这是嵌套的引用块。
> > 
> > 可以包含**格式化文本**和`代码`。

#### 表格

| 功能 | 传统方案 | TurboMarkdown | 性能提升 | 兼容性 | 稳定性 | 易用性 | 可扩展性 | 社区支持 |
|------|----------|---------------|----------|--------|--------|--------|----------|----------|
| 解析速度 | 慢 | 快 | 300% | 良好 | 稳定 | 简单 | 强 | 活跃 |
| 内存使用 | 高 | 低 | 50% | 兼容 | 可靠 | 友好 | 灵活 | 完善 |
| 滚动流畅度 | 卡顿 | 流畅 | 显著提升 | 全面 | 优秀 | 直观 | 丰富 | 及时 |
| 图片加载 | 阻塞 | 异步 | 完全不阻塞 | 广泛 | 健壮 | 便捷 | 模块化 | 专业 |

#### 分隔线

---

#### 链接和图片

[GitHub仓库](https://github.com/example/turbomarkdown)

![示例图片](https://via.placeholder.com/300x200?text=TurboMarkdown)

#### 数学公式

行内公式：质能方程 mc²

块级公式：
积分公式示例

## 5. 性能测试数据

### 5.1 测试环境
- 设备：小米 Redmi Note 8 Pro
- Android 版本：Android 11
- 内存：6GB
- 测试文档：10,000 行技术文档

### 5.2 性能对比

| 指标 | 传统TextView | TurboMarkdown | 提升幅度 |
|------|-------------|---------------|----------|
| 首次加载时间 | 3.2s | 0.8s | 4x |
| 内存峰值 | 145MB | 62MB | 2.3x |
| 滚动帧率 | 35fps | 60fps | 71% |
| 图片加载时间 | 2.1s | 0.3s | 7x |

## 6. 最佳实践

### 6.1 内存管理
```kotlin
// 及时清理缓存
override fun onDestroy() {
    super.onDestroy()
    MarkdownRenderer.clearCache()
}

// 合理配置RecyclerView
recyclerView.apply {
    setItemViewCacheSize(20)  // 根据设备性能调整
    setHasFixedSize(false)    // 支持动态高度
}
```

### 6.2 性能监控
```kotlin
// 添加性能监控
viewModel.renderState.observe(this) { state ->
    when (state) {
        is MarkdownRenderState.Success -> {
            val statistics = viewModel.getStatistics()
            Log.d("Performance", "Items: ${"$"}{statistics["total_items"]}")
            Log.d("Performance", "Cache: ${"$"}{statistics["cache_size"]}")
        }
    }
}
```

### 6.3 错误处理
```kotlin
// 优雅的错误处理
viewModel.errorMessage.observe(this) { error ->
    error?.let {
        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
    }
}
```

## 7. 扩展功能

### 7.1 自定义主题
```kotlin
// 创建自定义主题
val customTheme = createCustomTheme {
    // 自定义颜色配置
    primaryColor = Color.BLUE
    secondaryColor = Color.GREEN
    accentColor = Color.RED
}
```

### 7.2 插件系统
```kotlin
// 添加自定义插件
val markwon = Markwon.builder(context)
    .usePlugin(CustomPlugin())
    .build()
```

## 8. 故障排除

### 8.1 常见问题

**问题1：图片加载失败**
- 检查网络权限
- 确认图片URL有效
- 查看Glide配置

**问题2：滚动卡顿**
- 减少ViewHolder复用池大小
- 检查是否启用硬件加速
- 优化布局层次

**问题3：内存泄漏**
- 及时清理缓存
- 检查图片资源释放
- 使用内存分析工具

### 8.2 调试技巧

```kotlin
// 启用调试模式
MarkdownRenderer.setDebugMode(true)

// 监控内存使用
val memoryStats = viewModel.getMemoryStats()
Log.d("Memory", "Used: ${"$"}{memoryStats.used}MB")
```

## 9. 总结

TurboMarkdown 通过创新的分块渲染技术，成功解决了 Android 平台上渲染超长 Markdown 文档的性能问题。主要优势包括：

1. **极致性能**: 4倍的加载速度提升
2. **流畅体验**: 60fps 的滚动帧率
3. **内存优化**: 50% 的内存占用减少
4. **扩展性强**: 丰富的插件系统

## 10. 参考资料

- [Markwon 官方文档](https://github.com/noties/Markwon)
- [CommonMark 规范](https://commonmark.org/)
- [Android 性能优化指南](https://developer.android.com/topic/performance)
- [RecyclerView 最佳实践](https://developer.android.com/guide/topics/ui/layout/recyclerview)

---

*文档版本：1.0*  
*最后更新：2024年*  
*作者：TurboMarkdown 开发团队*
""".trimIndent()
    
    /**
     * 综合表格测试用例
     */
    const val COMPREHENSIVE_TABLE_TEST_MARKDOWN = """
# 综合表格测试

## 混合列数测试

这个测试包含了从1列到7列的各种表格，用于验证不同列数的优化效果。

### 1列表格

| 单列内容 |
|---------|
| 项目1 |
| 项目2 |
| 项目3 |

### 2列表格

| 功能 | 状态 |
|------|------|
| 解析 | ✅ |
| 渲染 | ✅ |
| 缓存 | ✅ |

### 3列表格

| 功能 | 状态 | 备注 |
|------|------|------|
| 解析 | ✅ | 完成 |
| 渲染 | ✅ | 完成 |
| 缓存 | ✅ | 完成 |

### 4列表格

| 功能 | 状态 | 备注 | 性能 |
|------|------|------|------|
| 解析 | ✅ | 完成 | A+ |
| 渲染 | ✅ | 完成 | A+ |
| 缓存 | ✅ | 完成 | A |

### 5列表格

| 功能 | 状态 | 备注 | 性能 | 内存 |
|------|------|------|------|------|
| 解析 | ✅ | 完成 | A+ | 优秀 |
| 渲染 | ✅ | 完成 | A+ | 优秀 |
| 缓存 | ✅ | 完成 | A | 良好 |

### 6列表格

| 功能 | 状态 | 备注 | 性能 | 内存 | 兼容性 |
|------|------|------|------|------|--------|
| 解析 | ✅ | 完成 | A+ | 优秀 | 优秀 |
| 渲染 | ✅ | 完成 | A+ | 优秀 | 优秀 |
| 缓存 | ✅ | 完成 | A | 良好 | 良好 |

### 7列表格

| 功能 | 状态 | 备注 | 性能 | 内存 | 兼容性 | 优化 |
|------|------|------|------|------|--------|------|
| 解析 | ✅ | 完成 | A+ | 优秀 | 优秀 | 是 |
| 渲染 | ✅ | 完成 | A+ | 优秀 | 优秀 | 是 |
| 缓存 | ✅ | 完成 | A | 良好 | 良好 | 是 |

## 优化策略总结

- **1-3列表格**：使用80%屏幕宽度，智能滚动条
- **4+列表格**：使用120%屏幕宽度，强制滚动条
- **所有表格**：保持流畅的水平滑动体验
"""
} 