# App 端读经 / 抄经逻辑设计

> 基于 `E:\Android\Buddha` 现有代码（2026-10-22 实测），不是从零设计。
> 技术栈：Kotlin 1.9.24 / minSdk 24 / targetSdk 35 / XML + viewBinding / TopMVVM / Retrofit(suspend) + LiveData。
> 无 Compose、无 Hilt；`libconfig.gradle` 已声明 `room_version 2.6.1` 但**尚未接入**。

---

## 0. 现状盘点

| 位置 | 现状 | 处置 |
|---|---|---|
| `FojingReadFragment` | 只有一个 `Banner`（banner_infinite_loop=false），数据是 5 个空字符串 | **换 ViewPager2**，接章节数据源（Banner 是轮播控件，撑不住 557 章） |
| `FojingCopyFragment` | RecyclerView + GridLayoutManager，列数 = (宽-32dp)/40dp，行高 85dp，格子 40dp 宽；底部 tv_answer1~4 四个 56sp 方块 | 格子算法保留；**RecyclerView 换成自绘**（手写用 RecyclerView 会因 item 回收丢笔迹） |
| `FojingDetailsFragment` | 已有 `tv_read`「已阅读 x%」`tv_copy`「已抄写 x%」，点击 `start(FojingReadFragment())`——**没传 bookId** | 改为传 bookId + 起始 seq |
| `BuddhaBook` | 有 `bookUrl`（txt 下载地址）、`readPercent`、`copyPercent`、`viewCount`、`copyCount` | `bookUrl` **废弃**；两个 percent 改为服务端计算回填 |
| `BookshelfVM` | 只有列表/专题/书架/评论/点赞，**没有正文、没有章节、没有进度** | 新增 `ReadVM` / `CopyVM`，不要塞进 BookshelfVM |
| `ApiManager` | 全部 `@GET` + `session` query，无章节/正文/进度接口 | 新增 4 个接口（见 §5） |
| 本地存储 | 无 Room、无 SQLite，纯内存 + 网络 | 接入 Room，**本地库作为唯一渲染数据源** |

**结论：目录表 + 正文表在服务端已就绪，App 侧真正的缺口是「章节接口 + 本地缓存 + 两套进度模型」。**

---

## 1. 分层

```
Fragment（只管渲染与手势）
  ↕ observe LiveData / StateFlow
VM  ReadVM / CopyVM（各自一套状态机，互不复用）
  ↕
Repository（BookRepository）
  ├─ 本地 Room：唯一数据源，UI 只读这里
  ├─ 网络：拉章节、上报进度（失败进 outbox）
  └─ 进度合并：读经取最新，抄经取累加
```

**铁律：UI 不直接读网络返回。** 网络结果先落 Room，再由 Room 的 Flow 推到 UI。
理由：佛经是强离线场景（地铁、寺院、飞机），且进度必须在断网时不丢；两套渲染路径（在线/离线）后期一定会分叉出 bug。

### Room 表（5 张）

```kotlin
@Entity(tableName = "book_chapter", primaryKeys = ["bookId", "seq"])
data class ChapterEntity(
    val bookId: Int, val seq: Int,
    val juanNo: Int, val juanTitle: String,
    val sectionKind: String,          // 品/序/正文/附
    val sectionTitle: String,
    val content: String,              // 简体，原样存
    val contentTrad: String?,
    val charCount: Int,
    val charStart: Int, val charEnd: Int,
    val contentHash: String,
    val cachedAt: Long
)

@Entity(tableName = "read_progress")
data class ReadProgressEntity(
    @PrimaryKey val bookId: Int,
    val seq: Int, val charOffset: Int,
    val percentPermille: Int,          // 千分比，避免浮点
    val updatedAt: Long, val dirty: Boolean
)

@Entity(tableName = "copy_progress")
data class CopyProgressEntity(
    @PrimaryKey val bookId: Int,
    val seq: Int, val charOffset: Int, // 单调游标：抄到哪个字
    val copiedChars: Long,             // 累计去重字数
    val totalChars: Int,
    val skippedChars: Int,             // 跳过的格数
    val updatedAt: Long, val dirty: Boolean
)

@Entity(tableName = "copy_session")
data class CopySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int, val seq: Int,
    val startOffset: Int, val endOffset: Int,
    val chars: Int, val durationMs: Long,
    val createdAt: Long, val synced: Boolean
)
// 第 5 张：chapter_download（整部经离线包状态），M2 再加
```

