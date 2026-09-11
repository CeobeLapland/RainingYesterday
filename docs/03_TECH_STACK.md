# 03 · 技术选型与构建

> 目标：给课设一个"毕业能跑、答辩能演示、后续能扩展"的技术栈。原则是少而稳，不为炫技引入多余依赖，每项都服务于架构（`01`）与数据（`02`）里已经定下的规则。

## 1. 选型总表

| 关注点 | 选择 | 理由 |
| --- | --- | --- |
| 语言 | Kotlin | 与 Compose 同生态，空安全、协程 |
| UI | Jetpack Compose | 声明式，契合单向数据流 |
| 架构组件 | ViewModel + StateFlow | UI 状态管理标准做法 |
| 依赖注入 | Hilt | 官方推荐，降低手工装配成本 |
| 本地存档 | Room | 结构化玩家存档 + 版本迁移 |
| 内容存储 | JSON 文件 + kotlinx.serialization | 数据驱动，跨端复用 |
| 地图 | osmdroid（主）+ MapLibre（备选） | 开源瓦片、免 key、离线友好 |
| 定位 | Google FusedLocation / Android Location | 可配置精度与频率 |
| 相机/图片 | CameraX + Photo Picker | 记忆节点存图 + 实拍识别（实时预览，需 CAMERA 权限） |
| 图片加载 | Coil | Compose 原生支持 |
| 网络（后补） | Retrofit + OkHttp | REST 轻社交、云存档 |
| 真实信号 | 可选外部源 + 本地模拟兜底 | 真实天气/季节/校园信号（默认可关） |
| AR（P2） | ARCore 主验证 + CameraX/IMU 兜底 | MVP 不引入，后期"AR 观察窗"验证 |
| 测试 | JUnit + kotlinx-coroutines-test + Turbine | 领域层纯 Kotlin 可测 |

## 2. 语言与平台

```text
Kotlin 2.x（K2 编译器）
minSdk 26（Android 8.0）
targetSdk / compileSdk 35
Gradle Kotlin DSL + Version Catalog（libs.versions.toml）
```

minSdk 26 足以覆盖 CameraX、Photo Picker（部分回退）、后台定位，能稳定覆盖绝大多数校园内测试机。

## 3. 关键依赖清单

```toml
[versions]
kotlin = "2.0.x"
composeBom = "2025.xx.xx"

[libraries]
# UI
compose-bom = ...
compose-material3 = ...
compose-navigation = ...
coil-compose = ...

# 架构
hilt-android = "2.5x"
hilt-navigation-compose = ...
lifecycle-viewmodel-compose = ...
lifecycle-runtime-compose = ...

# 数据
room-runtime / room-ktx / room-compiler = "2.6.x"
kotlinx-serialization-json = "1.7.x"

# 地图 / 定位
osmdroid-android = "6.1.x"
play-services-location = "21.x"

# 相机
camera-core / camera-camera2 / camera-lifecycle / camera-view

# 网络（后补，先加依赖不写逻辑）
retrofit = "2.x"
okhttp = "4.x"

# 真实信号（可选，默认本地模拟；数据源由 SignalSource 封装，不锁定供应商）
# 走 retrofit，无需额外依赖

# AR（P2 占位，MVP 不引入；arcore-core / sceneview 待验证阶段再定）

# 测试
junit / kotlinx-coroutines-test / turbine / compose-ui-test
```

> 具体版本号以 `libs.versions.toml` 落定时为准；本表只锁定"用什么"，不锁死小版本。

## 4. 地图与定位（最高不确定性，详述）

### 4.1 地图 SDK 选型

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| **osmdroid** | 纯 OSM 瓦片、免 key、WGS84 原生（与真实坐标一致）、离线缓存成熟 | UI 是 View 体系，需 `AndroidView` 包进 Compose | **主选** |
| MapLibre Native | 矢量瓦片、渲染现代、样式可控 | 接入复杂度更高、矢量瓦片源需自建 | 备选 |
| Google Maps | 生态好 | 大陆需 key / 合规问题、需 GCJ 转换 | 不用 |
| 高德 / 百度 | 国内贴合 | 卫星图非 WGS84，叠加真实坐标层要转 GCJ-02 | 不用 |

选择 osmdroid 的核心原因：本项目是"真实坐标 + 虚拟叠加层"，地图必须用 WGS84 直连定位坐标，不引入国密坐标偏移的坑；且免 key、可离线，课设演示无网也能跑。

### 4.2 叠加层（Compose 自绘）

地图瓦片只作底图，**上面的一切（六边形迷雾、地点标记、足迹、传闻、异常、他人足迹）都是 Compose Canvas 自绘层**，不依赖地图 SDK 的叠加能力。这保证底图可换（osmdroid → MapLibre → 纯离线矢量图），业务层不受影响。

### 4.3 定位策略（玩家可配置）

```text
PlayerSettings.locationMode:
- disable   : 不定位，用地图上手动点击模拟位置（演示/开发用）
- foreground : 前台定位，低耗（默认）
- background : 后台轨迹记录（需额外权限与说明，后补）
```

定位频率、最小位移、精度优先级全部进 `PlayerSettings`，与功耗直接挂钩，做成可配置项而非硬编码。

