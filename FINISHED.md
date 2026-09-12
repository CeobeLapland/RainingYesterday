# 完成记录

按里程碑追加，最近在顶部。详细计划见 `docs/05_ROADMAP.md`（正式范围以 `GDD_v2.md` 为准）。

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