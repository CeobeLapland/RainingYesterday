# 完成记录

按里程碑追加，最近在顶部。详细计划见 `docs/05_ROADMAP.md`（正式范围以 `GDD_v2.md` 为准）。

## M3 补丁6 · 迷雾挖洞管线确认真机可用（完成/已定位根因）

- **核心结论（经真机截图像素验证）**：迷雾=整屏铺雾色 + 在玩家位置挖圆形探索区，挖洞管线确认正常——屏幕中央亮度(162) 明显高于四周(149)，说明透明圆洞被成功挖出。
- **"满屏黑"根本原因已定位**：玩家真实定位点位于地图初始视野之外（真实坐标 39.729,116.164 vs 初始海口视野 39.992,116.312），挖的洞在屏幕外，看起来整屏都是雾。
- **修复**：加入"地图跟随玩家"——玩家位置偏离视野中心超阈值时相机自动平移居中，保证玩家/探索区始终在屏幕内可见。
- **挖洞改用 Android 原生 `PorterDuff.Mode.CLEAR`**，替代此前不可靠的 Compose `saveLayer+BlendMode.Clear`。
- 说明：圆形探索区是迷雾的可靠最小形态；六边形格子+世界网格投影的精确版暂缓（坐标链复杂），后续再迭代。

## M3 补丁5 · 内置高德源 + GCJ 坐标转换（完成）

- 新增 **高德地图** 瓦片源（免费、国内直连、无需 key），设为默认底图源。
- 新增 `domain/util/Gcj02`：WGS84 ↔ GCJ02 转换（算法标准实现）+ 往返单测。高德瓦片是 GCJ-02 偏移坐标系，叠加层（迷雾/地点/足迹/定位）的 WGS84 坐标自动转换后与高德底图对齐。
- `MapProjection` 支持 `useGcj` 模式：中心与叠加点都转 GCJ 后算相对偏移；点击返回坐标反算回 WGS84。
- 设置页瓦片源新增「高德地图」选项（UI 层映射）。天地图/OSM/USGS/自定义 保留可选。
- 验证：`assembleDebug` + 34/34 单测通过（新增 Gcj02Test 3）；真机启动正常无崩溃。

## M3 补丁4 · 迷雾"整屏铺雾"修复（完成）

- **根因修复**：迷雾绘制逻辑写反了——原先 `exploredKeys.isNotEmpty()` 才铺雾，导致未解锁/未定位时**整屏迷雾根本不画**（屏幕亮的，"什么都没有"）。改正为**始终铺整屏雾色**（未探索区域永远盖住），已解锁格再 `BlendMode.Clear` 挖洞显示探明区域。这符合战争迷雾本质。
- 坐标换算已用互逆的 `latLngToAxial`/`axialToLatLng`（世界原点 pointy-top），格子钉死在地球坐标、随地图拖/缩，不跟玩家跑。
- 验证：`assembleDebug` + 31/31 单测通过；真机启动正常无崩溃。

## M3 补丁3 · 迷雾彻底重做（世界坐标钉死）+ 瓦片源增强（完成）

- **迷雾语义修正（关键）**：战争迷雾=每个格子固定对应一块真实地理网格。重写 `HexGrid` 坐标换算核心，提供互逆的 `latLngToAxial` / `axialToLatLng`（world-origin pointy-top 平铺），`FogService` 委托同一套变换。格子按**世界经纬度**钉死在地球坐标，随地图拖/缩同步移动，绝不跟着玩家跑。修正了此前"挖洞画在画布角落/重叠、跟随定位"的根因。
- **瓦片源增强**：新增 **天地图（国内可直连，CGCS2000≈WGS84 无偏移）** 与 **自定义 URL** 两种源，加上原 OSM/USGS 海外源，全部用户可配置。天地图需 key（设置页填）、自定义 URL 支持 {z}/{x}/{y}/{s} 占位。用 osmdroid `XYTileSource` 构建。
- 设置页：增加天地图 key 输入框 + 自定义 URL 输入框（按所选源动态显示）。
- 验证：`assembleDebug` + 31/31 单测通过；真机启动正常无崩溃。

## M3 补丁2 · 迷雾修复 + 导航重构（完成）