### 4.4 AR 双轨（P2，MVP 只留接口不引入 SDK）

GDD v2 决定两条路线都验证，不因演示限制而只走低配：

| 路线 | 能力 | 代价 | 结论 |
| --- | --- | --- | --- |
| **ARCore** | 平面检测、设备相对锚点、光照估计 | 依赖 Google Play Services for AR + 设备白名单 | 主验证 |
| **CameraX + IMU 伪 AR** | 相机预览 + 陀螺仪/磁力计 + 透视投影叠加 | 无平面检测、无稳定锚点、会漂移 | 兜底验证，用 2D billboard 补偿 |

两条路线本质都只改表现层视图，共享同一份 `DerivedState.arVisible` 输入（`01` §4.3）。后期各做一个"AR 观察窗"最小验证点，实测设备覆盖与漂移后再定主方案。**MVP 阶段不添加 AR 依赖。**

## 5. 内容层：JSON 的解析约定

- 用 **kotlinx.serialization** 解析 `02_DATA_MODEL.md` 里的全部 JSON。
- 解析器配置 **`ignoreUnknownKeys = true`**：JSON 多加字段不崩，向后兼容。
- 可选字段给默认值，避免旧主题包升级后解析失败。
- 内容文件放 `assets/content/<theme_id>/`，随包发布；`ContentRepository` 首次启动解析并缓存到内存。
- 热更新（后补）：从远端拉新 JSON 覆盖缓存，签名校验后再生效。

## 6. 真实信号与 LLM API（可选，默认关闭）

| 能力 | 默认 | 说明 |
| --- | --- | --- |
| 真实天气 | **本地模拟兜底** | 优先接真实天气（`SignalSource`/`WeatherSource` 真实实现），失败降级到按季节 + 随机生成 |
| 季节日照 | **本地推算** | 日出日落按真实日期/纬度本地计算，无需外部 API |
| 校园信号 | **人工配置兜底** | 社团/档口/施工等动态，第一版人工维护配置注入，接口留好 |
| NPC LLM | **预设对话兜底** | `npc.api` 默认 `enabled:false`；玩家填入 key 后才走外部接口，同一 schema 双轨 |
| 轻社交后端 | **关闭** | `PlayerSettings.showOthers=false` 时完全本地，无任何网络请求 |

这类外部能力都遵循同一原则：**先做本地可玩的兜底，外部源是可选增强通道（真实优先、模拟兜底）**，避免课设被第三方 key / 配额卡死。

## 7. 测试

- 领域层（`domain/engine`）是纯 Kotlin，**必须**可独立单测，不依赖 Android、地图、Room。
- 触发引擎用 JUnit + 参数化用例覆盖各种 `Condition` 组合。
- Flow 测试用 Turbine；协程用 `kotlinx-coroutines-test` 的 `runTest`。
- UI 冒烟测试用 Compose UI Test，但不追求全量覆盖（课设成本）。

## 8. 权限清单（AndroidManifest）

| 权限 | 用途 | 备注 |
| --- | --- | --- |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | 定位解锁迷雾 | 运行时请求 |
| `ACCESS_BACKGROUND_LOCATION` | 后台轨迹 | 仅 `locationMode=background` 且用户主动开启时才请求 |
| `INTERNET` | 瓦片 / 热更新 / 可选 API | 通用 |
| `ACCESS_NETWORK_STATE` | 判断瓦片是否可下载 | 通用 |
| `CAMERA` | 实拍识别实时预览 | 实拍识别（GDD 7.29）需 CameraX 实时预览，运行时请求；仅存照片仍用 Photo Picker 免此权限 |
| 存储 | 无 | 照片存应用私有目录，用 Photo Picker 选图 |

## 9. 构建与仓库结构约定

```text
settings.gradle.kts         # 单模块 + version catalog
libs.versions.toml          # 依赖统一入口
app/build.gradle.kts        # 一个 app 模块
app/src/main/assets/content/  # 世界内容 JSON（theme 包）
```

单模块不拆多 module（`01` 已定），编辑器也先放 `app` 内的 `editor/` 包。

## 10. 决策记录

| 决策 | 结论 |
| --- | --- |
| UI 框架 | Jetpack Compose + Material3 |
| DI | Hilt |
| 存档 | Room（含迁移策略，见 `05` 里程碑 M9） |
| 内容解析 | kotlinx.serialization + ignoreUnknownKeys |
| 地图 | osmdroid（WGS84、免 key、可离线）主选 |
| 叠加层 | Compose Canvas 自绘，不绑地图 SDK |
| 定位 | 玩家可配置三档，默认前台 |
| 天气 | 真实优先 + 本地模拟兜底，SignalSource 接口 |
| 实拍识别 | CameraX 实时预览 + 已知条目本地匹配，不做通用识别 |
| AR | ARCore 主验证 + CameraX/IMU 兜底，MVP 不引入依赖 |
| NPC LLM | 可选、默认关、双轨兜底 |
| 网络 | Retrofit 后补，MVP 阶段无强制联网 |
| 测试重点 | 领域引擎纯 Kotlin 单测优先 |