`charOffset` 语义统一为 **章内字符偏移**（`String` 下标，UTF-16），与 `content` 直接对应，不做汉字序换算。
只有抄经格子和注音弹窗需要**汉字序**（跳过标点空白），换算在客户端现算（一章 < 1 万字，< 0.1ms）。
**不要把汉字序存进库**——一套偏移已经够乱，两套必错。

---

## 2. 读经

### 2.1 进入流程

```
详情页「阅读」→ ReadVM(bookId)
  ├─ 本地有目录？ → 无则拉 /book/chapter/list（一次拿全书目录+hash，约 75KB/557章）
  ├─ 读 read_progress → 恢复位置（无则从 seq=1 开始）
  ├─ 拉当前章：本地命中且 hash 一致 → 直接用；不一致 → 后台重拉并替换
  ├─ 预取下一章（翻页零等待）
  └─ 渲染 + 每 1s 节流写进度
```

**目录与正文分开拉**，这是之前定的：目录 75KB 一次拉完，正文按章。不要做「整部经一次拉完」——《华严经》62 万字会直接把 App 拖死。

### 2.2 分页：第一版不要静态分页

佛经章节长度极不均衡：心经 260 字（1 屏），华严单章可能 2 万字（50 屏）。两种方案：

| 方案 | 做法 | 代价 |
|---|---|---|
| A 章内滚动 + 左右翻章 | ViewPager2，每页 = 一章的可滚动 TextView，左右滑切章 | 无分页计算，字号/行距随时变；但「翻页感」弱 |
| B 章内静态分页 | StaticLayout 按宽高切行，算出该章页数，ViewPager 管全局页序列 | 翻页感强；但字号改动要重算全部页，章边界拼接复杂 |

**建议第一版走 A**，因为用户 90% 的时间在读心经/金刚经/地藏经这几部，单章都不长，滚动体验可接受；上 B 的收益不抵工程量。等有了阅读时长数据再决定。

若后期上 B：Adapter 数据源必须是**跨章扁平的页序列**（`(seq, pageIndex)`），不能一章一页，否则章边界翻页会跳。

### 2.3 进度模型

- 采集：滚动停止 300ms 后，取可见首行的字符 index → `charOffset`
- 写入：1s 节流 → Room（`dirty=true`）
- 上报：`WorkManager` 每 30s 或有网时批量推，成功后清 dirty
- 冲突：**取 updatedAt 最新的一份**（读经是单值覆盖语义，多设备只保留最后位置）
- `percentPermille` = (前 seq-1 章 charCount 之和 + charOffset) / 全书 charCount。全书 charCount 取自目录接口汇总，客户端可算

**翻页即写、退出必写**：`onStop` 和 `onDestroyView` 都要 flush 一次，否则用户杀了进程进度就丢。

### 2.4 目录

树形三级：**卷 → 品 → 章（part）**。`sectionKind` 决定呈现方式：
- `品`：作为分组标题，可折叠
- `序` / `正文` / `附`：直接作为可点条目

华严 557 章必须做卷级折叠，否则目录页滑动体验极差。用 `RecyclerView` + 两级 expandable 即可，不必上第三方库。

---

## 3. 抄经

### 3.1 与读经的本质差异

| | 读经 | 抄经 |
|---|---|---|
| 位置语义 | 单值覆盖（读到哪） | 单调游标（抄到哪）+ 累加（抄了多少） |
| 写频率 | 1s 节流 | 每 20 字或 5s，`onStop` 强制 flush |
| 会话 | 无 | 有（时长、字数、打卡） |
| 冲突合并 | 取 updatedAt 最新 | 游标取 max，累加取 sum |
| 渲染 | 文本流 | 逐格位图/笔迹 |

**不要合并成一个 VM 或一张进度表。** 读经每次翻页都写、抄经是低频大写入，混在一起会让读经的高频写不断碰到抄经的累计字段。

### 3.2 格子模型：换成自绘

