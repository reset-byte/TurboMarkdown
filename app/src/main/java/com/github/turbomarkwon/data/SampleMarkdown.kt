package com.github.turbomarkwon.data

/**
 * 示例Markdown内容
 */
object SampleMarkdown {
    
    /**
     * Mermaid图表专项测试数据
     */
    const val MERMAID_TEST_MARKDOWN = """
# Mermaid 图表测试套件

本文档专门用于测试各种类型的Mermaid图表渲染效果。

## 1. 流程图测试

### 1.1 基本流程图
```mermaid
flowchart TD
    A[开始] --> B{是否有数据?}
    B -->|是| C[处理数据]
    B -->|否| D[获取数据]
    C --> E[渲染图表]
    D --> C
    E --> F[显示结果]
    F --> G[结束]
```

### 1.2 复杂流程图
```mermaid
flowchart LR
    A[用户请求] --> B[身份验证]
    B -->|成功| C[权限检查]
    B -->|失败| D[登录页面]
    C -->|有权限| E[数据处理]
    C -->|无权限| F[错误提示]
    E --> G[缓存检查]
    G -->|命中| H[返回缓存]
    G -->|未命中| I[数据库查询]
    I --> J[更新缓存]
    J --> K[返回数据]
    H --> L[格式化输出]
    K --> L
    L --> M[发送响应]
```

## 2. 序列图测试

### 2.1 基本序列图
```mermaid
sequenceDiagram
    participant User as 用户
    participant App as 应用
    participant Server as 服务器
    participant DB as 数据库
    
    User->>App: 发起请求
    App->>Server: 转发请求
    Server->>DB: 查询数据
    DB-->>Server: 返回数据
    Server-->>App: 返回结果
    App-->>User: 显示结果
```

### 2.2 复杂序列图
```mermaid
sequenceDiagram
    participant Client as 客户端
    participant Gateway as 网关
    participant Auth as 认证服务
    participant Cache as 缓存
    participant Service as 业务服务
    participant DB as 数据库
    
    Client->>Gateway: 请求数据
    Gateway->>Auth: 验证token
    Auth-->>Gateway: 验证通过
    Gateway->>Cache: 检查缓存
    Cache-->>Gateway: 缓存未命中
    Gateway->>Service: 调用业务服务
    Service->>DB: 查询数据
    DB-->>Service: 返回数据
    Service->>Cache: 更新缓存
    Service-->>Gateway: 返回结果
    Gateway-->>Client: 返回数据
```

## 3. 类图测试

### 3.1 基本类图
```mermaid
classDiagram
    class MarkdownRenderer {
        +renderMarkdown(text: String)
        +clearCache()
        -parseNodes(nodes: List)
    }
    
    class CodeDisplayView {
        +setCode(code: String, language: String)
        +copyToClipboard()
        -highlightSyntax(code: String)
    }
    
    class MermaidDisplayView {
        +setMermaidContent(content: String)
        +destroy()
        -loadTemplate()
        -renderChart()
    }
    
    MarkdownRenderer --> CodeDisplayView : uses
    MarkdownRenderer --> MermaidDisplayView : uses
```

### 3.2 复杂类图
```mermaid
classDiagram
    class Application {
        <<interface>>
        +onCreate()
        +onDestroy()
    }
    
    class MainActivity {
        -binding: ActivityMainBinding
        -viewModel: MarkdownViewModel
        -adapter: MarkdownAdapter
        +onCreate()
        +setupRecyclerView()
        +showTestDialog()
    }
    
    class MarkdownAdapter {
        -markwon: Markwon
        +onCreateViewHolder()
        +onBindViewHolder()
        +getItemViewType()
    }
    
    class BaseViewHolder {
        <<abstract>>
        +bind(item: MarkdownItem)
        +onRecycled()
    }
    
    class CodeBlockViewHolder {
        -codeDisplayView: CodeDisplayView
        -mermaidDisplayView: MermaidDisplayView
        +bind()
        +showCodeBlock()
        +showMermaidDiagram()
    }
    
    Application <|-- MainActivity
    MainActivity --> MarkdownAdapter
    MarkdownAdapter --> BaseViewHolder
    BaseViewHolder <|-- CodeBlockViewHolder
    CodeBlockViewHolder --> CodeDisplayView
    CodeBlockViewHolder --> MermaidDisplayView
```

## 4. 状态图测试

### 4.1 基本状态图
```mermaid
stateDiagram-v2
    [*] --> Loading
    Loading --> Success
    Loading --> Error
    Success --> [*]
    Error --> Loading : 重试
    Error --> [*]
```

### 4.2 复杂状态图
```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Loading : 开始渲染
    Loading --> Parsing : 解析Markdown
    Parsing --> Rendering : 渲染视图
    Rendering --> Success : 成功
    Parsing --> Error : 解析失败
    Rendering --> Error : 渲染失败
    Error --> Loading : 重试
    Success --> Idle : 完成
    Success --> Loading : 重新加载
    
    state Loading {
        [*] --> Preparing
        Preparing --> Processing
        Processing --> Finalizing
        Finalizing --> [*]
    }
    
    state Error {
        [*] --> NetworkError
        [*] --> ParseError
        [*] --> RenderError
        NetworkError --> [*]
        ParseError --> [*]
        RenderError --> [*]
    }
```

## 5. 甘特图测试

### 5.1 项目进度甘特图
```mermaid
gantt
    title TurboMarkdown开发进度
    dateFormat  YYYY-MM-DD
    section 需求分析
    需求调研          :done, des1, 2024-01-01, 2024-01-07
    架构设计          :done, des2, 2024-01-08, 2024-01-14
    技术选型          :done, des3, 2024-01-15, 2024-01-21
    section 开发阶段
    基础框架          :done, dev1, 2024-01-22, 2024-02-05
    Markdown解析      :done, dev2, 2024-02-06, 2024-02-20
    UI渲染优化        :done, dev3, 2024-02-21, 2024-03-06
    性能优化          :active, dev4, 2024-03-07, 2024-03-20
    Mermaid支持       :active, dev5, 2024-03-15, 2024-03-25
    section 测试阶段
    单元测试          :test1, 2024-03-21, 2024-03-28
    集成测试          :test2, 2024-03-29, 2024-04-05
    性能测试          :test3, 2024-04-06, 2024-04-12
    section 发布阶段
    Beta版本          :beta, 2024-04-13, 2024-04-19
    正式发布          :release, 2024-04-20, 2024-04-26
```

## 6. 饼图测试

### 6.1 性能统计饼图
```mermaid
pie title 应用性能分布
    "渲染时间" : 42.5
    "解析时间" : 28.3
    "缓存命中" : 15.2
    "网络请求" : 8.7
    "其他" : 5.3
```

## 7. 用户旅程图测试

### 7.1 用户使用流程
```mermaid
journey
    title 用户使用TurboMarkdown流程
    section 初次使用
      下载应用 : 5: 用户
      打开应用 : 4: 用户
      查看示例 : 3: 用户
    section 日常使用
      加载文档 : 4: 用户
      浏览内容 : 5: 用户
      查看图表 : 5: 用户
      复制代码 : 4: 用户
    section 高级功能
      性能统计 : 3: 用户
      测试用例 : 2: 用户
      分享内容 : 4: 用户
```

## 8. Git图测试

### 8.1 Git分支流程
```mermaid
gitGraph
    commit id: "初始化项目"
    branch feature/markdown-parser
    checkout feature/markdown-parser
    commit id: "添加Markdown解析器"
    commit id: "优化解析性能"
    checkout main
    merge feature/markdown-parser
    commit id: "发布v1.0"
    branch feature/mermaid-support
    checkout feature/mermaid-support
    commit id: "添加Mermaid支持"
    commit id: "优化图表渲染"
    checkout main
    merge feature/mermaid-support
    commit id: "发布v1.1"
```

## 9. 思维导图测试

### 9.1 应用架构思维导图
```mermaid
mindmap
  root((TurboMarkdown))
    UI层
      MainActivity
      MarkdownAdapter
      ViewHolders
    业务层
      MarkdownViewModel
      MarkdownParser
      MarkdownRenderer
    数据层
      MarkdownItem
      SampleMarkdown
      Cache
    视图层
      CodeDisplayView
      MermaidDisplayView
      RecyclerView
```

---

## 测试总结

本测试套件包含了9种不同类型的Mermaid图表，用于验证：

1. **渲染正确性** - 各种图表类型是否正确显示
2. **性能表现** - 复杂图表的渲染速度
3. **内存使用** - 多个图表的内存占用情况
4. **错误处理** - 异常情况的处理能力
5. **用户体验** - 图表的交互和显示效果

通过这些测试用例，可以全面评估Mermaid图表渲染功能的质量和稳定性。
"""

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
val markwon = MarkdownUtils.getOptimizedMarkwon(context)

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
     * 自定义容器测试数据
     */
    val CONTAINER_TEST_MARKDOWN = """
# 自定义容器语法测试

本文档展示新的 :::type 容器语法功能，支持多种类型的信息容器。

## 📘 基础容器类型

### 提示容器
:::note
这是一个基础的提示信息容器。
支持**Markdown语法**和*格式化文本*。

- 列表项目1
- 列表项目2
:::

### 建议容器
:::tip
💡 这里是一些有用的建议和小贴士。

可以包含代码块：
```kotlin
fun showTip() {
    println("这是一个建议")
}
```
:::

### 警告容器
:::warning
⚠️ 请注意这个重要的警告信息！

这种容器适合显示需要用户注意的内容。
:::

### 危险容器
:::danger
❗ 这是一个严重的警告或错误信息。

请谨慎操作，避免数据丢失。
:::

### 信息容器
:::info
🛠 这里是一些技术信息或说明。

适合显示技术细节、配置说明等内容。
:::

### 成功容器
:::success
✅ 操作已成功完成！

这种容器适合显示成功状态或完成信息。
:::

## 🎯 高级用法

### 自定义标题
:::note 自定义提示标题
可以为容器指定自定义标题，替换默认标题。
:::

:::warning 重要配置说明
自定义标题让容器更加灵活和具体。
:::

### 问题容器
:::question
❓ 这是一个常见问题或需要思考的问题？

适合FAQ部分或引导用户思考。
:::

### 重要信息
:::important
📌 这是非常重要的信息，必须引起注意！

用于强调关键内容或必读信息。
:::

### 示例容器
:::example
🧪 这是一个实际的使用示例：

```javascript
// 初始化容器
const container = new Container('note');
container.setContent('这是内容');
container.render();
```

输出结果：
- 创建了一个note类型的容器
- 设置了相应的内容
- 渲染到页面上
:::

## 🔄 容器嵌套和复杂内容

### 包含表格的容器
:::info 数据统计表格
下面是一个包含表格的信息容器：

| 容器类型 | 图标 | 颜色 | 用途 |
|---------|------|------|------|
| note | 📘 | 蓝色 | 一般提示 |
| tip | 💡 | 绿色 | 建议 |
| warning | ⚠️ | 橙色 | 警告 |
| danger | ❗ | 红色 | 危险 |
:::

### 包含数学公式的容器
:::example 数学公式示例
容器中也可以包含LaTeX数学公式：

行内公式：这是质能方程 ${'$'}E = mc^2${'$'}

块级公式：
${'$'}${'$'}\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}${'$'}${'$'}

复杂公式：
${'$'}${'$'}\frac{\partial f}{\partial x} = \lim_{h \to 0} \frac{f(x+h) - f(x)}{h}${'$'}${'$'}

矩阵示例：
${'$'}${'$'}A = \begin{pmatrix}
a_{11} & a_{12} & a_{13} \\
a_{21} & a_{22} & a_{23} \\
a_{31} & a_{32} & a_{33}
\end{pmatrix}${'$'}${'$'}
:::

### 包含图片的容器
:::tip 图片展示
容器中也可以包含图片：

![示例图片](https://via.placeholder.com/300x200?text=Container+Image)

图片说明：这是一个在容器中显示的示例图片。
:::

### 包含 Mermaid 图表的容器
:::example Mermaid 图表展示
容器内可以包含各种类型的 Mermaid 图表：

```mermaid
flowchart TD
    A[开始] --> B{检查条件}
    B -->|是| C[执行操作]
    B -->|否| D[跳过]
    C --> E[结束]
    D --> E
```

这个流程图展示了容器内 Mermaid 图表的渲染效果。
:::

### 包含多种代码语言的容器
:::info 代码示例集合
容器内可以包含多种编程语言的代码：

**Kotlin 代码：**
```kotlin
class ContainerRenderer {
    fun renderContent(content: String) {
    }
}
```

**Python 代码：**
```python
def process_container(data):
    for item in data:
        print(f"处理项目: {item}")
    return True
```

**SQL 查询：**
```sql
SELECT container_type, COUNT(*) as count
FROM containers 
WHERE created_at > '2024-01-01'
GROUP BY container_type
ORDER BY count DESC;
```
:::

### 复杂嵌套内容容器
:::important 综合功能演示
这个容器展示了多种内容类型的混合使用：

## 子标题：性能分析

### 数据表格
| 指标 | 传统方案 | 新方案 | 提升 |
|------|----------|--------|------|
| 渲染速度 | 100ms | 25ms | 4x |
| 内存使用 | 50MB | 20MB | 2.5x |

### 算法实现
```typescript
interface ContainerConfig {
    type: string;
    title?: string;
    color: string;
    icon: string;
}

class AdvancedContainer {
    private config: ContainerConfig;
    
    constructor(config: ContainerConfig) {
        this.config = config;
    }
    
    render(): HTMLElement {
        // 渲染逻辑
        return this.createContainerElement();
    }
}
```

### 数学模型
性能提升的数学模型：

${'$'}${'$'}P_{improvement} = \frac{T_{old} - T_{new}}{T_{old}} \times 100\%${'$'}${'$'}

其中：
- ${'$'}T_{old}${'$'} 是原始渲染时间
- ${'$'}T_{new}${'$'} 是优化后渲染时间

### 架构图
```mermaid
graph LR
    A[容器解析器] --> B[内容分析器]
    B --> C[类型识别器]
    C --> D[渲染引擎]
    D --> E[视图组合器]
    E --> F[最终输出]
```

### 总结列表
- ✅ 支持代码高亮
- ✅ 支持数学公式
- ✅ 支持表格渲染
- ✅ 支持图表展示
- ✅ 支持图片显示
- ✅ 支持嵌套内容
:::

## 🛠 技术实现

### 实现原理
:::info 技术架构
容器功能基于以下组件实现：

1. **ContainerNode**: 自定义AST节点
2. **ContainerBlockParser**: 解析:::语法
3. **ContainerViewHolder**: 复合内容渲染器
4. **ContainerPlugin**: 整合所有组件

支持的语法格式：
- `:::type` - 基础容器
- `:::type 自定义标题` - 带标题容器
- `:::` - 容器结束标记

### 渲染流程
```mermaid
sequenceDiagram
    participant P as 解析器
    participant A as 适配器
    participant V as ViewHolder
    participant R as 渲染器
    
    P->>A: 创建容器项目
    A->>V: 绑定容器数据
    V->>V: 分析子节点
    V->>R: 渲染不同类型内容
    R->>V: 返回渲染结果
    V->>V: 组合最终视图
```
:::

### 配置信息
:::example 容器配置示例
```kotlin
val containerConfig = ContainerConfig(
    icon = "📘",
    colorRes = "#2196F3", 
    title = "提示"
)

// 支持的所有容器类型
val supportedTypes = listOf(
    "note", "tip", "warning", "danger", "error",
    "info", "success", "question", "important", "example"
)
```

容器内容分析器：
```kotlin
class ContainerContentAnalyzer {
    fun analyzeAndRender(node: Node): List<View> {
        val views = mutableListOf<View>()
        
        node.children.forEach { child ->
            when (child) {
                is FencedCodeBlock -> views.add(createCodeView(child))
                is TableBlock -> views.add(createTableView(child))
                is Paragraph -> {
                    if (containsMathFormula(child)) {
                        views.add(createMathView(child))
                    } else {
                        views.add(createTextView(child))
                    }
                }
                // ... 其他类型处理
            }
        }
        
        return views
    }
}
```
:::

## 📊 性能和兼容性

### 性能特性
:::success 优化特性
✅ 高性能渲染 - 每个子内容独立渲染  
✅ 内存优化 - 按需创建视图组件  
✅ 缓存支持 - 代码块和图表缓存  
✅ 滚动优化 - 支持表格水平滚动  
✅ 异步渲染 - Mermaid 图表异步加载  
:::

### 兼容性说明
:::warning 兼容性提醒
- 需要Android API 21+
- 支持所有Markdown语法
- 与现有插件兼容
- 向后兼容HTML标签语法
- 支持容器嵌套（但建议避免过度嵌套）

### 性能基准测试
| 内容类型 | 渲染时间 | 内存使用 | 缓存支持 |
|---------|----------|----------|----------|
| 纯文本 | < 5ms | 最低 | N/A |
| 代码块 | 10-20ms | 中等 | ✅ |
| 数学公式 | 15-30ms | 中等 | ✅ |
| 表格 | 20-40ms | 较高 | 部分 |
| Mermaid | 50-200ms | 较高 | ✅ |
:::

## 🎨 样式定制

### 主题颜色
:::note 颜色方案
每种容器类型都有预定义的主题颜色：

- **note**: #2196F3 (蓝色) - 通用信息
- **tip**: #4CAF50 (绿色) - 积极建议  
- **warning**: #FF9800 (橙色) - 注意事项
- **danger**: #F44336 (红色) - 危险警告
- **info**: #2196F3 (蓝色) - 技术信息
:::

### 自定义样式
:::tip 样式定制建议
可以通过修改ContainerNode.CONTAINER_TYPES来自定义：
- 图标emoji
- 主题颜色
- 默认标题

也可以扩展支持新的容器类型。

#### 自定义容器类型示例
```kotlin
// 添加新的容器类型
ContainerNode.CONTAINER_TYPES["custom"] = ContainerConfig(
    icon = "🎯",
    colorRes = "#9C27B0",
    title = "自定义"
)
```
:::

---

*以上展示了自定义容器的全部功能，包括基础用法、复杂内容支持和技术实现。新的容器系统可以完美处理代码块、数学公式、表格、图表等各种复杂内容。*
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
    
    /**
     * 数学公式渲染测试用例
     */
    const val MATHEMATICAL_FORMULA_TEST_MARKDOWN = """
# 数学公式渲染测试

## 基本数学符号测试

### 行内公式
这是一个行内公式示例：质能方程 ${'$'}E = mc^2${'$'}，其中 ${'$'}E${'$'} 是能量，${'$'}m${'$'} 是质量，${'$'}c${'$'} 是光速。

另一个行内公式：勾股定理 ${'$'}a^2 + b^2 = c^2${'$'}，以及欧拉公式 ${'$'}e^{i\pi} + 1 = 0${'$'}。

### 块级公式

#### 基本运算
${'$'}${'$'}
\begin{align}
a + b &= c \\
a - b &= d \\
a \times b &= e \\
a \div b &= f
\end{align}
${'$'}${'$'}

#### 分数和根号
$$
\frac{a}{b} = \frac{c}{d} \quad \text{和} \quad \sqrt{a^2 + b^2} = c
$$

$$
\frac{x^2 + 2x + 1}{x - 1} = \frac{(x+1)^2}{x-1}
$$

#### 上下标
$$
x^2 + y^2 = r^2 \quad \text{和} \quad H_2O + NaCl
$$

$$
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
$$

## 高级数学公式

### 积分和导数
$$
\int_0^1 x^2 dx = \frac{1}{3}
$$

$$
\frac{d}{dx} \sin(x) = \cos(x)
$$

$$
\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}
$$

