# 04 · 代码规范

> 本规范只约束"怎么写得让 AI 和队友都能看懂、不跑偏"，不追求风格洁癖。核心目标：领域层纯 Kotlin 可测、单向数据流不被破坏、内容不进代码。

## 1. 命名

| 对象 | 规则 | 示例 |
| --- | --- | --- |
| 包 | 全小写，无缩写 | `com.rainingyesterday.domain.engine` |
| 类 / 接口 | PascalCase，接口不加 `I` 前缀 | `ContentRepository` |
| 数据类 | PascalCase，名词 | `Place`, `WorldState` |
| 函数 | camelCase，动词开头 | `evaluateCondition()` |
| 枚举 | PascalCase，值小写或 C 式 | `PlaceType.BUILDING` |
| 常量 | UPPER_SNAKE | `DEFAULT_RADIUS_M` |
| JSON id | 全小写蛇形 | `old_hall`, `rain_mushroom` |

## 2. Kotlin 语言规范

### 2.1 不可变优先

- 状态一律 `val`，能 `data class` 就用 `data class`；模型对象用 `copy()` 产生新实例，原地可变对象只在明确的局部作用域。
- 领域层产出的 `GameState` / `WorldState` 必须是**不可变快照**。

### 2.2 空安全

- 优先 `sealed class` + 明确空值语义，`!!` 仅用于"逻辑上不可能为 null"的场景，并写注释说明依据。
- 不返回"魔法 null"，缺省用 `List` 空集合，不用 `null` 表示"没有"。

### 2.3 集合与作用域

- 集合类型写不可变接口 `List` / `Map`（实现可能是可变，但对外只暴露只读）。
- 优先级：`sealed class` > `enum` > string/tag 魔法值；`02` 里的枚举必须落到 `domain/model` 的 Kotlin 枚举。

## 3. Compose UI 规范

### 3.1 无状态组合 + 状态提升

- Composable 分两类：**无状态组件**（参数 + 回调，不持有业务状态）与**有状态容器**（接 ViewModel）。
- 状态下沉：能下沉就下沉到最小的作用域；`remember` 只放"纯 UI 状态"（如某个开关的展开态），业务状态一律 Up 到 ViewModel。

```kotlin
// 好：无状态组件
@Composable
fun PlaceMarker(place: Place, onClick: (String) -> Unit) { ... }

// 坏：Composable 里直接读仓库/改存档
```

### 3.2 单向数据流（`01` 已定，这里强调落地）

```text
Composable 回调 → onXxx(intent) → ViewModel → 领域引擎 → StateFlow<UiState> → collectAsState → 渲染
```

- 每个屏幕一个 ViewModel，暴露 `StateFlow<UiState>`，UI 通过 `collectAsStateWithLifecycle()` 订阅。
- UI **绝不**直接写领域状态；ViewModel 也不碰 Room / 地图 SDK，只通过仓库接口 / 领域引擎。

### 3.3 主题与组件

- 颜色、字体、尺寸进 `ui/theme` 设计 token，禁止散落魔法值。
- 通用组件放 `ui/components`，被两个以上屏幕复用才下沉。

## 4. 数据层规范

### 4.1 仓库接口（domain 定义，data 实现）

- 接口在 `domain/repository` 定义，实现在 `data`。领域层只认接口，不认 Room / JSON 细节。

```kotlin
interface ContentRepository {
    fun places(): List<Place>
    fun place(id: String): Place?
}
```

### 4.2 JSON 内容

- 一律用 `02` 的 schema；新增字段先改 `02` 再改代码，`ignoreUnknownKeys=true` 保证兼容。
- 解析出的模型对象与 Runtime 校验分离：内容载入后做一次**合法性校验**（id 唯一、类型合法、引用存在），坏数据在启动时显式报错，不默默吞掉。

### 4.3 Room

- Entity 的 `refId` 指向内容 JSON 的 `id`，**不复制**内容字段（`02` 已定）。
- 增删改走 Dao 的 `suspend` 函数 + 事务标注。

## 5. 错误处理

- 内容解析失败：启动时报错并给出可读信息（哪个主题包、哪个文件、哪行）。
- 外部 API（天气/LLM/网络）：一律 `try/catch` 兜底到本地，**外部失败绝不中断游戏**。
- 定位权限被拒：优雅降级到"手动点击模拟位置"模式，不崩、不卡引导。

## 6. 内容生产规范（写给设计/录入）

- 一个内容类型对应一个 JSON 文件（`places.json`、`npcs.json` …），文件名见 `02` 主题 `files` 清单。
- id 全局唯一、蛇形、可读；跨文件引用一律用 id（`"place": "old_hall"`）。
- `history` 数组索引即解锁层级，按"表层 → 深层"顺序填写。
- `anomaly` 任务必须带 `unlock`；`rumor` 允许 `may_fail=true`（扑空是合法结果，不要为了"必有收获"而破坏规则）。

## 7. Git 提交

- 提交信息用祈使句 + 范围前缀：`feat(engine): 触发引擎支持 time_range 条件`。
- 优先按文件/模块小步提交，避免"一大坨"。
- 不提交密钥、`google-services.json`、玩家存档与生成产物。

## 8. 反模式清单（不要做）

| 不要 | 原因 |
| --- | --- |
| 在代码里硬编码内容（地点名、NPC 台词、配方） | 破坏数据驱动 |
| 把世界内容写进 Room | 归档与内容混淆，`02` 已分层 |
| 领域层 import Android / Compose / 地图 SDK | 破坏纯 Kotlin 可测性 |
| UI 直接改 `GameState` | 破坏单向数据流 |
| JSON 字段用中文 key / 大驼峰 | 解析与可读性成本 |
| 为"将来可能"引入抽象层 | 违背"克制"原则，见 `00` |