- **迷雾修复**：挖洞坐标改为以「玩家屏幕位置」为锚点铺开六边形（原本相对画布左上角，导致雾挖在角落、看全是雾）。现在点击定位→玩家周围一圈六边形被挖开，露出底图/地点。
- **瓦片源可配置**：新增 `TileSource` 枚举（OSM标准/USGS卫星/USGS地形）进 `PlayerSettings`，设置页可切换（海外源国内慢时可换），持久化。
- **导航重构**：五 Tab 由「世界/家/摄像/消息/我的」替代原「地图/我的空间/日记/图鉴/对话」。摄像=新增占位；日记/图鉴/设置 移入「我的」嵌套子页；世界=地图（含原占位）。
- **地图顶栏**：改正规顶格工具栏（昨夜有雨 + 定位切换 + 迷雾开关 + 设置按钮），占上方一小部分。
- 新增 material-icons-extended 依赖、Home/Camera/Message/ProfileOverview/Settings 屏、SettingsViewModel。
- 验证：`assembleDebug` + 31/31 单测通过；真机启动显示新五 Tab + 顶栏，进入「我的」可进日记/图鉴/设置子页，无崩溃。

## M3 补丁 · 真实迷雾表现（完成）

- 迷雾视觉落地：用 `Compose Canvas 矢量多边形挖洞`（整屏铺雾色 → `BlendMode.Clear` 挖掉已解锁六边形），而非粒子/贴图。契合"叠加层自绘、不绑地图 SDK、无美术资源、可离线"。
- 修正灰格子根因：osmdroid 瓦片缓存改用应用私有目录（避免外存写失败）+ 固定世界原点（保证 hex 绝对坐标稳定）。
- 示例地点坐标从上海改到海淀，与初始视野对齐，标记可见。
- 定位控制条：把"定位"做成可点击循环切换 前台/手点模拟；地图单点 overlay 回传经纬度（disable=设置模拟位置）。
- 验证：`assembleDebug` 通过，真机安装启动，地图渲染雾/控制条，无崩溃。

## M4 · 探索 / 足迹 / 地点发现（完成）

- `domain/model`：`EncyclopediaEntry`（图鉴条目，refId 指向内容不复制）、`TrackPoint`（足迹点）、`DiscoveryRecord`（发现回放），均 @Serializable。
- `domain/repository/SaveRepository`：存档接口（探索度/图鉴/足迹/设置），领域层只认接口。
- `domain/engine/ExplorationService`：走进地点判定（haversine≤radius）、足迹去噪（与上点最小间距才记录）、图鉴发现幂等、周足迹统计。纯 Kotlin。
- `data/save/PrefsSaveRepository`：SharedPreferences + JSON 持久化，满足"重启不丢"，M9 Room 落地时替换（接口不变）。
- `ui/map/MapViewModel`：注入 SaveRepository，定位变化 → 走进地点[发现]写图鉴 + 足迹连点持久化；地图/图鉴联动。图层/设置持久化。
- `ui/map/MapScreen`：Canvas 新增足迹连点成线 + 地点标记区分已发现(实心暖色)/未发现(虚线轮廓)。
- `ui/collection`：真实图鉴屏，列出所有地点 + 状态 + 进度，注入 ContentRepository/SaveRepository。
- 验证：`testDebugUnitTest` 31/31 通过（新增 ExplorationServiceTest 5）；`assembleDebug` 通过；真机验证图鉴 Tab「地点图鉴 0/3」+ 列出三地点，无崩溃。

## M3 · 定位 + 六边形迷雾（完成）