### 求和和乘积
$$
\sum_{k=1}^{n} k^2 = \frac{n(n+1)(2n+1)}{6}
$$

$$
\prod_{i=1}^{n} i = n!
$$

### 极限
$$
\lim_{x \to 0} \frac{\sin x}{x} = 1
$$

$$
\lim_{n \to \infty} \left(1 + \frac{1}{n}\right)^n = e
$$

## 矩阵和线性代数

### 矩阵表示
$$
A = \begin{pmatrix}
a_{11} & a_{12} & a_{13} \\
a_{21} & a_{22} & a_{23} \\
a_{31} & a_{32} & a_{33}
\end{pmatrix}
$$

### 行列式
$$
\det(A) = \begin{vmatrix}
a & b & c \\
d & e & f \\
g & h & i
\end{vmatrix} = a(ei - fh) - b(di - fg) + c(dh - eg)
$$

### 向量
$$
\vec{v} = \begin{pmatrix} x \\ y \\ z \end{pmatrix} \quad \text{和} \quad \vec{u} \cdot \vec{v} = |\vec{u}||\vec{v}|\cos\theta
$$

## 希腊字母和特殊符号

### 希腊字母
$$
\alpha, \beta, \gamma, \delta, \epsilon, \zeta, \eta, \theta, \iota, \kappa, \lambda, \mu
$$

