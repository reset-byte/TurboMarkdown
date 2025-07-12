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
     * 综合表格测试用例
     */
    val COMPREHENSIVE_TABLE_TEST_MARKDOWN = """
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
    val MATHEMATICAL_FORMULA_TEST_MARKDOWN = """
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
    val IMAGE_RENDERING_TEST_MARKDOWN = """
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
} 