现有实现是 `GridLayoutManager` + `FojingCopyAdapter`，每格一个 item（40dp 宽 × 80dp 高，上下两条横线 + 左右竖线）。**做只读展示没问题，做手写一定出问题**：

1. RecyclerView 回收 item 时，手写 View 的笔迹会丢（除非每格单独存 Bitmap，内存翻倍）
2. 连续书写时手指会划过多个格子，需要跨 item 的手势分发，`RecyclerView` 的拦截机制会打架
3. 竖排要从右往左、列内从上往下，`GridLayoutManager` 是行优先填充，`reverseLayout` 只能整体翻转，做不出「列优先 + 右起」

**建议：整屏一个自绘 View（`CopyPaperView : View`）。**

```
onDraw:
  ├─ drawGrid()   // 竖线、横线、田字格，一次画完
  ├─ drawModel()  // 每格顶部：淡色范字（描红一档）；底部：用户笔迹
  └─ drawCursor() // 当前书写格高亮
onTouchEvent:
  ├─ ACTION_DOWN → 定位命中格（x/列宽, y/行高）
  ├─ ACTION_MOVE → 追加点到当前字的 Path（只允许在本格内）
  └─ ACTION_UP   → 该字 DONE，游标前进，若跨格则结算上一字
```

列宽/行高沿用现有算法（`(width - 32dp) / 40dp`、`(height - 30dp) / 85dp`），保证视觉与已完成的 UI 稿一致。

### 3.3 字序映射

```
格 index  ←→  汉字序 n  ←→  content 的 charOffset
```

- 预处理：进章时扫一遍 `content`，生成 `IntArray`：`hanziIndex → charOffset`（跳过标点、空白、换行）
- 一格 = 一个汉字，标点不占格（这是传统写经的规矩，也避免「抄不抄标点」的口径争议）
- 弹读音、算进度、恢复断点全部走这张表，**不重复实现三套换算**

### 3.4 抄经纸的三种模式（第一版只做 A）

| 模式 | 做法 | 识别成本 | 建议 |
|---|---|---|---|
| A 描红 | 格内顶部淡色范字，用户在下方写 | 无 | **第一版** |
| B 空白临写 | 格内空白，用户凭记忆写 | 无（不校验） | 第二版 |
| C 手写识别 | ML Kit Digital Ink 判断对错 | 高，佛经生僻字识别率极差 | **不做** |

C 不做的理由是硬的：`㝹`「㮈」这类字连印刷体都难认，模型训练数据里几乎没有，识别错误会打击用户。抄经本身是修行，不是考试。

### 3.5 状态机

```
IDLE → 进入页面 → RESUME(有断点则定位)
  ↓ 开始书写
WRITING(当前格)
  ├─ 抬笔 → 该字 DONE → 游标+1 → 若到格末 → 翻页(保持同一章的连续字序)
  ├─ 点「跳过」→ SKIPPED → 游标+1，skippedChars+1
  ├─ 点「撤销」→ 回退一字，重绘
  └─ onStop / 切后台 → flush 到 Room + 结算 session
DONE(整章抄完) → 弹完成页（字数/时长）→ 回详情页
```

**关键：翻页不等于换章。** 一章可能 20 屏，翻屏只是移动 paper 的起始 index；只有跨 `seq` 才换数据源。

### 3.6 进度：顺序抄写让「完成度」退化成游标

这是最省事的一点：用户从头顺序抄，已抄范围天然是 `[0, charOffset)` 一个连续区间，**不需要区间合并**。

- `copiedChars`：累计去重字数 = 各章已抄前缀之和
- `percentPermille` = (前 seq-1 章 charCount + 本章已抄字数) / 全书 charCount
- 允许跳章抄（用户可能只想抄《普门品》）：那么 `copy_progress` 要按 (bookId, seq) 存多行，`percentPermille` 用「已完成章字数 + 各章已抄字数」求和

**重复抄同一段算不算进度**：不算完成度，但算 `copy_session.chars`（功德计数）。两个指标分开，跟之前定的一致。

### 3.7 落库与上报节奏