$$
\nu, \xi, \omicron, \pi, \rho, \sigma, \tau, \upsilon, \phi, \chi, \psi, \omega
$$

$$
\Gamma, \Delta, \Theta, \Lambda, \Xi, \Pi, \Sigma, \Upsilon, \Phi, \Psi, \Omega
$$

### 特殊运算符
$$
\nabla \cdot \vec{F} = \frac{\partial F_x}{\partial x} + \frac{\partial F_y}{\partial y} + \frac{\partial F_z}{\partial z}
$$

$$
\forall x \in \mathbb{R}, \exists y \in \mathbb{R} \text{ such that } x + y = 0
$$

## 复杂公式组合

### 傅里叶变换
$$
F(\omega) = \int_{-\infty}^{\infty} f(t) e^{-i\omega t} dt
$$

### 薛定谔方程
$$
i\hbar \frac{\partial}{\partial t} \Psi(x,t) = \hat{H} \Psi(x,t)
$$

### 麦克斯韦方程组
$$
\begin{align}
\nabla \cdot \vec{E} &= \frac{\rho}{\epsilon_0} \\
\nabla \cdot \vec{B} &= 0 \\
\nabla \times \vec{E} &= -\frac{\partial \vec{B}}{\partial t} \\
\nabla \times \vec{B} &= \mu_0 \vec{J} + \mu_0 \epsilon_0 \frac{\partial \vec{E}}{\partial t}
\end{align}
$$

