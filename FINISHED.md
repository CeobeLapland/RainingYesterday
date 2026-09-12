# 完成记录

按里程碑追加，最近在顶部。详细计划见 `docs/05_ROADMAP.md`（正式范围以 `GDD_v2.md` 为准）。

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