- `domain/model/HexGrid`：轴向坐标六边形系统（半径围簇/六邻/距离/key 互转/像素投影），「地图迷雾形状决定者」，纯 Kotlin。
- `domain/model`：`FogCell`（迷雾格）+ 分层显现枚举、`PlayerSettings`（locationMode 三档 + 频率/位移/精度功耗参数 + 图层开关）、`MapLayer`（底图/迷雾/地点/足迹/NPC/他人 六图层，迷雾可关、他人默认关）。
- `domain/engine/FogService`：进半径解锁邻域格（增量去重）、分层显现随探索度推进（地点→历史→传闻→异常）、经纬度→所属格。
- `data/location`：`LocationProvider` 接口 + `RealLocationProvider`（Android LocationManager 免 GMS，权限缺失优雅降级）+ `MockLocationProvider`（手点模拟，无设备/演示兜底）。
- `ui/map`：`MapViewModel`（串起 定位→迷雾→触发引擎→图层开关）+ `MapScreen`（osmdroid 底图 AndroidView + Compose Canvas 自绘叠加层 + `MapProjection` 坐标换算，迷雾/定位控制条）。满足 GDD 7.1、docs/03 §4.1–4.3。
- DI 补丁：为 `JsonWorldContentParser`/`ContentValidator` 加 @Inject、`AppModule` 提供 Context。
- 验证：`testDebugUnitTest` 26/26 通过（新增 FogTest 10）；`assembleDebug` 通过；真机安装启动，地图 Tab 渲染出迷雾控制条，无崩溃。

## M2 · 领域引擎核心（完成）

- `domain/model`：`WorldState`（time/date/weather/season/daylight/campusEvents + 夜间/深层推断）、`PlayerProgress`（探索度/关系/持有物/位置）、`Daylight`。
- `domain/engine/ConditionEvaluator`：全部 10 种 Condition kind（time_range 含跨午夜、weather、season、near haversine 距离、exploration、relationship、has_item、day_of_week、date）+ all/any 递归嵌套求值。
- `domain/engine/WorldStateMachine`：时钟源（`WorldClock` 可注入固定时间）、`update`/`setState` 入口、季节本地推算、深层状态推断。
- `domain/engine/TriggerEngine`：由 ContentRepository + WorldState + PlayerProgress 算出 `DerivedState`（visibleEvents / activeRumors / npcPositions / spawnables / availableAnomalies / availableRituals / pendingEffects / arVisible [MVP 只算不渲染]）。传闻/异常/NPC 作息/稀有资源/微仪式/事件统一驱动。
- 验证：`testDebugUnitTest` 16/16 通过（M1 5 + M2 11，覆盖各 kind、组合嵌套、天气/探索度/位置变化引起 DerivedState 变化）；`assembleDebug` 通过。

## M1 · 内容数据管线（完成）

- `domain/model` 全部内容类型落成 Kotlin：Place/Npc/Task/Item/Recipe/RandomEvent/Rumor/Theme/Creature/Ritual/Festival/Tool + ArSpec + 真实信号 Signals + 枚举全集。
- `Condition` 用自定义 KSerializer（`@Serializable(with=)`）支持 `{kind,value}` 单一条件与 `{all|any}` 组合的递归解析/编码。
- `ContentRepository` 接口 + `JsonWorldContentParser`（纯 Kotlin，输入文件名→文本映射）+ `ContentValidator`（id 唯一、跨文件引用存在、坏 JSON 启动报错）+ `AssetContentRepository`（assets 读取）+ Hilt `DataModule`。
- `assets/content/campus_default/` 最小主题包：3 地点 / 2 NPC / 10 物品 / 2 任务（含 1 异常）/ 1 传闻 / 1 配方 / 1 事件 / 1 生灵 / 1 微仪式 / 1 节日 / 1 工具。
- 文档补缺：为 `festival` 与 `tool` 补充了 02 `§2.13/2.14 schema` 与 `ToolState` 存档行（theme.files 原已引用两者但缺定义）。
- 验证：`testDebugUnitTest` 5/5 通过（真实包载入+引用合法、各类 Condition、多余字段不崩、坏 JSON 报错、引用校验）；`assembleDebug` 通过。

## M0 · 工程脚手架（完成）

- 单模块 Gradle 工程 + Version Catalog（Gradle 8.11.1 / AGP 8.9.2 / Kotlin 2.0.21 / KSP + Hilt 2.53.1）。
- Hilt 装配（`RainingYesterdayApp` / `MainActivity`）、Material3 主题（`docs/06` 色板，含暗色）、Navigation 五 Tab 骨架。
- 占位屏：地图 / 我的空间 / 日记 / 图鉴 / 对话；`ui/ar` 占位包（P2 不改）。
- 验证：`assembleDebug` 通过；真机（Huawei MAA-AN00）安装启动，五 Tab 切换正常，无崩溃。
- 说明：`gradle-wrapper.properties` 的 distributionUrl 使用华为镜像（官方源在本网络下连接超时，国内环境更稳）。