### 概率论
$$
P(A|B) = \frac{P(B|A)P(A)}{P(B)}
$$

$$
f(x) = \frac{1}{\sigma\sqrt{2\pi}} e^{-\frac{1}{2}\left(\frac{x-\mu}{\sigma}\right)^2}
$$

## 数学证明示例

### 欧拉恒等式推导
设 ${'$'}z = e^{i\theta}${'$'}，则：
$$
z = \cos\theta + i\sin\theta
$$

当 $\theta = \pi$ 时：
$$
e^{i\pi} = \cos\pi + i\sin\pi = -1 + 0i = -1
$$

因此：
$$
e^{i\pi} + 1 = 0
$$

### 微积分基本定理
如果 ${'$'}f${'$'} 在 ${'$'}[a,b]${'$'} 上连续，且 ${'$'}F'(x) = f(x)${'$'}，则：
$$
\int_a^b f(x) dx = F(b) - F(a)
$$

## 工程数学应用

### 控制系统
$$
G(s) = \frac{K}{s(s+1)(s+2)}
$$

$$
H(s) = \frac{1}{1 + G(s)}
$$

### 信号处理
$$
X(f) = \int_{-\infty}^{\infty} x(t) e^{-2\pi i f t} dt
$$

### 统计学
$$
\bar{x} = \frac{1}{n} \sum_{i=1}^{n} x_i
$$