| 动作 | 时机 |
|---|---|
| 笔迹落盘 | 每 20 字 或 每 5 秒，`onStop` 强制 |
| 进度落库 | 与笔迹同批 |
| 进度上报 | 每章抄完 / 退出页面 / 每日一次兜底（WorkManager） |
| session 结算 | 进入记 startTime，退出算 durationMs，一次写库 |

**不要每写一个字就写库。** 抄经时落笔频率是每秒 1-2 次，SQLite 单条写虽然快，但会持续占用 IO 线程并触发 WAL checkpoint，体感是笔迹延迟。

---

## 4. 两条线的分界（一句话版）

- **读经是「定位问题」**：一个游标 + 一次覆盖写。
- **抄经是「记账问题」**：一个单调游标 + 一串事件流水。
- 共用：`book_chapter` 表、Repository、章节接口、读音库、生僻字表。
- 不共用：VM、进度表、渲染方式、写库节奏、冲突策略。

---

## 5. 需新增的接口（4 个）

> **2026-10-22 已落地，实际路径和字段与下面这版不同**：接口挂在 `BookController`，
> 名字是 `getBookChapterList` / `getBookChapter` / `getBookProgress` / `updateReadProgress` / `updateCopyProgress`，
> 进度表用的是现有 `book_reach_record`（加列），不是新建表。
> 以 `_接口与M1改动.md` 为准，本节只保留设计初衷。

```
GET  /book/chapter/list?bookId=&session=
     → [{seq, juanNo, juanTitle, sectionKind, sectionTitle, charCount, contentHash}]
     // 557 章约 75KB，一次拉完，可缓存 24h

GET  /book/chapter?bookId=&seq=&session=
     → {seq, content, contentTrad, charStart, charEnd, charCount, contentHash}
     // 客户端带 contentHash，服务端可返回 304 语义（同 hash 则 body 为空）

POST /book/read/progress
     {bookId, seq, charOffset, percentPermille}
     → {ok, serverPercent}

POST /book/copy/progress
     {bookId, seq, charOffset, chars, durationMs, skippedChars}
     → {ok, copiedChars, percentPermille}
```

两点提醒：
1. 现有接口全是 `@GET` + `session` query 参数。进度上报建议用 `POST + JSON body`，别为了「风格统一」把三次抄经的批量数据塞进 query string。
2. `BuddhaBook.bookUrl`（txt 地址）要从接口里下掉，否则新旧两套并存，后面一定有人误用。

---

## 6. 里程碑

| 阶段 | 内容 | 验收 |
|---|---|---|
| M1 | 章节接口 + Room + 目录，阅读页能按章翻 | 打开心经/华严都能翻，断网可读已缓存章 |
| M2 | 读经进度、样式设置、繁简切换、整部下载 | 杀进程后位置不丢；换设备进度同步 |
| M3 | 抄经自绘格子 + 逐字状态 + 断点续抄 | 抄 100 字退出再进，笔迹与游标都还原 |
| M4 | 撤销、跳过、会话统计、进度上报 | 抄完一章弹结算页，数据上报成功 |
| M5 | 读音库接入（点击弹窗）+ 生僻字字体替换 | 点「耨」出 nòu + 音频 |

M1 是硬前提，M3 可以并行开，因为抄经的数据依赖只有 `chapter.content`。

---

## 7. 风险清单

1. **Banner 库做数据翻页**：`com.youth.banner` 是轮播控件，动态 500+ 页会一次性绑定多个 View，内存与滑动都不稳。必须换 `ViewPager2`。
2. **RecyclerView 做手写**：item 回收导致笔迹丢失，这是必踩的坑，直接自绘绕开。
3. **竖排右起**：若确定走竖排，`GridLayoutManager` 做不出来，自绘时按「列优先、x 递减」算坐标即可。
4. **生僻字豆腐块**：`㝹`「㮈」等 112 种扩展区字系统字体无字形，必须内置字体或替换表，否则用户看到 `□`。
5. **字数口径**：`charCount` 是否含标点决定所有百分比。之前定的是「含标点含换行，全角标点算一字」，客户端算进度时不要另立一套。
6. **咒语不注音**：大悲咒、楞严咒整段不标现代拼音，配诵读音频。
7. **全量离线 29.8MB**：整部下载要分卷、可断点、可清理，别默默吃用户空间。
