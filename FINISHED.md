# 完成记录

按里程碑追加，最近在顶部。详细计划见 `docs/05_ROADMAP.md`（正式范围以 `GDD_v2.md` 为准）。

## M0 · 工程脚手架（完成）

- 单模块 Gradle 工程 + Version Catalog（Gradle 8.11.1 / AGP 8.9.2 / Kotlin 2.0.21 / KSP + Hilt 2.53.1）。
- Hilt 装配（`RainingYesterdayApp` / `MainActivity`）、Material3 主题（`docs/06` 色板，含暗色）、Navigation 五 Tab 骨架。
- 占位屏：地图 / 我的空间 / 日记 / 图鉴 / 对话；`ui/ar` 占位包（P2 不改）。
- 验证：`assembleDebug` 通过；真机（Huawei MAA-AN00）安装启动，五 Tab 切换正常，无崩溃。
- 说明：`gradle-wrapper.properties` 的 distributionUrl 使用华为镜像（官方源在本网络下连接超时，国内环境更稳）。