$$
s^2 = \frac{1}{n-1} \sum_{i=1}^{n} (x_i - \bar{x})^2
$$

## 性能测试公式

### 算法复杂度
$$
T(n) = O(n \log n) \quad \text{快速排序平均时间复杂度}
$$

$$
S(n) = O(n) \quad \text{空间复杂度}
$$

### 渲染性能指标
$$
\text{FPS} = \frac{1}{\text{Frame Time}} \times 1000
$$

$$
\text{Memory Usage} = \frac{\text{Used Memory}}{\text{Total Memory}} \times 100\%
$$

---

*本测试用例包含了各种数学公式的渲染测试，用于验证 TurboMarkdown 的数学公式渲染能力。*
"""
    
    /**
     * 图片渲染测试用例
     */
    const val IMAGE_RENDERING_TEST_MARKDOWN = """
# 图片渲染测试

## 实际应用场景测试

### 应用推广场景

**1. 100+ Real-Life Scenarios — From Zero to Fluent!**
Structured from Level 0 to 2, making it easy to speak confidently in travel, work, or daily conversations!
![image1](https://popaife.s3-accelerate.amazonaws.com/other/talkingLime-2025-07-01-01.webp)

**2. Visual Vocabulary Learning + Instant Feedback**
Master essential vocabulary through realistic scenarios.
Tap any word to hear native pronunciation.Read aloud to reinforce memory and improve your pronunciation.
![image2](https://popaife.s3-accelerate.amazonaws.com/other/talkingLime-2025-07-01-02.webp)

**3. Contextual Phrase Training — Not Rote Memorization**
No more boring drills — learn real phrases in real conversations.
![image3](https://popaife.s3-accelerate.amazonaws.com/other/talkingLime-2025-07-01-03.webp)

🧪 **Try the new experience now**
and immerse yourself in speaking practice like never before!

---

## 图片格式兼容性测试

### WebP 格式
WebP 是 Google 开发的现代图片格式，具有更好的压缩率：
![WebP示例](https://example.com/sample.webp)

### PNG 格式
PNG 格式支持透明度，适合图标和简单图形：
![PNG示例](https://via.placeholder.com/400x300.png?text=PNG+Format+Test)

### JPEG 格式
JPEG 格式适合照片和复杂图像：
![JPEG示例](https://via.placeholder.com/400x300.jpg?text=JPEG+Format+Test)

### SVG 格式
SVG 是矢量图形格式，支持无损缩放：
![SVG示例](https://via.placeholder.com/400x300.svg?text=SVG+Format+Test)

## 不同尺寸图片测试

### 小尺寸图片 (100x100)
![小图片](https://via.placeholder.com/100x100?text=Small+Image)

### 中等尺寸图片 (400x300)
![中等图片](https://via.placeholder.com/400x300?text=Medium+Image)

### 大尺寸图片 (800x600)
![大图片](https://via.placeholder.com/800x600?text=Large+Image)

### 超宽图片 (1200x300)
![超宽图片](https://via.placeholder.com/1200x300?text=Ultra+Wide+Image)

### 超高图片 (300x1200)
![超高图片](https://via.placeholder.com/300x1200?text=Ultra+Tall+Image)

## 图片与文本混合布局测试

### 图片在段落中间
这是一段文本，用于测试图片与文本的混合布局效果。

![中间图片](https://via.placeholder.com/400x200?text=Middle+Image)

这是图片后面的文本，用于验证图片渲染后的文本布局是否正确。

### 连续多张图片
测试连续多张图片的渲染效果：

![图片1](https://via.placeholder.com/300x200?text=Image+1)
![图片2](https://via.placeholder.com/300x200?text=Image+2)
![图片3](https://via.placeholder.com/300x200?text=Image+3)

### 图片与列表结合
- 列表项目1
  ![列表图片1](https://via.placeholder.com/250x150?text=List+Image+1)
- 列表项目2
  ![列表图片2](https://via.placeholder.com/250x150?text=List+Image+2)
- 列表项目3
  ![列表图片3](https://via.placeholder.com/250x150?text=List+Image+3)

## Alt 文本测试

### 有Alt文本的图片
![这是一个带有alt文本的示例图片](https://via.placeholder.com/400x300?text=With+Alt+Text)

### 无Alt文本的图片
![](https://via.placeholder.com/400x300?text=No+Alt+Text)

### 长Alt文本的图片
![这是一个非常长的alt文本描述，用于测试当图片无法加载时是否能正确显示这个很长的描述文本，包含中文和英文mixed content](https://via.placeholder.com/400x300?text=Long+Alt+Text)

## 错误处理测试

### 无效URL图片
![无效URL](https://invalid-url.com/nonexistent-image.jpg)

### 网络错误图片
![网络错误](https://httpstat.us/404.jpg)

### 超时图片
![超时图片](https://httpstat.us/408.jpg)

## 特殊字符URL测试

### 包含空格的URL
![空格URL](https://via.placeholder.com/400x300?text=URL+with+spaces)

### 包含中文的URL
![中文URL](https://via.placeholder.com/400x300?text=中文+URL)

### 包含特殊符号的URL
![特殊符号URL](https://via.placeholder.com/400x300?text=Special+%26+Symbols)

## 性能测试场景

### 高分辨率图片
![高分辨率](https://via.placeholder.com/2048x1536?text=High+Resolution+Image)

### Base64 编码图片
![Base64图片](data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iIzMzNzNkYyIvPgogIDx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmaWxsPSJ3aGl0ZSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iMC4zZW0iPkJhc2U2NCBJbWFnZTwvdGV4dD4KPC9zdmc+)

### 动态生成图片
![动态图片](https://picsum.photos/400/300?random=1)

## 图片标题和链接测试

### 带标题的图片
![示例图片](https://via.placeholder.com/400x300?text=Image+with+Title "这是图片标题")

### 可点击的图片
[![可点击图片](https://via.placeholder.com/400x300?text=Clickable+Image)](https://example.com)

### 带链接和标题的图片
[![链接图片](https://via.placeholder.com/400x300?text=Linked+Image+with+Title "点击访问示例网站")](https://example.com)

## 图片加载性能监控

### 测试指标
- **图片加载时间**: 从发起请求到图片完全加载的时间
- **内存使用**: 图片解码后占用的内存大小
- **缓存命中率**: 重复加载相同图片时的缓存使用情况
- **网络使用**: 图片下载的网络流量统计

### 性能优化策略
1. **图片预加载**: 提前加载可能需要的图片
2. **懒加载**: 仅在图片进入视口时才开始加载
3. **尺寸适配**: 根据显示尺寸加载合适分辨率的图片
4. **格式选择**: 根据图片内容选择最佳格式
5. **压缩优化**: 在保证质量的前提下减少文件大小

## 响应式图片测试

### 不同密度屏幕适配
在不同DPI的设备上，图片应该自动选择合适的分辨率：
![响应式图片](https://via.placeholder.com/400x300?text=Responsive+Image)

### 暗黑模式适配
在暗黑模式下，某些图片可能需要特殊处理：
![暗黑模式图片](https://via.placeholder.com/400x300/333333/ffffff?text=Dark+Mode+Image)

---

*本测试用例包含了各种图片渲染场景，用于验证 TurboMarkdown 的图片加载和渲染能力，包括性能优化和错误处理。*
"""

    /**
     * 自定义标签测试用例的完整 Markdown 内容
     */
    const val CUSTOM_TAGS_TEST_MARKDOWN = """
# 🏷️ 自定义标签演示

这是 **TurboMarkdown** 的自定义标签功能演示，基于 Markwon 4.6.2 的 HtmlPlugin 机制实现。

## 📋 支持的标签类型

### 🎨 样式标签 (CustomStyleTagHandler)

#### 状态标签
- <warn>警告信息</warn> - 橙色警告样式
- <info>信息提示</info> - 蓝色信息样式  
- <success>成功消息</success> - 绿色成功样式
- <error>错误信息</error> - 红色错误样式
- <danger>危险警告</danger> - 红色危险样式

#### 强调标签
- <highlight>高亮文本</highlight> - 黄色背景高亮
- <mark>标记文本</mark> - 黄色背景标记

#### 尺寸标签
- 正常大小文本 <small>小号文本</small> 正常大小文本
- 正常大小文本 <large>大号文本</large> 正常大小文本

#### 主题标签
- <primary>主色调文本</primary> - 蓝色主题样式
- <secondary>次要文本</secondary> - 灰色次要样式

### ✨ 装饰标签 (CustomTextDecorationTagHandler)

#### 装饰效果
- <u>下划线文本</u> - 添加下划线
- <s>删除线文本</s> - 添加删除线  
- H<sub>2</sub>O - 下标效果
- E=mc<sup>2</sup> - 上标效果

## 🔄 嵌套使用示例

### 标签嵌套
<warn>这是一个 <u>带下划线</u> 的警告信息</warn>

<success>成功消息中的 <small>小号文本</small> 和 <large>大号文本</large></success>

<info>信息中包含 <highlight>高亮文本</highlight> 和 <mark>标记文本</mark></info>

### 与 Markdown 语法混合
<primary>**粗体主色调文本**</primary>

<secondary>*斜体次要文本*</secondary>

<error>`代码样式的错误信息`</error>

## 📊 技术实现

### 实现原理
1. **继承 TagHandler**：实现 Markwon 的 TagHandler 接口
2. **支持的标签**：通过 `supportedTags()` 方法声明
3. **样式处理**：在 `handle()` 方法中应用相应的 Span
4. **插件注册**：通过 HtmlPlugin 注册到 Markwon 实例

### 支持的所有标签
**样式标签**：`warn`, `info`, `success`, `error`, `danger`, `highlight`, `mark`, `small`, `large`, `primary`, `secondary`

**装饰标签**：`u`, `s`, `sub`, `sup`

### 使用方法
```kotlin
// 创建带有自定义标签支持的 Markwon 实例
val markwon = Markwon.builder(context)
    .usePlugin(CustomTagPlugin.create())
    .build()

// 渲染包含自定义标签的 Markdown
val markdown = "<warn>这是警告信息</warn>"
markwon.setMarkdown(textView, markdown)
```

## 🧪 测试用例

### 基础功能测试
<warn>警告</warn> <info>信息</info> <success>成功</success> <error>错误</error>

### 样式组合测试  
<primary><large>大号主色调</large></primary> <secondary><small>小号次要文本</small></secondary>

### 复杂嵌套测试
<highlight>高亮文本中的 <u>下划线</u> 和 <s>删除线</s></highlight>

### 科学公式测试
水的化学式：H<sub>2</sub>O  
爱因斯坦质能方程：E=mc<sup>2</sup>

---

*本演示展示了 TurboMarkdown 自定义标签的所有功能，包括样式应用、嵌套使用和与标准 Markdown 语法的兼容性。*
"""
} 